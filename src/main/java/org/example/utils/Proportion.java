package org.example.utils;

import org.example.models.Ticket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Proportion {
    public static double computeProportionValue(@NotNull List<Ticket> consistentTickets) {

        double proportionSum = 0;
        int validatedCount = 0;
        for(Ticket ticket: consistentTickets) {
            /* P = (FV-IV)/(FV-OV) */
            if(ticket.getInjectedRelease() == null && ticket.getOpeningRelease() == null && ticket.getFixedRelease() == null)
                continue; /* Ignore the ticket that are inconsistent */
            int iv = ticket.getInjectedRelease().getIndex();
            int ov = ticket.getOpeningRelease().getIndex();
            int fv = ticket.getFixedRelease().getIndex();
            double prop;
            if(fv != ov && ov != iv) {
                prop = (1.0) * (fv - iv) / (fv - ov);

                proportionSum = proportionSum + prop;
                validatedCount++;
            }
        }
        return proportionSum/validatedCount;
    }

}