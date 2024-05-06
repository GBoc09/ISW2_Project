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

    public @Nullable List<RevCommit> retrieveAssociatedCommits(@NotNull List<RevCommit> commits, Ticket ticket) throws GitAPIException {

        Iterable<RevCommit> commitIterable = git.log().call();
        for(RevCommit commit: commitIterable)
            commits.add(commit);

        List<RevCommit> associatedCommit = new ArrayList<>();
        for(RevCommit commit: commits){
            if(RegularExpression.matchRegex(commit.getFullMessage(), ticket.getKey())){
                associatedCommit.add(commit);
            }
        }
        return associatedCommit;
    }

    public List<RevCommit> retrieveCommit(VersionRetriever versionRetriever) throws GitAPIException {
        Iterable<RevCommit> commitIterable = git.log().call();

        List<RevCommit> commits = new ArrayList<>();
        List<Version> projectVersion = versionRetriever.getProjVersions();
        for(RevCommit commit: commitIterable) {
            if(!GitUtils.castToLocalDate(commit.getCommitterIdent().getWhen()).isAfter(projectVersion.get(projectVersion.size()-1).getDate())) {
                commits.add(commit);
            }
        }

        return commits;
    }
    public List<Ticket> associateTicketAndCommit(VersionRetriever versionRetriever, CommitRetriever commitRetriever, List<Ticket> tickets) {
        try {
            List<RevCommit> commits = commitRetriever.retrieveCommit(versionRetriever);
            for (Ticket ticket : tickets) {
                List<RevCommit> associatedCommits = commitRetriever.retrieveAssociatedCommits(commits, ticket);
                ticket.setAssociatedCommits(associatedCommits);
                //GitUtils.printCommit(associatedCommits);
            }
            tickets.removeIf(ticket -> ticket.getAssociatedCommits().isEmpty());
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
        return tickets;
    }

    public Git getGit() {
        return this.git;
    }

    public void setGit(Git git) {
        this.git = git;
    }

    public Repository getRepository() {
        return this.repository;
    }

    public void setRepository(Repository repository){
        this.repository = repository;
    }
}
