package org.example.models;

public class LOCMetrics {
    private int maxLoc;
    private int loc;
    private double avgLoc;

    public LOCMetrics(int maxLoc, int loc, double avgLoc) {
        this.maxLoc = maxLoc;
        this.loc = loc;
        this.avgLoc = avgLoc;
    }
}
