package org.example.retrievers;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.example.models.Ticket;
import org.example.models.Version;
import org.example.utils.GitUtils;
import org.example.utils.RegularExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CommitRetriever {
    private Git git;
    private Repository repository;

    public CommitRetriever(String repositoryPath) {
        this.repository = GitUtils.getRepository(repositoryPath);
        this.git = new Git(repository);
    }

    private List<RevCommit> retrieveAssociatedCommit(@NotNull List<RevCommit> commits, Ticket ticket)  {

        ArrayList<RevCommit> associatedCommit = new ArrayList<>();
        for(RevCommit commit: commits) {
            if (commit.getFullMessage().contains(ticket.getKey())) {
                associatedCommit.add(commit);
            }
        }
        return associatedCommit;
    }

    public List<RevCommit> retrieveCommit() throws GitAPIException {
        Iterable<RevCommit> commitIterable = git.log().call();

        List<RevCommit> commits = new ArrayList<>();
        for(RevCommit commit: commitIterable) {
            commits.add(commit);
        }

        return commits;
    }
    /** Associate the tickets with the commits that reference them. Moreover, discard the tickets that don't have any commits.*/
    public List<Ticket> associateTicketAndCommit(CommitRetriever commitRetriever, ArrayList<Ticket> tickets) {
        try {
            List<RevCommit> commits = commitRetriever.retrieveCommit();
            for (Ticket ticket : tickets) {
                List<RevCommit> associatedCommits = commitRetriever.retrieveAssociatedCommit(commits, ticket);
                ticket.setAssociatedCommits(associatedCommits);
            }
            tickets.removeIf(ticket -> ticket.getAssociatedCommits().isEmpty()/* || (ticket.getOpeningRelease().getIndex() == 0)*/);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

        return tickets;
    }
}
