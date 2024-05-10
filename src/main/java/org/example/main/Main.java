package org.example.main;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.example.models.ReleaseCommits;
import org.example.models.Ticket;
import org.example.retrievers.CommitRetriever;
import org.example.retrievers.TicketRetriever;
import org.example.utils.TicketUtils;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        TicketRetriever bookkeeperRetriever = new TicketRetriever("BOOKKEEPER");
        List<Ticket> bookTickets = bookkeeperRetriever.getTickets();
        //TicketRetriever openjpaRetriever = new TicketRetriever("OPENJPA");
        CommitRetriever bookCommitRetriever = bookkeeperRetriever.getCommitRetriever();
        //commitRetriever.retrieveChangesFromTickets(bookTickets);
        try {
            List<ReleaseCommits> releaseCommitsList = bookCommitRetriever.getReleaseCommits(bookkeeperRetriever.getVersionRetriever(), TicketUtils.getAssociatedCommit(bookTickets));
            printReleaseCommit(releaseCommitsList);
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void printReleaseCommit(List<ReleaseCommits> releaseCommitsList) {
        for(ReleaseCommits rc: releaseCommitsList) {
            System.out.println(rc.getJavaClasses().keySet());
        }
    }



}