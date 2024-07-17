package org.example.models;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.ArrayList;
import java.util.List;

public class JavaClass {
    private final String name;
    private final String content;
    private final Version release;
    private final List<RevCommit> commits = new ArrayList<>();
    private final Metrics metrics = new Metrics();



    public JavaClass(String name, String content, Version release) {
        this.name = name;
        this.content = content;
        this.release = release;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }
    public List<RevCommit> getCommits() {
        return commits;
    }

    public void addCommit(RevCommit commit) {
        this.commits.add(commit);
    }

    public Metrics getMetrics() {
        return metrics;
    }
    public Version getRelease(){return release;}
}