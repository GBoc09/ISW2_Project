package org.example.main;

import org.example.models.Ticket;
import org.example.retrievers.CommitRetriever;
import org.example.retrievers.TicketRetriever;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
       String bookkeeper = "BOOKKEEPER";
       String BOOKKEEPER_PATH = "/home/giulia/Documenti/GitHub/bookkeeper";

        String openjpa = "OPENJPA";
        String OPENJPA_PATH = "/home/giulia/Documenti/GitHub/openjpa";

        String issueType = "Bug";
        String state = "closed";
        String resolution = "fixed";

        TicketRetriever bookkeeperRetriever = new TicketRetriever(bookkeeper);
        CommitRetriever commitRetriever = new CommitRetriever("/home/giulia/Documenti/GitHub/bookkeeper");
        List<Ticket> bookkeeperTickets = bookkeeperRetriever.getTickets();

        System.out.println("sonar cloud");

    }
    //collegamento con sonar cloud first shot

}