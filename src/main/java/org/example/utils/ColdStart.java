package org.example.utils;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.example.enums.ProjectEnums;
import org.example.models.Ticket;
import org.example.retrievers.TicketRetriever;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ColdStart {
    private ColdStart() {}

    /** When the number of valid tickets used for compute the proportion value are less than a specific number, use the tickets of other
     * project.*/
    public static List<Ticket> getTicketForColdStart(ProjectEnums project) throws GitAPIException, IOException, URISyntaxException {
        TicketRetriever retriever = new TicketRetriever(project.toString(), true);

        return new ArrayList<>(retriever.getTickets());
    }

}