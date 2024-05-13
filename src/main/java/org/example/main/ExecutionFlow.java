package org.example.main;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.example.creator.FileCreator;
import org.example.enums.FilenamesEnum;
import org.example.models.ReleaseInfo;
import org.example.models.Ticket;
import org.example.retrievers.CommitRetriever;
import org.example.retrievers.MetricsRetriever;
import org.example.retrievers.TicketRetriever;
import org.example.retrievers.VersionRetriever;
import org.example.utils.TicketUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExecutionFlow {
    private ExecutionFlow(){}

    /** This constructor sets up the necessary components for retrieving and processing ticket and commit data
     * create an instance of TicketRetriever with given project name;
     * retrieves a list of ticket using ticketRetriever.getTickets();
     * attempts to retrieve release commits associated  with tickets and prints the release commit information */
    public static void collectData(String projName){
        System.out.println(projName);
        TicketRetriever ticketRetriever = new TicketRetriever(projName);
        CommitRetriever commitRetriever = ticketRetriever.getCommitRetriever();
        VersionRetriever versionRetriever = ticketRetriever.getVersionRetriever();
        List<Ticket> tickets = ticketRetriever.getTickets();
        try {
            System.out.println("\n" + projName + " - NUMERO DI COMMIT: " + commitRetriever.retrieveCommit().size() + "\n");
            //Retrieve the release information about commits, classes and metrics that involve the release.
            List<ReleaseInfo> allTheReleaseInfo = commitRetriever.getReleaseCommits(versionRetriever, commitRetriever.retrieveCommit());
            MetricsRetriever.computeMetrics(allTheReleaseInfo, tickets, commitRetriever, versionRetriever);
            FileCreator.writeOnCsv(projName, allTheReleaseInfo, FilenamesEnum.METRICS, 0);
            printReleaseInfo(projName, allTheReleaseInfo);

            //--------------------------------- WALK FORWARD ---------------------------------

            List<ReleaseInfo> releaseInfoListHalved = discardHalfReleases(allTheReleaseInfo);

            //TODO dobbiamo ignorare il caso in cui non abbiamo training set?
            //Iterate starting by 1 so that the walk forward starts from using at least one training set.
            for(int i = 1; i < releaseInfoListHalved.size(); i++) {
                //Selection of the tickets opened until the i-th release.
                List<Ticket> ticketsUntilRelease = TicketUtils.getTicketsUntilRelease(tickets, i);

                //TODO rifare il calcolo della buggyness soltanto per i set di training oppure anche quello di testing?
                //Non viene aggiornata la buggyness del testing set
                MetricsRetriever.computeBuggyness(releaseInfoListHalved.subList(0, i), ticketsUntilRelease, commitRetriever, versionRetriever);

                FileCreator.writeOnArff(projName, releaseInfoListHalved.subList(0, i), FilenamesEnum.TRAINING, i);
                ArrayList<ReleaseInfo> testingRelease = new ArrayList<>();
                testingRelease.add(releaseInfoListHalved.get(i));
                FileCreator.writeOnArff(projName, testingRelease, FilenamesEnum.TESTING, i);
            }
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static @NotNull List<ReleaseInfo> discardHalfReleases(@NotNull List<ReleaseInfo> releaseInfoList) {
        //TODO dobbiamo tenerci la metà delle release per il training e un'altra per il testing (quindi n/2 +1 set)
        // oppure dobbiamo tenerci n/2 set in totale, ovvero n/2-1 set per il training e uno per il testing?
        // Cosa fare se n è dispari?

        int n = releaseInfoList.size();
        releaseInfoList.sort((o1, o2) -> {
            Integer i1 = o1.getRelease().getIndex();
            Integer i2 = o2.getRelease().getIndex();
            return i1.compareTo(i2);
        });

        return releaseInfoList.subList(0, n/2+1);
    }

    private static void printReleaseInfo(String projName, @NotNull List<ReleaseInfo> releaseInfoList) {
        for(ReleaseInfo rc: releaseInfoList) {
            System.out.println(projName + " version: " + rc.getRelease().getName() + ";" +
                    " Commits: " + rc.getCommits().size() + ";" +
                    " Java classes: " + rc.getJavaClasses().size() + ";" +
                    " Buggy classes: " + rc.getBuggyClasses());
        }
    }
}