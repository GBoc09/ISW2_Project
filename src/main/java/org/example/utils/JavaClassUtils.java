package org.example.utils;

import org.eclipse.jgit.revwalk.RevCommit;
import org.example.models.ChangedJavaClass;
import org.example.models.JavaClass;
import org.example.models.ReleaseCommits;
import org.example.models.Version;
import org.example.retrievers.CommitRetriever;
import org.example.retrievers.VersionRetriever;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JavaClassUtils {
    private void JavaClassUtil() {}

    public static void updateJavaBuggyness(ChangedJavaClass className, @NotNull List<ReleaseCommits> releaseCommitsList, List<Version> affectedReleases) {

        for(ReleaseCommits rc: releaseCommitsList) {
            if(affectedReleases.contains(rc.getRelease())) { //Get the affected release and update the buggyness of the java class
                List<JavaClass> javaClasses = rc.getJavaClasses(); //Get the java classes of the release
                findClassAndSetBuggyness(className, javaClasses);
            }
        }
    }

    /**
     * This method find the modified class into the class of the release, set its buggyness to true and associate the commit to the class.
     *
     * @param className   the name of the searched class
     * @param javaClasses the list of java classes in the release
    */
    private static void findClassAndSetBuggyness(ChangedJavaClass className, @NotNull List<JavaClass> javaClasses) {
        for(JavaClass javaClass: javaClasses) {
            if(Objects.equals(javaClass.getName(), className.getJavaClassName())) {
                javaClass.getMetrics().setClassBuggyness();
                return;
            }
        }
    }

    public static void updateNumberOfFixedDefects(VersionRetriever versionRetriever, @NotNull RevCommit lastCommit, List<ChangedJavaClass> classChangedList, List<ReleaseCommits> releaseCommitsList) {
        ReleaseCommits releaseCommits = VersionUtils.retrieveCommitRelease(
                versionRetriever,
                GitUtils.castToLocalDate(lastCommit.getCommitterIdent().getWhen()),
                releaseCommitsList);

        if (releaseCommits != null) {

            for (ChangedJavaClass javaClass : classChangedList) {
                updateFixedDefects(releaseCommits, javaClass.getJavaClassName());
            }
        }
    }

    private static void updateFixedDefects(@NotNull ReleaseCommits releaseCommits, String className) {

        for(JavaClass javaClass: releaseCommits.getJavaClasses()) {
            if(Objects.equals(javaClass.getName(), className)) {
                javaClass.getMetrics().updateFixedDefects();

                return;
            }
        }
    }

    public static void updateJavaClassCommits(CommitRetriever commitRetriever, @NotNull List<RevCommit> commits, List<JavaClass> javaClasses) {

        for(RevCommit commit: commits) {
            List<ChangedJavaClass> changedJavaClassList = commitRetriever.retrieveChanges(commit);

            for(ChangedJavaClass changedJavaClass: changedJavaClassList) {
                for(JavaClass javaClass: javaClasses) {
                    if (Objects.equals(changedJavaClass.getJavaClassName(), javaClass.getName())) {
                        javaClass.addCommit(commit);
                        break;
                    }
                }
            }
        }
    }

    public static List<ChangedJavaClass> createChangedJavaClass(List<JavaClass> javaClasses) {
        List<ChangedJavaClass> changedJavaClassList = new ArrayList<>();

        for(JavaClass javaClass: javaClasses) {
            changedJavaClassList.add(new ChangedJavaClass(
                    javaClass.getName()
            ));
        }

        return changedJavaClassList;
    }
}