package com.punanito.ctraderbridge.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class VolumeUtils {

    public long calculatePositionSize(
            double accountBalance,
            double riskPercent,
            double entryPrice,
            double stopLossPrice,
            double pipValue,
            long lotSize,
            int digits) {

        // ile ryzykujemy
        double riskAmount = accountBalance * (riskPercent / 100.0);

        double pips = Math.abs(entryPrice - stopLossPrice) * Math.pow(10, digits);

        double lots = riskAmount / (pips * pipValue);

        double units = lots * lotSize;

        // minimalny wolumen 1000 (zależnie od brokera)
        long finalVolume = Math.max(1000, Math.round(units));

        System.out.println("Volume units: " + finalVolume + " (" + lots + " lots)");

        return finalVolume;
    }

}
