package org.example.utils;

import org.example.enums.ProjectEnums;
import org.example.models.Ticket;
import org.example.retrievers.TicketRetriever;

import java.util.ArrayList;
import java.util.List;

public class ColdStart {
    /**When the number of valid tickets used for compute the proportion value are less than 5, use the tickets of other
     * project.*/
    public static List<Ticket> coldStart() {
        List<Ticket> consistentTickets = new ArrayList<>();
        for(ProjectEnums project: ProjectEnums.values()) {
            TicketRetriever retriever = new TicketRetriever(project.toString(), true);
            consistentTickets.addAll(retriever.getTickets());
        }

        return consistentTickets;
    }

}