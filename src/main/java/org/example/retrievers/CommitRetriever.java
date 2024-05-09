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
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.example.models.Ticket;
import org.example.models.Version;
import org.example.utils.GitUtils;
import org.example.utils.RegularExpression;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommitRetriever {
    private Git git;
    private Repository repository;

    public CommitRetriever(String repositoryPath) {
        this.repository = GitUtils.getRepository(repositoryPath);
        this.git = new Git(repository);
    }
    /** to filter out commits that are associated with a specific ticket
     * It's iterates through a list of RevCommit objects and checks if the full message of each commit contain a certain
     * key related to a ticket. To check if the message contains a specific key it appears to be using a regular expression */
    private List<RevCommit> retrieveAssociatedCommits(@NotNull List<RevCommit> commits, Ticket ticket)  {
        ArrayList<RevCommit> associatedCommit = new ArrayList<>(); /** to store the associated commit with a ticket */
        for(RevCommit commit: commits) {
            if(RegularExpression.matchRegex(commit.getFullMessage(), ticket.getKey())) {
                associatedCommit.add(commit);
            }
        }
        return associatedCommit;
    }

    public List<RevCommit> retrieveAssociatedCommits2(@NotNull List<RevCommit> commits, Ticket ticket) throws GitAPIException {
        List<RevCommit> associatedCommit = new ArrayList<>();
        for(RevCommit commit: commits) {
            if(RegularExpression.matchRegex(commit.getFullMessage(), ticket.getKey())) {
                associatedCommit.add(commit);
            }
        }
        return associatedCommit;
    }
    public List<RevCommit> retrieveCommit(VersionRetriever versionRetriever) throws GitAPIException {
        Iterable<RevCommit> commitIterable = git.log().call();

        List<RevCommit> commits = new ArrayList<>();
        List<Version> projVersions = versionRetriever.getProjVersions();

        for(RevCommit commit: commitIterable) {
            commits.add(commit);
            if (!GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen()).isAfter(projVersions.get(projVersions.size()-1).getDate())){
                commits.add(commit);
            }
        }

        return commits;
    }
    /** Associate the tickets with the commits that reference them. Moreover, discard the tickets that don't have any commits.*/
    public List<Ticket> associateTicketAndCommit(VersionRetriever versionRetriever, CommitRetriever commitRetriever, List<Ticket> tickets) {
        try {
            List<RevCommit> commits = commitRetriever.retrieveCommit(versionRetriever);
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
    public void retrieveChanges(RevCommit commit) {
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
