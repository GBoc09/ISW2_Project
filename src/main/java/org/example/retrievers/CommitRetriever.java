package org.example.retrievers;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.example.models.*;
import org.example.utils.GitUtils;
import org.example.utils.JavaClassUtils;
import org.example.utils.RegularExpression;
import org.example.utils.VersionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public class CommitRetriever {
    private final Git git;
    private final Repository repository;
    private final VersionRetriever versionRetriever;
    private List<RevCommit> commitList;

    public CommitRetriever(String repositoryPath, VersionRetriever versionRetriever) throws IOException {
        this.repository = GitUtils.getRepository(repositoryPath);
        this.git = new Git(repository);
        this.versionRetriever = versionRetriever;
    }
    /** to filter out commits that are associated with a specific ticket */
    private @NotNull List<RevCommit> retrieveAssociatedCommits(@NotNull List<RevCommit> commits, Ticket ticket) {
        List<RevCommit> associatedCommit = new ArrayList<>();
        for(RevCommit commit: commits) {
            if(RegularExpression.matchRegex(commit.getFullMessage(), ticket.getKey())) {
                associatedCommit.add(commit);
            }
        }
        return associatedCommit;
    }


    /** This class is used to manage tickets and commits.
    */
    public List<RevCommit> retrieveCommit() throws GitAPIException {
        if(commitList != null) return commitList;

        Iterable<RevCommit> commitIterable = git.log().call();

        List<RevCommit> commits = new ArrayList<>();
        List<Version> projVersions = versionRetriever.getProjVersions();
        Version lastVersion = projVersions.get(projVersions.size()-1);

        for(RevCommit commit: commitIterable) {
            if (!GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen()).isAfter(lastVersion.getDate())){
                commits.add(commit);
            }
        }

        commits.sort(Comparator.comparing(o -> o.getCommitterIdent().getWhen()));

        this.commitList = commits;

        return commits;
    }
    /** Associate the tickets with the commits that reference them. Moreover, discard the tickets that don't have any commits.*/
    public void associateTicketAndCommit(@NotNull List<Ticket> tickets) throws GitAPIException {
        List<RevCommit> commits = this.retrieveCommit();
        for (Ticket ticket : tickets) {
            List<RevCommit> associatedCommits = this.retrieveAssociatedCommits(commits, ticket);
            List<RevCommit> consistentCommits = new ArrayList<>();

            for(RevCommit commit: associatedCommits) {
                LocalDate when = GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen());

                if(!ticket.getFixedRelease().getDate().isBefore(when) && //commitDate <= fixedVersionDate
                        !ticket.getInjectedRelease().getDate().isAfter(when)) { //commitDate > injectedVersionDate
                    consistentCommits.add(commit);
                }
            }
            ticket.setAssociatedCommits(consistentCommits);
        }
        tickets.removeIf(ticket -> ticket.getAssociatedCommits().isEmpty()); //Discard tickets that have no associated commits
    }

    /** This method retrieves release commits associated with each version of the project, along with their associated
     * Java classes and updates the commit information for those Java classes. */
    public List<ReleaseInfo> getReleaseCommits(@NotNull VersionRetriever versionRetriever, List<RevCommit> commits) throws IOException {

        List<ReleaseInfo> releaseCommits = new ArrayList<>();
        LocalDate lowerBound = LocalDate.of(1900, 1, 1);
        for(Version version : versionRetriever.getProjVersions()) {
            ReleaseInfo releaseCommit = GitUtils.getCommitsOfRelease(commits, version, lowerBound);
            if(releaseCommit != null) {
                List<JavaClass> javaClasses = getClasses(releaseCommit.getLastCommit());
                releaseCommit.setJavaClasses(javaClasses);
                releaseCommits.add(releaseCommit);
                JavaClassUtils.updateJavaClassCommits(this, releaseCommit.getCommits(), javaClasses);
            }
            lowerBound = version.getDate();
        }

        return releaseCommits;
    }

    /** This method ensures that each version  object contains a list of commits that occurred between its date and the
     * date of the previous version, effectively associating commits with their respective versions. */
    public void associateCommitAndVersion(List<Version> projVersions) throws GitAPIException {

        LocalDate lowerBound = LocalDate.of(1900, 1, 1);
        for(Version version: projVersions) {
            for(RevCommit commit: retrieveCommit()) {
                LocalDate commitDate = GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen());
                if ((commitDate.isBefore(version.getDate()) || commitDate.isEqual(version.getDate())) && commitDate.isAfter(lowerBound)) {
                    version.addCommitToList(commit);
                }
            }
            lowerBound = version.getDate();
        }
        versionRetriever.deleteVersionWithoutCommits();
    }

    /** This method scans the commit's tree for Java class file that are not test classes, retrieves the next release/version
     * after the commit, and creates JavaClass objects for each eligible file associating them with the next release. */
    private @NotNull List<JavaClass> getClasses(@NotNull RevCommit commit) throws IOException {

        List<JavaClass> javaClasses = new ArrayList<>();

        RevTree tree = commit.getTree();    //We get the tree of the files and the directories that were belong to the repository when commit was pushed
        TreeWalk treeWalk = new TreeWalk(this.repository);    //We use a TreeWalk to iterate over all files in the Tree recursively
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);

        while(treeWalk.next()) {
            //We are keeping only Java classes that are not involved in tests
            if(treeWalk.getPathString().contains(".java") && !treeWalk.getPathString().contains("/test/")) {
                //We are retrieving (name class, content class) couples
                Version release = VersionUtils.retrieveNextRelease(versionRetriever, GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen()));

                if(release == null) break; //When there isn't a version after the commit, ignore that commit.

                javaClasses.add(new JavaClass(
                        treeWalk.getPathString(),
                        new String(this.repository.open(treeWalk.getObjectId(0)).getBytes(), StandardCharsets.UTF_8),
                        release));
            }
        }
        treeWalk.close();

        return javaClasses;
    }

   /** This method essentially compares the current commit with its parent to determine which Java classes have been
    * changed and then creates ChangedJavaClass objects to represent these changes.  */
    public List<ChangedJavaClass> retrieveChanges(@NotNull RevCommit commit) throws IOException {
        List<ChangedJavaClass> changedJavaClassList = new ArrayList<>();
        try(DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

            RevCommit parentComm = commit.getParent(0);

            diffFormatter.setRepository(this.repository);
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);

            List<DiffEntry> entries = diffFormatter.scan(parentComm.getTree(), commit.getTree());

            for (DiffEntry entry : entries) {
                ChangedJavaClass newChangedJavaClass = new ChangedJavaClass(entry.getNewPath());
                changedJavaClassList.add(newChangedJavaClass);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            //commit has no parents: this is the first commit, so add all classes
            List<JavaClass> javaClasses = getClasses(commit);
            changedJavaClassList = JavaClassUtils.createChangedJavaClass(javaClasses);
        }

        return changedJavaClassList;
    }

    public void computeAddedAndDeletedLinesList(@NotNull JavaClass javaClass) throws IOException {

        for(RevCommit comm : javaClass.getCommits()) {
            try(DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

                RevCommit parentComm = comm.getParent(0);

                diffFormatter.setRepository(this.repository);
                diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);

                List<DiffEntry> diffs = diffFormatter.scan(parentComm.getTree(), comm.getTree());
                for(DiffEntry entry : diffs) {
                    if(entry.getNewPath().equals(javaClass.getName())) {
                        javaClass.getMetrics().getAddedLinesList().add(getAddedLines(diffFormatter, entry));
                        javaClass.getMetrics().getDeletedLinesList().add(getDeletedLines(diffFormatter, entry));
                        break;
                    }
                }

            } catch(ArrayIndexOutOfBoundsException e) {
                //commit has no parents: skip this commit, return an empty list and go on
            }
        }
    }
    private int getAddedLines(@NotNull DiffFormatter diffFormatter, DiffEntry entry) throws IOException {

        int addedLines = 0;
        for(Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
            addedLines += edit.getEndB() - edit.getBeginB();
        }
        return addedLines;

    }

    private int getDeletedLines(@NotNull DiffFormatter diffFormatter, DiffEntry entry) throws IOException {

        int deletedLines = 0;
        for(Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
            deletedLines += edit.getEndA() - edit.getBeginA();
        }
        return deletedLines;

    }


}