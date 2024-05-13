package org.example.retrievers;

import org.eclipse.jgit.revwalk.RevCommit;
import org.example.models.ChangedJavaClass;
import org.example.models.JavaClass;
import org.example.models.ReleaseInfo;
import org.example.models.Ticket;
import org.example.utils.GitUtils;
import org.example.utils.JavaClassUtils;
import org.example.utils.VersionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MetricsRetriever {
    private MetricsRetriever(){}

    public static void computeBuggyness(List<ReleaseInfo> releaseInfoList, @NotNull List<Ticket> tickets, CommitRetriever commitRetriever, VersionRetriever versionRetriever) {
        initializeBuggyness(releaseInfoList);
        for (Ticket ticket : tickets) {
            computeBuggyness(releaseInfoList, commitRetriever, versionRetriever, ticket);

        }
    }
    private static void computeFixedDefects(List<ReleaseInfo> releaseInfoList, @NotNull List<Ticket> tickets, CommitRetriever commitRetriever, VersionRetriever versionRetriever) {
        for(Ticket ticket: tickets){
            //For each ticket, update the number of fixed defects of classes present in the last commit of the ticket (the fixed commit).
            JavaClassUtils.updateNumberOfFixedDefects(versionRetriever, ticket.getAssociatedCommits(), releaseInfoList, commitRetriever);
        }
    }
    private static void computeBuggyness(List<ReleaseInfo> releaseInfoList, CommitRetriever commitRetriever, VersionRetriever versionRetriever, @NotNull Ticket ticket) {
        for (RevCommit commit : ticket.getAssociatedCommits()) {
            //For each commit associated to a ticket, set all classes touched in commit as buggy in all the affected versions of the ticket.
            ReleaseInfo releaseInfo = VersionUtils.retrieveCommitRelease(
                    versionRetriever,
                    GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen()),
                    releaseInfoList);

            if (releaseInfo != null) {
                List<ChangedJavaClass> classChangedList = commitRetriever.retrieveChanges(commit);

                for (ChangedJavaClass javaClass : classChangedList) {
                    JavaClassUtils.updateJavaBuggyness(javaClass, releaseInfoList, ticket.getAffectedReleases());
                }
            }
        }
    }

    private static void initializeBuggyness(List<ReleaseInfo> releaseInfoList) {
        for(ReleaseInfo releaseInfo: releaseInfoList) {
            for(JavaClass javaClass: releaseInfo.getJavaClasses()) {
                javaClass.getMetrics().setClassBuggyness(false);
            }
        }
    }

    public static void computeMetrics(List<ReleaseInfo> releaseInfoList, @NotNull List<Ticket> tickets, CommitRetriever commitRetriever, VersionRetriever versionRetriever) {
        //Add the size metric in all the classes of the release.
        addSizeLabel(releaseInfoList);
        try {
            computeBuggyness(releaseInfoList, tickets, commitRetriever, versionRetriever);
            computeFixedDefects(releaseInfoList, tickets, commitRetriever, versionRetriever);
            computeLocData(releaseInfoList, commitRetriever);
            computeNAuth(releaseInfoList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void computeNAuth(@NotNull List<ReleaseInfo> releaseInfoList) {

        for(ReleaseInfo rc: releaseInfoList){
            for (JavaClass javaClass : rc.getJavaClasses()) {
                List<String> classAuthors = new ArrayList<>();

                for (RevCommit commit : javaClass.getCommits()) {
                    if (!classAuthors.contains(commit.getAuthorIdent().getName())) {
                        classAuthors.add(commit.getAuthorIdent().getName());
                    }

                }
                javaClass.getMetrics().setnAuth(classAuthors.size());

            }
        }
    }

    private static void computeLocData(@NotNull List<ReleaseInfo> releaseInfoList, CommitRetriever commitRetriever) throws IOException {
        for(ReleaseInfo rc: releaseInfoList) {
            for (JavaClass javaClass : rc.getJavaClasses()) {
                commitRetriever.computeAddedAndDeletedLinesList(javaClass);
                computeLocAndChurnMetrics(javaClass);
            }
        }
    }
    private static void computeLocAndChurnMetrics(@NotNull JavaClass javaClass) {

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
        int numberOfRevision = javaClass.getCommits().size();
        if(!javaClass.getMetrics().getAddedLinesList().isEmpty()) {
            avgLOC = 1.0*sumLOC/numberOfRevision;
        }
        if(!javaClass.getMetrics().getAddedLinesList().isEmpty() || !javaClass.getMetrics().getDeletedLinesList().isEmpty()) {
            avgChurn = 1.0*churn/numberOfRevision;
        }
        if(!javaClass.getMetrics().getDeletedLinesList().isEmpty()) {
            avgDeletedLOC = 1.0*sumOfTheDeletedLOC/numberOfRevision;
        }

        javaClass.getMetrics().setLocAdded(sumLOC);
        javaClass.getMetrics().setMaxLocAdded(maxLOC);
        javaClass.getMetrics().setAvgLocAdded(avgLOC);
        javaClass.getMetrics().setChurn(churn);
        javaClass.getMetrics().setMaxChurn(maxChurn);
        javaClass.getMetrics().setAvgChurn(avgChurn);

        javaClass.getMetrics().setLocDeleted(sumOfTheDeletedLOC);
        javaClass.getMetrics().setMaxLocDeleted(maxDeletedLOC);
        javaClass.getMetrics().setAvgLocDeleted(avgDeletedLOC);

    }



    public static void addSizeLabel(@NotNull List<ReleaseInfo> releaseInfoList) {

        for(ReleaseInfo rc: releaseInfoList) {
            for(JavaClass javaClass: rc.getJavaClasses()) {
                String[] lines = javaClass.getContent().split("\r\n|\r|\n");
                javaClass.getMetrics().setSize(lines.length);
            }
        }
    }
}
