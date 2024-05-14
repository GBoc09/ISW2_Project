package org.example.utils;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.example.enums.ProjectEnums;
import org.example.models.Ticket;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class Proportion {
    private static double coldStartProportionValue = -1;

    private Proportion() {}

    public static double computeColdStartProportionValue() throws GitAPIException, IOException, URISyntaxException {
        if(coldStartProportionValue != -1) return coldStartProportionValue;

        List<Double> proportionValueList = new ArrayList<>();

        for(ProjectEnums proj: ProjectEnums.values()) {
            double p = computeProportionValue(ColdStart.getTicketForColdStart(proj));
            if(p != 0)
                proportionValueList.add(p);
        }

        coldStartProportionValue = computeMedian(proportionValueList);
        return coldStartProportionValue;
    }

    private static double computeMedian(@NotNull List<Double> proportionValueList) {
        proportionValueList.sort(Double::compareTo);
        if(proportionValueList.size()%2 != 0) {
            return proportionValueList.get((proportionValueList.size()-1)/2);
        } else {
            double v1 = proportionValueList.get((proportionValueList.size()-1)/2);
            double v2 = proportionValueList.get(proportionValueList.size()/2);
            return v1+v2/2;
        }
    }

    public static double computeProportionValue(@NotNull List<Ticket> consistentTickets) {

        double proportionSum = 0;
        int validatedCount = 0;

        for(Ticket ticket: consistentTickets) {
            //P = (FV-IV)/(FV-OV)
            if(ticket.getInjectedRelease() == null && ticket.getOpeningRelease() == null && ticket.getFixedRelease() == null)
                continue; //Ignore the ticket that are inconsistent.
            int iv = ticket.getInjectedRelease().getIndex();
            int ov = ticket.getOpeningRelease().getIndex();
            int fv = ticket.getFixedRelease().getIndex();
            if(isAValidTicketForProportion(ticket)) {
                double prop;
                if(fv == ov) {
                    prop = (1.0) * (fv - iv);
                } else {
                    prop = (1.0) * (fv - iv) / (fv - ov);
                }
                proportionSum = proportionSum + prop;
                validatedCount++;
            }
        }
        if(validatedCount == 0)
            return 0;

        return proportionSum/validatedCount;
    }

    public static boolean isAValidTicketForProportion(@NotNull Ticket ticket) {
        if(ticket.getInjectedRelease() == null || ticket.getOpeningRelease() == null || ticket.getFixedRelease() == null) return false;

        int iv = ticket.getInjectedRelease().getIndex();
        int ov = ticket.getOpeningRelease().getIndex();
        return ov != iv;
    }
}