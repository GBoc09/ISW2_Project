package org.example.utils;

import org.example.models.Ticket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TicketUtils {
    private TicketUtils() {}
    public static void printTickets(@NotNull List<Ticket> tickets) {
        for(Ticket ticket: tickets) {
            if(ticket.getInjectedRelease() != null && ticket.getOpeningRelease() != null && ticket.getFixedRelease() != null) {
                System.out.println(ticket.getKey() + "," + ticket.getCreationDate() + "," + ticket.getResolutionDate() + "  ->  " +
                        "Injected Version:" + ticket.getInjectedRelease().getName() + " - " +
                        "Opening Version:" + ticket.getOpeningRelease().getName() + " - " +
                        "Fix Version:" + ticket.getFixedRelease().getName());
            }
        }
    }
    public static void sortTickets(List<Ticket> tickets) {
        tickets.sort(Comparator.comparing(Ticket::getCreationDate));
    }
}
