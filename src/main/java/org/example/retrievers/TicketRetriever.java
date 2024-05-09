package org.example.retrievers;

import org.example.models.Ticket;
import org.example.models.Version;
import org.example.utils.ColdStart;
import org.example.utils.JSONUtils;
import org.example.utils.Proportion;
import org.example.utils.TicketUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** With this class we take the tickets from JIRA */

public class TicketRetriever {
    public static final String FIELDS = "fields";
    VersionRetriever versionRetriever;
    List<Ticket> tickets;
    boolean coldStart = false;

    /** This constructor initializes a TicketRetriever object for a specific project, retrieves bug tickets with a certain
     * criteria and prints out information about the retrieved tickets and associated commits. */
    public TicketRetriever(String projName) {
        String issueType = "Bug";
        String status = "closed";
        String resolution = "fixed";
        try {
            versionRetriever = new VersionRetriever(projName);
            tickets = retrieveBugTickets(projName, issueType, status, resolution);
            TicketUtils.printTickets(tickets);
            System.out.println("Tickets estratti da " + projName + ": " + tickets.size());
            int count = 0;
            for(Ticket ticket: tickets) {
                count += ticket.getAssociatedCommits().size();
            }
            System.out.println("Commits estratti da " + projName + ": " + count);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TicketRetriever(String projName, boolean coldStart){
    String issueType = "Bug";
    String status = "closed";
    String resolution = "fixed";
    this.coldStart = coldStart;
        try {
            versionRetriever = new VersionRetriever(projName);
            tickets = retrieveBugTickets(projName, issueType, status, resolution);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /** Set OV and FV of the ticket. IV will retrieve from AV takes from Jira or takes applying proportion. */
    private void setReleaseInfoInTicket(@NotNull Ticket ticket) {
        Version openingRelease = retrieveNextRelease(ticket.getCreationDate());
        Version fixRelease = retrieveNextRelease(ticket.getResolutionDate());

        ticket.setOpeningRelease(openingRelease);
        ticket.setFixedRelease(fixRelease);
    }

    private @Nullable Version retrieveNextRelease(LocalDate localDate) {
        for(Version versionInfo : versionRetriever.projVersions) {
            LocalDate releaseDate = versionInfo.getDate();
            if(!releaseDate.isBefore(localDate)) {
                return versionInfo;
            }
        }
        return null;
    }

    public  @NotNull List<Ticket> retrieveBugTickets(String projName, String issueType, String status, String resolution) throws IOException, JSONException {
        int j;
        int i = 0;
        int total;
        ArrayList<Ticket> consistentTickets = new ArrayList<>();
        ArrayList<Ticket> inconsistentTickets = new ArrayList<>();
        /* Get JSON API for closed bugs w/ AV in the project */
        do {
            /* Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000 */
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + projName + "%22AND%22issueType%22=%22" + issueType + "%22AND(%22status%22=%22" + status + "%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22" + resolution + "%22&fields=key,resolutiondate,versions,created&startAt="
                    + i + "&maxResults=" + j;
            JSONObject json = JSONUtils.readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");
            for (; i < total && i < j; i++) {
                /** Iterate through each bug */
                String key = issues.getJSONObject(i%1000).get("key").toString();
                String resolutionDate = issues.getJSONObject(i%1000).getJSONObject(FIELDS).get("resolutiondate").toString();
                String creationDate = issues.getJSONObject(i%1000).getJSONObject(FIELDS).get("created").toString();
                List<Version> releases = versionRetriever.getAffectedVersions(issues.getJSONObject(i%1000).getJSONObject(FIELDS).getJSONArray("versions"));
                Ticket ticket = new Ticket(creationDate, resolutionDate, key, releases, versionRetriever);
                setReleaseInfoInTicket(ticket);
                if(ticket.getInjectedRelease() != null && (ticket.getInjectedRelease().getIndex() > ticket.getOpeningRelease().getIndex())) continue;
                addTicket(ticket, consistentTickets, inconsistentTickets); //Add the ticket to the consistent or inconsistent list, based on the consistency check
            }
        } while (i < total);
        adjustInconsistentTickets(inconsistentTickets, consistentTickets); /*Adjust the inconsistency tickets using proportion for missing IV */
        if(!coldStart) adjustInconsistentTickets(inconsistentTickets, consistentTickets); /* Adjust the inconsistency tickets using proportion for missing IV, when you are not using cold start */
        CommitRetriever commitRetriever = new CommitRetriever("/home/giulia/Documenti/GitHub/" + projName.toLowerCase());
        discardInvalidTicket(consistentTickets); /* Discard the tickets that aren't consistent yet.*/
        TicketUtils.printTickets(consistentTickets);
        System.out.println("\nTickets estratti prima di togliere commit da " + projName + ": " + consistentTickets.size() + "\n");

        System.out.println("\n------------------------------------------------------------------------------\n");

        TicketUtils.sortTickets(consistentTickets);

        return commitRetriever.associateTicketAndCommit(versionRetriever, commitRetriever, consistentTickets);

    }
    /** This method helps to ensure that only valid tickets remain in the list after filtering out invalid ones based on
     * release indices.
     * Discard tickets that have OV > FV or that have IV=OV */
    private void discardInvalidTicket(ArrayList<Ticket> tickets) {
        tickets.removeIf(ticket -> ticket.getOpeningRelease().getIndex() > ticket.getFixedRelease().getIndex() ||   /* Discard if OV > FV */
                ticket.getInjectedRelease().getIndex() >= ticket.getOpeningRelease().getIndex() || /*Discard if IV >= OV */
                (ticket.getOpeningRelease() == null || ticket.getFixedRelease() == null)); /* Discard if there is a new version after the creation or the fix of the ticket */
    }


    /** It adjusts inconsistent tickets based on a proportion value and ensures that the adjusted tickets are consistent
     * before adding them to the list of consistent tickets.*/
    private  void adjustInconsistentTickets(@NotNull List<Ticket> inconsistentTickets, @NotNull List<Ticket> consistentTickets){
        double proportionValue;
        if(consistentTickets.size() >= 5) {
            proportionValue = Proportion.computeProportionValue(consistentTickets);
        } else {
            proportionValue = Proportion.computeProportionValue(ColdStart.coldStart());
        }
        System.out.println("Proportion value: " + proportionValue);
        for(Ticket ticket: inconsistentTickets) {
            adjustTicket(ticket, proportionValue); /* Use proportion to compute the IV */
            if(isNotConsistent(ticket)) {
                throw new RuntimeException(); /* Create a new exception for the case when the ticket is not adjusted correctly */
            }
            consistentTickets.add(ticket); /* Add the adjusted ticket to the consistent list */
        }

    }

    /** This method computers a new injected version for the ticket based on the proportion value and updates the ticket
     * accordingly. */
    private void adjustTicket(Ticket ticket, double proportionValue) {
        /* Assign the new injected version for the inconsistent ticket as max(0, FV-(FV-OV)*P)*/
        Version ov = ticket.getOpeningRelease();
        Version fv = ticket.getFixedRelease();
        int newIndex;
        if(fv.getIndex() == ov.getIndex()) {
            newIndex = (int) Math.floor(fv.getIndex() - proportionValue);
        } else {
            newIndex = (int) Math.floor(fv.getIndex() - (fv.getIndex() - ov.getIndex()) * proportionValue);
        }
        if(newIndex < 0) {
            ticket.setInjectedRelease(versionRetriever.projVersions.get(0));
            return;
        }
        ticket.setInjectedRelease(versionRetriever.projVersions.get(newIndex));
    }
    /** Check that IV <= OV <= FV and that IV = AV[0]. If one condition is false, the ticket will add to inconsistency tickets */
    private static void addTicket(Ticket ticket, ArrayList<Ticket> consistentTickets, ArrayList<Ticket> inconsistentTickets) {
        if(isNotConsistent(ticket)) {
            inconsistentTickets.add(ticket);
        } else {
            consistentTickets.add(ticket);
        }
    }
    private static boolean isNotConsistent(Ticket ticket) {
        Version iv = ticket.getInjectedRelease();
        Version ov = ticket.getOpeningRelease();
        Version fv = ticket.getFixedRelease();
        return (iv == null) ||
                (iv.getIndex() > ov.getIndex()) ||
                (ov.getIndex() > fv.getIndex());
    }


    public List<Ticket> getTickets() {
        return tickets;
    }
}

