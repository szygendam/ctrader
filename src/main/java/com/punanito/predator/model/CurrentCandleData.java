package com.punanito.predator.model;

public class CurrentCandleData {
    private CandleColor color;
    private double priceOpen;
    private double priceClose;
    private double high;
    private double low;
    private double bodyAbs;
    private double lowVsHighAbs;
    private double upperWick;
    private double lowerWick;
    private long minuteStartTime;
    private int ticksCount;

    public CandleColor getColor() {
        return color;
    }

    public void setColor(CandleColor color) {
        this.color = color;
    }

    public double getPriceOpen() {
        return priceOpen;
    }

    public void setPriceOpen(double priceOpen) {
        this.priceOpen = priceOpen;
    }

    public double getPriceClose() {
        return priceClose;
    }

    public void setPriceClose(double priceClose) {
        this.priceClose = priceClose;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getBodyAbs() {
        return bodyAbs;
    }

    public void setBodyAbs(double bodyAbs) {
        this.bodyAbs = bodyAbs;
    }

    public double getLowVsHighAbs() {
        return lowVsHighAbs;
    }

    public void setLowVsHighAbs(double lowVsHighAbs) {
        this.lowVsHighAbs = lowVsHighAbs;
    }

    public double getUpperWick() {
        return upperWick;
    }

    public void setUpperWick(double upperWick) {
        this.upperWick = upperWick;
    }

    public double getLowerWick() {
        return lowerWick;
    }

    public void setLowerWick(double lowerWick) {
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