package org.example.main;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.example.creator.FileCreator;
import org.example.enums.CsvNamesEnum;
import org.example.models.ReleaseCommits;
import org.example.models.Ticket;
import org.example.retrievers.CommitRetriever;
import org.example.retrievers.MetricsRetriever;
import org.example.retrievers.TicketRetriever;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class ExecutionFlow {
    private ExecutionFlow() {}

    /** This constructor sets up the necessary components for retrieving and processing ticket and commit data
     * create an instance of TicketRetriever with given project name;
     * retrieves a list of ticket using ticketRetriever.getTickets();
     * attempts to retrieve release commits associated  with tickets and prints the release commit information */

    public static void collectData(String projName){
        TicketRetriever ticketRetriever = new TicketRetriever(projName);
        List<Ticket> tickets = ticketRetriever.getTickets();
        CommitRetriever commitRetriever = ticketRetriever.getCommitRetriever();
        try {
            System.out.println("\n" + projName + " - NUMERO DI COMMIT: " + commitRetriever.retrieveCommit().size() + "\n");
            List<ReleaseCommits> releaseCommitsList = commitRetriever.getReleaseCommits(ticketRetriever.getVersionRetriever(), commitRetriever.retrieveCommit());
            MetricsRetriever.computeBuggynessAndFixedDefects(releaseCommitsList, tickets, commitRetriever, ticketRetriever.getVersionRetriever());
            MetricsRetriever.computeMetrics(releaseCommitsList, commitRetriever);
            FileCreator.writeOnCsv(projName, releaseCommitsList, CsvNamesEnum.BUGGY, 0);
            printReleaseCommit(projName, releaseCommitsList);
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static void printReleaseCommit(String projName, @NotNull List<ReleaseCommits> releaseCommitsList) {
        for(ReleaseCommits rc: releaseCommitsList) {
            System.out.println(projName + " version: " + rc.getRelease().getName() + ";" +
                    " Commits: " + rc.getCommits().size() + ";" +
                    " Java classes: " + rc.getJavaClasses().size() + ";" +
                    " Buggy classes: " + rc.getBuggyClasses());
        }
    }
}