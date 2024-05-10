package org.example.models;

import java.util.List;

public class Metrics {
    private boolean buggyness = false;
    private int size;
    private List<Integer> addedLinesOfCodeList;
    private List<Integer> deletedLinesOfCodeList;

    public Metrics() {}

    public void setClassBuggyness() {
        this.buggyness = true;
    }

    public boolean isBuggyness() {
        return buggyness;
    }

    public String isBuggy() {
        if(buggyness) return "True";

        return "False";
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<Integer> getAddedLinesList() {
        return addedLinesOfCodeList;
    }

    public List<Integer> getDeletedLinesList() {
        return deletedLinesOfCodeList;
    }
}
