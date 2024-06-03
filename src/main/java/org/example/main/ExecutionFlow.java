package org.example.main;

import org.example.creator.FileCreator;
import org.example.enums.FilenamesEnum;
import org.example.models.ClassifierEvaluation;
import org.example.models.ReleaseInfo;
import org.example.models.Ticket;
import org.example.retrievers.*;
import org.example.utils.TicketUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ExecutionFlow {
    private static final Logger logger = Logger.getLogger(ExecutionFlow.class.getName());
    private ExecutionFlow() {}

   /** system for collecting and analyzing data related to software projects.
    *     It initializes retrievers for tickets, commits, and versions.
    *     Retrieves tickets associated with the specified project.
    *     Retrieves information about commits related to releases.
    *     Computes metrics based on the release information and tickets.
    *     Writes the computed metrics to a CSV file.
    *     Performs a "walk forward" process, where it iterates over release information.
    *         It discards half of the releases.
    *         For each iteration, it computes bugginess, writes training and testing data to ARFF files.
    *     Initiates Weka evaluation by retrieving classifier evaluations.
    *     Writes evaluation data to a CSV file. */

    public static void collectData(String projName) throws Exception{
        TicketRetriever ticketRetriever = new TicketRetriever(projName);
        CommitRetriever commitRetriever = ticketRetriever.getCommitRetriever();
        VersionRetriever versionRetriever = ticketRetriever.getVersionRetriever();

        //Retrieve of all project tickets that are valid ticket.
        List<Ticket> tickets = ticketRetriever.getTickets();
        logger.info("Tickets retrieved.");

        //Retrieve the release information about commits, classes and metrics that involve the release.
        List<ReleaseInfo> allTheReleaseInfo = commitRetriever.getReleaseCommits(versionRetriever, commitRetriever.retrieveCommit());
        logger.info("Information about commits retrieved.");
        MetricsRetriever.computeMetrics(allTheReleaseInfo, tickets, commitRetriever, versionRetriever);
        logger.info("Metrics computed.");
        FileCreator.writeOnCsv(projName, allTheReleaseInfo, FilenamesEnum.METRICS, 0);
        logger.info("Csv file created.");

        //----------------------------------------------------------- WALK FORWARD -----------------------------------------------------------
        logger.info("Starting walk forward.");
        List<ReleaseInfo> releaseInfoListHalved = discardHalfReleases(allTheReleaseInfo);

        //Iterate starting by 1 so that the walk forward starts from using at least one training set.
        for(int i = 1; i < releaseInfoListHalved.size(); i++) {
            //Selection of the tickets opened until the i-th release.
            List<Ticket> ticketsUntilRelease = TicketUtils.getTicketsUntilRelease(tickets, i);

            // Testing set buggyness is not updated.
            MetricsRetriever.computeBuggyness(releaseInfoListHalved.subList(0, i), ticketsUntilRelease, commitRetriever, versionRetriever);

            FileCreator.writeOnArff(projName, releaseInfoListHalved.subList(0, i), FilenamesEnum.TRAINING, i);
            ArrayList<ReleaseInfo> testingRelease = new ArrayList<>();
            testingRelease.add(releaseInfoListHalved.get(i));
            FileCreator.writeOnArff(projName, testingRelease, FilenamesEnum.TESTING, i);
            int finalI = i;
            logger.info(() -> finalI + ") Iteration completed.");
        }
        logger.info("Arff file created.");
        logger.info("Starting Weka evaluation.");
        WekaInfoRetriever wekaInfoRetriever = new WekaInfoRetriever(projName, allTheReleaseInfo.size()/2);
        List<ClassifierEvaluation> classifierEvaluationList = wekaInfoRetriever.retrieveClassifiersEvaluation(projName);
        FileCreator.writeEvaluationDataOnCsv(projName, classifierEvaluationList);
        logger.info("Finished Weka evaluation.");
    }

    /** This method sorts the lists of the ReleaseInfo objects based on their associated release indices and then discard
     * half of the releases, keeping the first half.
     * It sorts the releaseInfoList based on the indices of the releases they contain
     * It returns a sublist of the first half of the sorted releaseInfoList, including the middle element if the size of
     * the list is odd.
     * The Comparator used for sorting relies on comparing the indices of the releases.
     */
    private static @NotNull List<ReleaseInfo> discardHalfReleases(@NotNull List<ReleaseInfo> releaseInfoList) {
        int n = releaseInfoList.size();
        releaseInfoList.sort((o1, o2) -> {
            Integer i1 = o1.getRelease().getIndex();
            Integer i2 = o2.getRelease().getIndex();
            return i1.compareTo(i2);
        });

        return releaseInfoList.subList(0, n/2+1);
    }
}