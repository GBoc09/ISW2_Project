package org.example.utils;

import org.eclipse.jgit.revwalk.RevCommit;
import org.example.models.ChangedJavaClass;
import org.example.models.JavaClass;
import org.example.models.ReleaseCommits;
import org.example.models.Version;

import java.util.List;
import java.util.Objects;

public class JavaClassUtils {
    private void JavaClassUtil() {}

    public static void updateJavaBuggyness(ChangedJavaClass className, List<ReleaseCommits> releaseCommitsList, List<Version> affectedReleases, RevCommit commit) {

        for(ReleaseCommits rc: releaseCommitsList) { //TODO Tra le affected release potrebbero esserci release senza commit
            if(affectedReleases.contains(rc.getRelease())) { //Get the affected release and update the buggyness of the java class
                List<JavaClass> javaClasses = rc.getJavaClasses(); //Get the java classes of the release
                findClassAndSetBuggyness(className, javaClasses, commit);
            }
        }
    }

    /**
     * This method find the modified class into the class of the release, set its buggyness to true and associate the commit to the class.
     *
     * @param className   the name of the searched class
     * @param javaClasses the list of java classes in the release
     * @param commit the commit that modified the searched class
     */
    private static void findClassAndSetBuggyness(ChangedJavaClass className, List<JavaClass> javaClasses, RevCommit commit) {
        for(JavaClass javaClass: javaClasses) {
            if(Objects.equals(javaClass.getName(), className.getJavaClassName())) {
                javaClass.getMetrics().setClassBuggyness();
                javaClass.addCommit(commit);
                return;
            }
        }
    }
}
