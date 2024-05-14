package org.example.utils;

import org.eclipse.jgit.revwalk.RevCommit;
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
    public static List<Ticket> getTicketsUntilRelease(List<Ticket> tickets, int versionBound) {
        List<Ticket> ticketList = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (ticket.getOpeningRelease().getIndex() <= versionBound) {
                ticketList.add(ticket);
            }
        }

        return ticketList;
    }
    public static @NotNull List<RevCommit> getAssociatedCommit(List<Ticket> tickets) {
        List<RevCommit> commits = new ArrayList<>();

        for(Ticket t: tickets) {
            commits.addAll(t.getAssociatedCommits());
        }

        return commits;
    }
}