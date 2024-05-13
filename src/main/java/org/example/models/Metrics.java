package org.example.models;

import java.util.ArrayList;
import java.util.List;

public class Metrics {
    private final LOCMetrics addedLOCMetrics = new LOCMetrics();
    private final LOCMetrics deletedLOCMetrics = new LOCMetrics();
    private final LOCMetrics churnLOCMetrics = new LOCMetrics();
    private boolean buggyness = false;
    private int size;
    private final List<Integer> addedLinesOfCodeList = new ArrayList<>();
    private final List<Integer> deletedLinesOfCodeList = new ArrayList<>();
    private int fixedDefects = 0;

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
    public void setMaxLocDeleted(int maxLocAdded) {
        this.addedLOCMetrics.maxLoc = maxLocAdded;
    }

    public int getMaxLocDeleted() {
        return addedLOCMetrics.maxLoc;
    }

    public void setLocDeleted(int locAdded) {
        this.addedLOCMetrics.loc = locAdded;
    }

    public int getLocDeleted() {
        return addedLOCMetrics.loc;
    }

    public void setAvgLocDeleted(double avgLocAdded) {
        this.addedLOCMetrics.avgLoc = avgLocAdded;
    }

    public double getAvgLocDeleted() {
        return addedLOCMetrics.avgLoc;
    }


    public void setMaxLocAdded(int maxLocAdded) {
        this.addedLOCMetrics.maxLoc = maxLocAdded;
    }

    public int getMaxLocAdded() {
        return addedLOCMetrics.maxLoc;
    }

    public void setLocAdded(int locAdded) {
        this.addedLOCMetrics.loc = locAdded;
    }

    public int getLocAdded() {
        return addedLOCMetrics.loc;
    }

    public void setAvgLocAdded(double avgLocAdded) {
        this.addedLOCMetrics.avgLoc = avgLocAdded;
    }

    public double getAvgLocAdded() {
        return addedLOCMetrics.avgLoc;
    }

    public void setChurn(int churn) {
        this.churnLOCMetrics.loc = churn;
    }

    public int getChurn() {
        return this.churnLOCMetrics.loc;
    }

    public void setMaxChurn(int maxChurn) {
        this.churnLOCMetrics.maxLoc = maxChurn;
    }

    public int getMaxChurn() {
        return this.churnLOCMetrics.maxLoc;
    }

    public void setAvgChurn(double avgChurn) {
        this.churnLOCMetrics.avgLoc = avgChurn;
    }

    public double getAvgChurn() {
        return this.churnLOCMetrics.avgLoc;
    }
    public int getFixedDefects() {
        return fixedDefects;
    }

    public void updateFixedDefects() {
        this.fixedDefects = fixedDefects + 1;
    }

    private static class LOCMetrics {
        private int maxLoc;
        private int loc;
        private double avgLoc;
    }
}
