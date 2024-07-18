package org.example.models;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.List;

public class ReleaseInfo {
    private final Version release;
    private final List<RevCommit> commits;
    private final RevCommit lastCommit;

    private List<JavaClass> javaClasses;
    private int buggyClasses;

    public ReleaseInfo(Version release, List<RevCommit> commits, RevCommit lastCommit) {
        this.release = release;
        this.commits = commits;
        this.lastCommit = lastCommit;
        this.javaClasses = null;
    }


    public Version getRelease() {
        return release;
    }

    public List<RevCommit> getCommits() {
        return commits;
    }

    public RevCommit getLastCommit() {
        return lastCommit;
    }

    public List<JavaClass> getJavaClasses() {
        return javaClasses;
    }


    public void setJavaClasses(List<JavaClass> javaClasses) {
        this.javaClasses = javaClasses;
    }
    public int getBuggyClasses() {
        return buggyClasses;
    }

    public void setBuggyClasses(int buggyClasses) {
        this.buggyClasses = buggyClasses;
    }
}