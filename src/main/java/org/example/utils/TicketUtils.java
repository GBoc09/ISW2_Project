package org.example.utils;

import org.eclipse.jgit.revwalk.RevCommit;
import org.example.models.Ticket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
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