package org.example.main;

import org.example.models.Ticket;
import org.example.retrievers.CommitRetriever;
import org.example.retrievers.TicketRetriever;

import java.util.List;

public class Main {
    public static void main(String[] args) {

        String issueType = "Bug";
        String status = "closed";
        String resolution = "fixed";

        TicketRetriever bookkeeperRetriever = new TicketRetriever("BOOKKEEPER");
        List<Ticket> bookTickets = bookkeeperRetriever.getTickets();
        //TicketRetriever openjpaRetriever = new TicketRetriever("OPENJPA");
        CommitRetriever commitRetriever = new CommitRetriever("/home/giulia/Documenti/GitHub/bookkeeper");
        //commitRetriever.retrieveChangesFromTickets(bookTickets);


    }

}