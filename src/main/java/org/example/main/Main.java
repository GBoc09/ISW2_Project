package org.example.main;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.example.models.Ticket;
import org.example.retrievers.CommitRetriever;
import org.example.retrievers.TicketRetriever;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        String issueType = "Bug";
        String status = "closed";
        String resolution = "fixed";
        TicketRetriever bookkeeperRetriever = new TicketRetriever("BOOKKEEPER", issueType, status, resolution);
        ArrayList<Ticket> bookTickets = bookkeeperRetriever.getTickets();

        CommitRetriever bookCommitRetriever = new CommitRetriever("/home/giulia/Documenti/GitHub/bookkeeper");

        associateTicketAndCommit(bookCommitRetriever, bookTickets);

//       String bookkeeper = "BOOKKEEPER";
//       String BOOKKEEPER_PATH = "/home/giulia/Documenti/GitHub/bookkeeper";
//
//        String openjpa = "OPENJPA";
//        String OPENJPA_PATH = "/home/giulia/Documenti/GitHub/openjpa";
//
//        String issueType = "Bug";
//        String state = "closed";
//        String resolution = "fixed";
//
//        TicketRetriever bookkeeperRetriever = new TicketRetriever(bookkeeper);
//        CommitRetriever commitRetriever = new CommitRetriever("/home/giulia/Documenti/GitHub/bookkeeper");
//        List<Ticket> bookkeeperTickets = bookkeeperRetriever.getTickets();

    }
    private static void associateTicketAndCommit(CommitRetriever bookCommitRetriever, ArrayList<Ticket> bookTickets) {
        try {
            for (Ticket ticket : bookTickets) {
                ArrayList<RevCommit> commits = bookCommitRetriever.retrieveCommit(ticket);
                ticket.setAssociatedCommits(commits);
            }
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

}