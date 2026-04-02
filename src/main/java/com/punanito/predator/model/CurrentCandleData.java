package com.punanito.predator.model;

import java.math.BigDecimal;

public class CurrentCandleData {
    private CandleColor color;
    private BigDecimal priceOpen;
    private BigDecimal priceClose;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal bodyAbs;
    private BigDecimal lowVsHighAbs;
    private BigDecimal upperWick;
    private BigDecimal lowerWick;
    private long minuteStartTime;
    private int ticksCount;

    public CandleColor getColor() {
        return color;
    }

    public void setColor(CandleColor color) {
        this.color = color;
    }

    public BigDecimal getPriceOpen() {
        return priceOpen;
    }

    public void setPriceOpen(BigDecimal priceOpen) {
        this.priceOpen = priceOpen;
    }

    public BigDecimal getPriceClose() {
        return priceClose;
    }

    public void setPriceClose(BigDecimal priceClose) {
        this.priceClose = priceClose;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getBodyAbs() {
        return bodyAbs;
    }

    public void setBodyAbs(BigDecimal bodyAbs) {
        this.bodyAbs = bodyAbs;
    }

    public BigDecimal getLowVsHighAbs() {
        return lowVsHighAbs;
    }

    public void setLowVsHighAbs(BigDecimal lowVsHighAbs) {
        this.lowVsHighAbs = lowVsHighAbs;
    }

    public BigDecimal getUpperWick() {
        return upperWick;
    }

    public void setUpperWick(BigDecimal upperWick) {
        this.upperWick = upperWick;
    }

    public BigDecimal getLowerWick() {
        return lowerWick;
    }

    public void setLowerWick(BigDecimal lowerWick) {
        this.lowerWick = lowerWick;
    }

    public long getMinuteStartTime() {
        return minuteStartTime;
    }

    public void setMinuteStartTime(long minuteStartTime) {
        this.minuteStartTime = minuteStartTime;
    }

    public int getTicksCount() {
        return ticksCount;
    }

    public void setTicksCount(int ticksCount) {
        this.ticksCount = ticksCount;
    }

    @Override
    public String toString() {
        return "CurrentCandleData{" +
                "color=" + color +
                ", priceOpen=" + priceOpen +
                ", priceClose=" + priceClose +
                ", high=" + high +
                ", low=" + low +
                ", bodyAbs=" + bodyAbs +
                ", lowVsHighAbs=" + lowVsHighAbs +
                ", upperWick=" + upperWick +
                ", lowerWick=" + lowerWick +
                ", minuteStartTime=" + minuteStartTime +
                ", ticksCount=" + ticksCount +
                '}';
    }
}