package org.example.utils;

import org.example.enums.ProjectEnums;
import org.example.models.Ticket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Proportion {
    private static double coldStartProportionValue = -1;

    private Proportion() {}

    public static double computeColdStartProportionValue() {
        if(coldStartProportionValue != -1) return coldStartProportionValue;

        List<Double> proportionValueList = new ArrayList<>();

        for(ProjectEnums proj: ProjectEnums.values()) {
            proportionValueList.add(computeProportionValue(ColdStart.getTicketForColdStart(proj)));
        }

        coldStartProportionValue = computeMedian(proportionValueList);
        return coldStartProportionValue;
    }

    private static double computeMedian(List<Double> proportionValueList) {
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
                throw new RuntimeException(); //create an exception for inconsistency ticket list: there is an inconsistent ticket in the consistent list
            int iv = ticket.getInjectedRelease().getIndex();
            int ov = ticket.getOpeningRelease().getIndex();
            int fv = ticket.getFixedRelease().getIndex();
            if(isAValidTicketForProportion(ticket)) {
                double prop = (1.0) * (fv - iv) / (fv - ov);
                proportionSum = proportionSum + prop;
                validatedCount++;
            }
        }

        return proportionSum/validatedCount;
    }

    public static boolean isAValidTicketForProportion(Ticket ticket) {
        int iv = ticket.getInjectedRelease().getIndex();
        int ov = ticket.getOpeningRelease().getIndex();
        int fv = ticket.getFixedRelease().getIndex();
        return fv != ov && ov != iv;
    }

}
