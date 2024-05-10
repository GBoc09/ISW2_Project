package org.example.retrievers;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.example.models.ReleaseCommits;
import org.example.models.Ticket;
import org.example.models.Version;
import org.example.utils.GitUtils;
import org.example.utils.RegularExpression;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommitRetriever {
    private Git git;
    private Repository repository;
    VersionRetriever versionRetriever;
    List<RevCommit> commitList;

    public CommitRetriever(String repositoryPath, VersionRetriever versionRetriever) {
        this.repository = GitUtils.getRepository(repositoryPath);
        this.git = new Git(repository);
        this.versionRetriever = versionRetriever;
    }
    /** to filter out commits that are associated with a specific ticket
     * It's iterates through a list of RevCommit objects and checks if the full message of each commit contain a certain
     * key related to a ticket. To check if the message contains a specific key it appears to be using a regular expression */
    private List<RevCommit> retrieveAssociatedCommits(@NotNull List<RevCommit> commits, Ticket ticket)  {
        ArrayList<RevCommit> associatedCommit = new ArrayList<>(); /* to store the associated commit with a ticket */
        for(RevCommit commit: commits) {
            if(RegularExpression.matchRegex(commit.getFullMessage(), ticket.getKey())) {
                associatedCommit.add(commit);
            }
        }
        return associatedCommit;
    }

    public List<RevCommit> retrieveCommit() throws GitAPIException {
        Iterable<RevCommit> commitIterable = git.log().call();

        List<RevCommit> commits = new ArrayList<>();
        List<Version> projVersions = versionRetriever.getProjVersions();

        for(RevCommit commit: commitIterable) {
            commits.add(commit);
            if (!GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen()).isAfter(projVersions.get(projVersions.size()-1).getDate())){
                commits.add(commit);
            }
        }
        this.commitList = commits;
        return commits;
    }
    /** Associate the tickets with the commits that reference them. Moreover, discard the tickets that don't have any commits.*/
    public List<Ticket> associateTicketAndCommit(VersionRetriever versionRetriever, CommitRetriever commitRetriever, List<Ticket> tickets) {
        try {
            List<RevCommit> commits = commitRetriever.retrieveCommit();
            for (Ticket ticket : tickets) {
                List<RevCommit> associatedCommits = commitRetriever.retrieveAssociatedCommits(commits, ticket);
                ticket.setAssociatedCommits(associatedCommits);
            }
            tickets.removeIf(ticket -> ticket.getAssociatedCommits().isEmpty());
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

        return tickets;
    }

    public List<ReleaseCommits> getReleaseCommits(VersionRetriever versionRetriever, List<RevCommit> commits) throws GitAPIException, IOException {
        List<ReleaseCommits> releaseCommits = new ArrayList<>();
        LocalDate date = LocalDate.of(1900, 1, 1);
        for(Version versionInfo: versionRetriever.getProjVersions()) {
            ReleaseCommits releaseCommit = GitUtils.getCommitsOfRelease(commits, versionInfo, date);
            if(releaseCommit != null) {
                Map<String, String> javaClasses = getClasses(releaseCommit.getLastCommit());
                releaseCommit.setJavaClasses(javaClasses);
                releaseCommits.add(releaseCommit);
            }
            date = versionInfo.getDate();
        }

        return releaseCommits;
    }

    private Map<String, String> getClasses(RevCommit commit) throws IOException {

        Map<String, String> javaClasses = new HashMap<>();

        RevTree tree = commit.getTree();	//We get the tree of the files and the directories that were belong to the repository when commit was pushed
        TreeWalk treeWalk = new TreeWalk(this.repository);	//We use a TreeWalk to iterate over all files in the Tree recursively
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);

        while(treeWalk.next()) {
            //We are keeping only Java classes that are not involved in tests
            if(treeWalk.getPathString().contains(".java") && !treeWalk.getPathString().contains("/test/")) {
                //We are retrieving (name class, content class) couples
                javaClasses.put(treeWalk.getPathString(), new String(this.repository.open(treeWalk.getObjectId(0)).getBytes(), StandardCharsets.UTF_8));
            }
        }
        treeWalk.close();

        return javaClasses;

    }



    public void retrieveChangesFromTickets(List<Ticket> tickets) {
        for(Ticket ticket: tickets) {
            retrieveChanges(ticket.getAssociatedCommits());
        }
    }

    private void retrieveChanges(List<RevCommit> commits) {
        for(RevCommit commit: commits) {
            retrieveChanges(commit);
        }
    }

    /** This method allows us to examine the changes introduced by by a specific commit in a Git repo by printing out the
     * information about each change.
     * RevCommit represents the commit for which changes need to be retrieved.
     * ObjectReader to read    Git objects from the repo.
     * CanonicalTreeParser is an object for both old and the new trees of the commit. The old tree represents the states
     * before the commit, while the new tree represents the  state after the commit. */
    private void retrieveChanges(RevCommit commit) {
        try {
            ObjectReader reader = git.getRepository().newObjectReader();
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            ObjectId oldTree = git.getRepository().resolve(commit.getName() + "~1^{tree}");
            oldTreeIter.reset(reader, oldTree);
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            ObjectId newTree = git.getRepository().resolve(commit.getName() + "^{tree}");
            newTreeIter.reset(reader, newTree);

            DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE); /** to format the differences between the old and the new tree
            DisableOutputStream.INSTANCE to suppress the output of the formatter, indicating that we're only interested in scanning the differences programmatically.*/
            diffFormatter.setRepository(git.getRepository());
            List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);

            for (DiffEntry entry : entries) {
                System.out.println(entry.getNewPath() + " " + entry.getChangeType());

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
