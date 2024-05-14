package org.example.models;

import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** With this class our intent is to create the object VERSION, whit its attributes
 * Each version has an ID, a name and a creation date. */
public class Version {
    String id;
    int index;
    String name;
    LocalDate date;
    List<RevCommit> commitList = new ArrayList<>();

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
    public boolean isCommitListEmpty() {
        return commitList.isEmpty();
    }

    public void addCommitToList(RevCommit commit) {
        this.commitList.add(commit);
    }
    public Version(String id, String name, @NotNull LocalDate date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }
}