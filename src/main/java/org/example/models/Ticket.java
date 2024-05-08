package org.example.models;

import org.eclipse.jgit.revwalk.RevCommit;
import org.example.retrievers.VersionRetriever;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** With this class we want to create the object TICKET with its attributes.
 * Each ticket has :
 * a key that identifies the problem,
 * a creationDate that marks when the problem was found,
 * a resolutionDate that marks when the problem was solved,
 * an affectedVersion that are all the version between the injected version included and the fixed version  */

public class Ticket {
    private String key;
    private LocalDate creationDate;
    private LocalDate resolutionDate;
    private List<Version> affectedReleases;
    private Version openingRelease;
    private Version fixedRelease;
    private Version injectedRelease;
    private VersionRetriever versionRetriever;
    private List<RevCommit> associatedCommits;

    public Ticket(@NotNull String creationDate, @NotNull String resolutionDate, String key, List<Version> affectedReleases, @NotNull VersionRetriever versionRetriever) {
        this.creationDate = LocalDate.parse(creationDate.substring(0, 10));
        this.resolutionDate = LocalDate.parse(resolutionDate.substring(0, 10));
        this.key = key;
        setVersionRetriever(versionRetriever);
        setInjectedRelease(affectedReleases);
    }
    public VersionRetriever getVersionRetriever() {
        return versionRetriever;
    }
    public void setVersionRetriever(VersionRetriever versionRetriever) {
        if(versionRetriever == null) {
            throw new RuntimeException();
        }
        this.versionRetriever = versionRetriever;
    }
    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setAssociatedCommits(List<RevCommit> associatedCommits) {
        this.associatedCommits = associatedCommits;
    }

    public LocalDate getResolutionDate() {
        return resolutionDate;
    }
    public String getKey() {
        return key;
    }
    public List<Version> getAffectedReleases() {
        return affectedReleases;
    }
    public Version getOpeningRelease() {
        return openingRelease;
    }
    public void setOpeningRelease(Version openingRelease) {
        this.openingRelease = openingRelease;
    }
    public Version getFixedRelease() {
        return fixedRelease;
    }
    public void setFixedRelease(Version fixedRelease) {
        this.fixedRelease = fixedRelease;
        computeAffectedRelease();
    }
    public Version getInjectedRelease() {
        return injectedRelease;
    }
    public void setInjectedRelease(Version release) {
        this.injectedRelease = release;
        computeAffectedRelease();
    }
    private void setInjectedRelease(List<Version> affectedReleases) {
        if(!affectedReleases.isEmpty()) {
            this.injectedRelease = affectedReleases.get(0);
            computeAffectedRelease();
        } else {
            this.injectedRelease = null;
        }
    }
    public void computeAffectedRelease() {
        // Execute the method only if the ticket has fixed and injected release
        if(this.injectedRelease == null || this.fixedRelease == null) return;
        List<Version> releases = new ArrayList<>();
        for (Version versionInfo : versionRetriever.getProjVersions()) {
            if ((versionInfo.getIndex() >= this.injectedRelease.getIndex()) && (versionInfo.getIndex() < this.fixedRelease.getIndex())) {
                releases.add(versionInfo);
            }
        }
        this.affectedReleases = releases;
    }
    public List<RevCommit> getAssociatedCommits() {
        return associatedCommits;
    }
}
