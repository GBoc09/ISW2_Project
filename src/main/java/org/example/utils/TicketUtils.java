package org.example.utils;

import org.example.models.Ticket;

import java.util.ArrayList;
import java.util.List;

public class TicketUtils {
    private TicketUtils() {}
    public static List<Ticket> getTicketsUntilRelease(List<Ticket> tickets, int versionBound) {
        List<Ticket> ticketList = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (ticket.getOpeningRelease().getIndex() <= versionBound) {
                ticketList.add(ticket);
            }
        }

        return ticketList;
    }
}