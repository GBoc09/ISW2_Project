package org.example.main;

import org.example.models.Ticket;
import org.example.retrievers.TicketRetriever;

import java.util.List;

public class Main {
    public static void main(String[] args) {

        String issueType = "Bug";
        String status = "closed";
        String resolution = "fixed";

        TicketRetriever bookkeeperRetriever = new TicketRetriever("BOOKKEEPER");
        TicketRetriever openjpaRetriever = new TicketRetriever("OPENJPA");

        List<Ticket> bookTickets = bookkeeperRetriever.getTickets();
    }

}