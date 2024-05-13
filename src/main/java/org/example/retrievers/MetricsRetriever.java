package org.example.retrievers;

import org.eclipse.jgit.revwalk.RevCommit;
import org.example.models.ChangedJavaClass;
import org.example.models.JavaClass;
import org.example.models.ReleaseCommits;
import org.example.models.Ticket;
import org.example.utils.GitUtils;
import org.example.utils.JavaClassUtils;
import org.example.utils.VersionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class MetricsRetriever {
    public static void computeBuggynessAndFixedDefects(List<ReleaseCommits> releaseCommitsList, @NotNull List<Ticket> tickets, CommitRetriever commitRetriever, VersionRetriever versionRetriever) {
        for(Ticket ticket: tickets){
            for (RevCommit commit : ticket.getAssociatedCommits()) {
                ReleaseCommits releaseCommits = VersionUtils.retrieveCommitRelease(
                        versionRetriever,
                        GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen()),
                        releaseCommitsList);

                if (releaseCommits != null) {
                    List<ChangedJavaClass> classChangedList = commitRetriever.retrieveChanges(commit);

                    for (ChangedJavaClass javaClass : classChangedList) {
                        JavaClassUtils.updateJavaBuggyness(javaClass, releaseCommitsList, ticket.getAffectedReleases(), commit);
                    }
                }
            }
            //For each ticket, update the number of fixed defects of classes present in the last commit of the ticket (the fixed commit).
            List<ChangedJavaClass> classChangedList = commitRetriever.retrieveChanges(ticket.getLastCommit());
            JavaClassUtils.updateNumberOfFixedDefects(versionRetriever, ticket.getLastCommit(), classChangedList, releaseCommitsList);
        }
    }

    public static void computeMetrics(List<ReleaseCommits> releaseCommitsList, CommitRetriever commitRetriever) {
        //Add the size metric in all the classes of the release.
        addSizeLabel(releaseCommitsList);
        try {
            computeLocData(releaseCommitsList, commitRetriever);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void computeLocData(List<ReleaseCommits> releaseCommitsList, CommitRetriever commitRetriever) throws IOException {
        for(ReleaseCommits rc: releaseCommitsList) {
            for (JavaClass javaClass : rc.getJavaClasses()) {
                commitRetriever.computeAddedAndDeletedLinesList(javaClass);
                computeLocAndChurnMetrics(javaClass);
            }
        }
    }
    private static void computeLocAndChurnMetrics(JavaClass javaClass) {

        int sumLOC = 0;
        int maxLOC = 0;
        double avgLOC = 0;
        int churn = 0;
        int maxChurn = 0;
        double avgChurn = 0;
        int sumOfTheDeletedLOC = 0;
        int maxDeletedLOC = 0;
        double avgDeletedLOC = 0;

        for(int i=0; i<javaClass.getMetrics().getAddedLinesList().size(); i++) {

            int currentLOC = javaClass.getMetrics().getAddedLinesList().get(i);
            int currentDeletedLOC = javaClass.getMetrics().getDeletedLinesList().get(i);
            int currentDiff = Math.abs(currentLOC - currentDeletedLOC);

            sumLOC = sumLOC + currentLOC;
            churn = churn + currentDiff;
            sumOfTheDeletedLOC = sumOfTheDeletedLOC + currentDeletedLOC;

            if(currentLOC > maxLOC) {
                maxLOC = currentLOC;
            }
            if(currentDiff > maxChurn) {
                maxChurn = currentDiff;
            }
            if(currentDeletedLOC > maxDeletedLOC) {
                maxDeletedLOC = currentDeletedLOC;
            }
        }

        //If a class has 0 revisions, its AvgLocAdded and AvgChurn are 0 (see initialization above).
        if(!javaClass.getMetrics().getAddedLinesList().isEmpty()) {
            avgLOC = 1.0*sumLOC/javaClass.getMetrics().getAddedLinesList().size();
        }
        if(!javaClass.getMetrics().getAddedLinesList().isEmpty() || !javaClass.getMetrics().getDeletedLinesList().isEmpty()) {
            avgChurn = 1.0*churn/(javaClass.getMetrics().getAddedLinesList().size() + javaClass.getMetrics().getDeletedLinesList().size());
        }
        if(!javaClass.getMetrics().getDeletedLinesList().isEmpty()) {
            avgLOC = 1.0*sumOfTheDeletedLOC/javaClass.getMetrics().getAddedLinesList().size();
        }

        javaClass.getMetrics().setLocAdded(sumLOC);
        javaClass.getMetrics().setMaxLocAdded(maxLOC);
        javaClass.getMetrics().setAvgLocAdded(avgLOC);
        javaClass.getMetrics().setChurn(churn);
        javaClass.getMetrics().setMaxChurn(maxChurn);
        javaClass.getMetrics().setAvgChurn(avgChurn);

    }



    public static void addSizeLabel(List<ReleaseCommits> releaseCommitsList) {

        for(ReleaseCommits rc: releaseCommitsList) {
            for(JavaClass javaClass: rc.getJavaClasses()) {
                String[] lines = javaClass.getContent().split("\r\n|\r|\n");
                javaClass.getMetrics().setSize(lines.length);
            }
        }
    }
}
