package org.example.main;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.example.models.ReleaseCommits;
import org.example.models.Ticket;
import org.example.retrievers.CommitRetriever;
import org.example.retrievers.TicketRetriever;
import org.example.utils.TicketUtils;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        new ExecutionFlow("BOOKKEEPER");
        //new ExecutionFlow("OPENJPA");

    }
}