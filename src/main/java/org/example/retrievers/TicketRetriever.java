package org.example.retrievers;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.example.models.Ticket;
import org.example.models.Version;
import org.example.utils.JSONUtils;
import org.example.utils.Proportion;
import org.example.utils.VersionUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** With this class we take the tickets from JIRA */
public class TicketRetriever {
    static final String FIELDS = "fields";
    VersionRetriever versionRetriever;
    CommitRetriever commitRetriever;
    List<Ticket> tickets;
    boolean coldStart = false;

    /** It initializes the TicketRetriever instance by calling the init method with the provided project name.
     * It initializes the CommitRetriever instance passing the path to the directory and the versioneRetriever instance. */
    public TicketRetriever(String projName) throws GitAPIException, IOException, URISyntaxException {
        init(projName);
        commitRetriever = new CommitRetriever("/home/giulia/Documenti/GitHub/" + projName.toLowerCase(), versionRetriever);
        commitRetriever.associateCommitAndVersion(versionRetriever.getProjVersions()); //Association of commits and versions and deletion of the version without commits

    }

    public TicketRetriever(String projName, boolean coldStart) throws GitAPIException, IOException, URISyntaxException {
        this.coldStart = coldStart;
        init(projName);
    }

    /** This constructor initializes a TicketRetriever object for a specific project, retrieves bug tickets with a certain
     * criteria and prints out information about the retrieved tickets and associated commits. */
    private void init(String projName) throws GitAPIException, IOException, URISyntaxException {
        String issueType = "Bug";
        String status = "closed";
        String resolution = "fixed";
        versionRetriever = new VersionRetriever(projName);
        tickets = retrieveBugTickets(projName, issueType, status, resolution);

    }

    /** Set OV and FV of the ticket. IV will retrieve from AV takes from Jira or takes applying proportion. */
    private void setReleaseInTicket(@NotNull Ticket ticket) {
        Version openingRelease = VersionUtils.retrieveNextRelease(versionRetriever, ticket.getCreationDate());
        Version fixRelease = VersionUtils.retrieveNextRelease(versionRetriever, ticket.getResolutionDate());

        ticket.setOpeningRelease(openingRelease);
        ticket.setFixedRelease(fixRelease);
    }

    public  @NotNull List<Ticket> retrieveBugTickets(String projName, String issueType, String status, String resolution) throws IOException, JSONException, GitAPIException, URISyntaxException {
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
                setReleaseInTicket(ticket);
                //Discard tickets that are incorrect or that are after the last release
                if (ticket.getOpeningRelease() == null ||
                        (ticket.getInjectedRelease() != null &&
                                (ticket.getInjectedRelease().getIndex() > ticket.getOpeningRelease().getIndex())) ||
                        ticket.getFixedRelease() == null)
                    continue;
                addTicket(ticket, consistentTickets, inconsistentTickets); //Add the ticket to the consistent or inconsistent list, based on the consistency check
            }
        } while (i < total);
        if(!coldStart) {
            adjustInconsistentTickets(inconsistentTickets, consistentTickets); //Adjust the inconsistency tickets using proportion for missing IV, when you are not using cold start
            consistentTickets.sort(Comparator.comparing(Ticket::getCreationDate));
            if(commitRetriever == null) {
                commitRetriever = new CommitRetriever("/home/giulia/Documenti/GitHub/" + projName.toLowerCase(), versionRetriever);
            }
            commitRetriever.associateTicketAndCommit(consistentTickets);
        } /* Adjust the inconsistency tickets using proportion for missing IV, when you are not using cold start */
        discardInvalidTicket(consistentTickets); /* Discard the tickets that aren't consistent yet.*/
        return consistentTickets;
    }

    /** This method helps to ensure that only valid tickets remain in the list after filtering out invalid ones based on
     * release indices.
     * Discard tickets that have OV > FV or that have IV=OV */
    private void discardInvalidTicket(@NotNull ArrayList<Ticket> tickets) {
        tickets.removeIf(ticket -> ticket.getOpeningRelease().getIndex() > ticket.getFixedRelease().getIndex() ||   /* Discard if OV > FV */
                ticket.getInjectedRelease().getIndex() >= ticket.getOpeningRelease().getIndex() || /*Discard if IV >= OV */
                (ticket.getOpeningRelease() == null || ticket.getFixedRelease() == null)); /* Discard if there is a new version after the creation or the fix of the ticket */
    }


    /** It adjusts inconsistent tickets based on a proportion value and ensures that the adjusted tickets are consistent
     * before adding them to the list of consistent tickets.*/
    private  void adjustInconsistentTickets(@NotNull List<Ticket> inconsistentTickets, @NotNull List<Ticket> consistentTickets) throws GitAPIException, IOException, URISyntaxException {
        List<Ticket> ticketForProportion = new ArrayList<>();
        List<Ticket> allTickets = new ArrayList<>();

        allTickets.addAll(inconsistentTickets);
        allTickets.addAll(consistentTickets);

        allTickets.sort(Comparator.comparing(Ticket::getResolutionDate));
        for(Ticket ticket: allTickets) {
            double proportionValue;
            if(inconsistentTickets.contains(ticket)) {  //If the ticket is in the inconsistent tickets list, then adjust the ticket using proportion.
                proportionValue = incrementalProportion(ticketForProportion);
                adjustTicket(ticket, proportionValue); //Use proportion to compute the IV.
            } else if(consistentTickets.contains(ticket) && Proportion.isAValidTicketForProportion(ticket)) {
                ticketForProportion.add(ticket);
            }

            if(isNotConsistent(ticket)) {
                continue;
            }
            if(!consistentTickets.contains(ticket))
                consistentTickets.add(ticket); //Add the adjusted ticket to the consistent list
        }
    }

    private static double incrementalProportion(@NotNull List<Ticket> consistentTickets) throws GitAPIException, IOException, URISyntaxException {
        double proportionValue;

        if(consistentTickets.size() >= 5) {
            proportionValue = Proportion.computeProportionValue(consistentTickets);
        } else {
            proportionValue = Proportion.computeColdStartProportionValue();
        }
        return proportionValue;
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
        if(newIndex < 0)
            newIndex = 0;
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
    public CommitRetriever getCommitRetriever() {
        return commitRetriever;
    }

    public VersionRetriever getVersionRetriever() {
        return versionRetriever;
    }
}