package org.example.retrievers;

import org.example.models.Ticket;
import org.example.models.Version;
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
import java.util.Objects;

/** With this class we take the tickets from JIRA */

public class TicketRetriever {
    VersionRetriever versionRetriever;
    ArrayList<Ticket> tickets;

    public TicketRetriever(String projName, String issueType, String status, String resolution) {
        try {
            versionRetriever = new VersionRetriever(projName);
            tickets = retrieveBugTickets(projName, issueType, status, resolution);
            TicketUtils.printTickets(tickets);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    // Set OV and FV of the ticket. IV will retrieve from AV takes from Jira or takes applying proportion.
    private boolean setReleaseInfoInTicket(@NotNull Ticket ticket) {
        Version openingRelease = retrieveRelease(ticket.getCreationDate());
        Version fixRelease = retrieveRelease(ticket.getResolutionDate());
        if(openingRelease == null || fixRelease == null) // Condition that detect if there is a new version after the creation or the fix of the ticket
            return false;
        ticket.setOpeningRelease(openingRelease);
        ticket.setFixedRelease(fixRelease);
        return true;
    }

    private @Nullable Version retrieveRelease(LocalDate localDate) {
        for(Version versionInfo : versionRetriever.projVersions) {
            LocalDate releaseDate = versionInfo.getDate();
            if(!releaseDate.isBefore(localDate)) {
                return versionInfo;
            }
        }
        return null;
    }

    public  @NotNull ArrayList<Ticket> retrieveBugTickets(String projName, String issueType, String status, String resolution) throws IOException, JSONException {
        int j, i = 0, total;
        ArrayList<Ticket> consistentTickets = new ArrayList<>();
        ArrayList<Ticket> inconsistentTickets = new ArrayList<>();
        //Get JSON API for closed bugs w/ AV in the project
        do {
            //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + projName + "%22AND%22issueType%22=%22" + issueType + "%22AND(%22status%22=%22" + status + "%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22" + resolution + "%22&fields=key,resolutiondate,versions,created&startAt="
                    + i + "&maxResults=" + j;
            JSONObject json = JSONUtils.readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");
            for (; i < total && i < j; i++) {
                //Iterate through each bug
                String key = issues.getJSONObject(i%1000).get("key").toString();
                String resolutionDate = issues.getJSONObject(i%1000).getJSONObject("fields").get("resolutiondate").toString();
                String creationDate = issues.getJSONObject(i%1000).getJSONObject("fields").get("created").toString();
                ArrayList<Version> releases = versionRetriever.getAffectedVersions(issues.getJSONObject(i%1000).getJSONObject("fields").getJSONArray("versions"));
                Ticket ticket = new Ticket(creationDate, resolutionDate, key, releases, versionRetriever);
                if(!setReleaseInfoInTicket(ticket)) continue; //Discard a ticket does not have a new release after its created date
                addTicket(ticket, consistentTickets, inconsistentTickets); //Add the ticket to the consistent or inconsistent list, based on the consistency check
            }
        } while (i < total);
        adjustInconsistentTickets(inconsistentTickets, consistentTickets); //Adjust the inconsistency tickets using proportion for missing IV

        return consistentTickets;
    }
    private  void adjustInconsistentTickets(@NotNull ArrayList<Ticket> inconsistentTickets, ArrayList<Ticket> consistentTickets) {
        double proportionValue = Proportion.computeProportionValue(consistentTickets);
        System.out.println("Proportion value: " + proportionValue);
        for(Ticket ticket: inconsistentTickets) {
            adjustTicket(ticket, proportionValue); //Use proportion to compute the IV
            if(!consistencyCheck(ticket)) {
                throw new RuntimeException(); //Create a new exception for the case when the ticket is not adjusted correctly
            }
            consistentTickets.add(ticket); //Add the adjusted ticket to the consistent list
        }

    }

    private void adjustTicket(Ticket ticket, double proportionValue) {
        //Assign the new injected version for the inconsistent ticket as max(0, FV-(FV-OV)*P)
        Version OV = ticket.getOpeningRelease();
        Version FV = ticket.getFixedRelease();
        int newIndex = (int) (FV.getIndex() - (FV.getIndex() - OV.getIndex())*proportionValue);
        if(newIndex < 0) {
            ticket.setInjectedRelease(versionRetriever.projVersions.get(0));
            return;
        }
        ticket.setInjectedRelease(versionRetriever.projVersions.get(newIndex));
    }
    //Check that IV <= OV <= FV and that IV = AV[0]. If one condition is false, the ticket will add to inconsistency tickets
    private static void addTicket(Ticket ticket, ArrayList<Ticket> consistentTickets, ArrayList<Ticket> inconsistentTickets) {
        if(!consistencyCheck(ticket)) {
            inconsistentTickets.add(ticket);
        } else {
            consistentTickets.add(ticket);
        }
    }
    private static boolean consistencyCheck(Ticket ticket) {
        Version IV = ticket.getInjectedRelease();
        Version OV = ticket.getOpeningRelease();
        Version FV = ticket.getFixedRelease();
        return (IV != null) &&
                (IV.getIndex() <= OV.getIndex()) &&
                (OV.getIndex() <= FV.getIndex());
    }

    public ArrayList<Ticket> getTickets() {
        return tickets;
    }
}











//    private static final String FIELDS = "fields";
//    private VersionRetriever versionRetriever;
//    private List<Ticket> tickets;
//
//    public TicketRetriever(String projectName) {
//
//        String issueType = "Bug";
//        String state = "closed";
//        String resolution = "fixed";
//
//        try {
//            versionRetriever = new VersionRetriever(projectName);
//            tickets = retrieveBugTickets(projectName, issueType, state, resolution);
//            TicketUtils.printTickets((ArrayList<Ticket>) tickets);
//            System.out.println("Ticket extract from " + projectName + "; " + tickets.size());
//            int count = 0;
//            for(Ticket ticket: tickets){
//                count += ticket.getAssociatedCommits().size();
//            }
//
//            System.out.println("Commit extract from " + projectName + ": " + count);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//    private void setReleaseInTicket(@NotNull Ticket ticket){
//
//        Version openingRelease = retrieveNextRelease(ticket.getCreationDate());
//        Version fixRelease = retrieveNextRelease(ticket.getResolutionDate());
//
//        ticket.setOpeningRelease(openingRelease);
//        ticket.setFixedRelease(fixRelease);
//
//    }
//    private @Nullable Version retrieveNextRelease(LocalDate date){
//
//        for(Version versionInfo: versionRetriever.getProjVersions()){
//            LocalDate releaseDate = versionInfo.getDate();
//            if(!releaseDate.isBefore(date)){
//                return versionInfo;
//            }
//        }
//        return null;
//
//    }
//
//    private @NotNull List<Ticket> retrieveBugTickets(String projectName, String issueType, String state, String resolution) throws IOException, JSONException {
//
//        int i = 0;
//        int j;
//        int total;
//        ArrayList<Ticket> consistentTickets = new ArrayList<>();
//        ArrayList<Ticket> inconsistentTickets = new ArrayList<>();
//
//        do {
//            //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
//            j = i + 1000;
//            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
//                    + projectName + "%22AND%22issueType%22=%22" + issueType + "%22AND(%22status%22=%22" + state + "%22OR"
//                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22" + resolution + "%22&fields=key,resolutiondate,versions,created&startAt="
//                    + i + "&maxResults=" + j;
//            JSONObject json = JSONUtils.readJsonFromUrl(url);
//            JSONArray issues = json.getJSONArray("issues");
//            total = json.getInt("total");
//            for (; i < total && i < j; i++) {
//                //Iterate through each bug
//                String key = issues.getJSONObject(i%1000).get("key").toString();
//                String resolutionDate = issues.getJSONObject(i%1000).getJSONObject(FIELDS).get("resolutiondate").toString();
//                String creationDate = issues.getJSONObject(i%1000).getJSONObject(FIELDS).get("created").toString();
//                List<Version> releases = versionRetriever.getAffectedVersions(issues.getJSONObject(i%1000).getJSONObject(FIELDS).getJSONArray("versions"));
//                Ticket ticket = new Ticket(creationDate, resolutionDate, key, releases, versionRetriever);
//                setReleaseInTicket(ticket);
//                addTicket(ticket, consistentTickets, inconsistentTickets);
//            }
//        } while (i < total);
//        CommitRetriever commitRetriever = new CommitRetriever("/home/giulia/Documenti/GitHub/" + projectName.toLowerCase());
//        TicketUtils.printTickets(consistentTickets);
//        System.out.println("\nTickets extract before delete commit form " + projectName + ": " + consistentTickets.size() + "\n");
//        System.out.println("\n------------------------------------------------------------------------------\n");
//        TicketUtils.sortTickets(consistentTickets);
//        return commitRetriever.associateTicketAndCommit(versionRetriever, commitRetriever, consistentTickets);
//    }
//
//    private void fixTicket(Ticket ticket, double proportionValue){
//
//        Version ov = ticket.getOpeningRelease();
//        Version fv = ticket.getFixedRelease();
//        int newIndex;
//
//        if(Objects.equals(fv.getIndex(), ov.getIndex())){
//            newIndex = (int) Math.floor(fv.getIndex() - proportionValue);
//        }else{
//            newIndex = (int) Math.floor(fv.getIndex() - (fv.getIndex() - ov.getIndex()) * proportionValue);
//        }
//
//        if(newIndex < 0){
//            ticket.setInjectedRelease(versionRetriever.getProjVersions().get(0));
//            return;
//        }
//        ticket.setInjectedRelease(versionRetriever.getProjVersions().get(newIndex));
//
//    }
//
//    private static void addTicket(Ticket ticket, ArrayList<Ticket> consistentTicket, ArrayList<Ticket> inconsistentTicket){
//
//        // IV <= OV <= FV, IV = AV[0]
//        // If condition is false, we have an inconsistency ticket
//
//        if(!isNotConsistent(ticket))
//            inconsistentTicket.add(ticket);
//        else
//            consistentTicket.add(ticket);
//
//    }
//
//    private static boolean isNotConsistent(Ticket ticket){
//
//        Version iv = ticket.getInjectedRelease();
//        Version ov = ticket.getOpeningRelease();
//        Version fv = ticket.getFixedRelease();
//
//        return iv != null && iv.getIndex() <= ov.getIndex() && ov.getIndex() <= fv.getIndex();
//
//    }
//    public VersionRetriever getVersionRetriever() {
//        return versionRetriever;
//    }
//
//    public void setVersionRetriever(VersionRetriever versionRetriever) {
//        this.versionRetriever = versionRetriever;
//    }
//
//
//    public List<Ticket> getTickets(){
//        return this.tickets;
//    }
//
//    public void setTickets(List<Ticket> tickets){
//        this.tickets = tickets;
//    }

