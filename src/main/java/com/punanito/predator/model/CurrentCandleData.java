package com.punanito.predator.model;

import java.math.BigDecimal;

public class CurrentCandleData {

    private long minuteStartTime;
    private int ticksCount;

    private BigDecimal priceOpen;
    private BigDecimal priceClose;
    private BigDecimal high;
    private BigDecimal low;

    private BigDecimal bodyAbs;
    private BigDecimal bodySigned;
    private BigDecimal lowVsHighAbs;
    private BigDecimal upperWick;
    private BigDecimal lowerWick;

    private CandleColor color;

    private BigDecimal positionInRange;
    private Integer secondOfMinute;

    private BigDecimal currentBid;
    private BigDecimal currentAsk;
    private BigDecimal currentSpread;

    private BigDecimal spreadMin;
    private BigDecimal spreadMax;
    private BigDecimal spreadAvg;

    private boolean newHighBreak;
    private boolean newLowBreak;

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

    public BigDecimal getBodySigned() {
        return bodySigned;
    }

    public void setBodySigned(BigDecimal bodySigned) {
        this.bodySigned = bodySigned;
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

    public CandleColor getColor() {
        return color;
    }

    public void setColor(CandleColor color) {
        this.color = color;
    }

    public BigDecimal getPositionInRange() {
        return positionInRange;
    }

    public void setPositionInRange(BigDecimal positionInRange) {
        this.positionInRange = positionInRange;
    }

    public Integer getSecondOfMinute() {
        return secondOfMinute;
    }

    public void setSecondOfMinute(Integer secondOfMinute) {
        this.secondOfMinute = secondOfMinute;
    }

    public BigDecimal getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(BigDecimal currentBid) {
        this.currentBid = currentBid;
    }

    public BigDecimal getCurrentAsk() {
        return currentAsk;
    }

    public void setCurrentAsk(BigDecimal currentAsk) {
        this.currentAsk = currentAsk;
    }

    public BigDecimal getCurrentSpread() {
        return currentSpread;
    }

    public void setCurrentSpread(BigDecimal currentSpread) {
        this.currentSpread = currentSpread;
    }

    public BigDecimal getSpreadMin() {
        return spreadMin;
    }

    public void setSpreadMin(BigDecimal spreadMin) {
        this.spreadMin = spreadMin;
    }

    public BigDecimal getSpreadMax() {
        return spreadMax;
    }

    public void setSpreadMax(BigDecimal spreadMax) {
        this.spreadMax = spreadMax;
    }

    public BigDecimal getSpreadAvg() {
        return spreadAvg;
    }

    public void setSpreadAvg(BigDecimal spreadAvg) {
        this.spreadAvg = spreadAvg;
    }

    public boolean isNewHighBreak() {
        return newHighBreak;
    }

    public void setNewHighBreak(boolean newHighBreak) {
        this.newHighBreak = newHighBreak;
    }

    public boolean isNewLowBreak() {
        return newLowBreak;
    }

    public void setNewLowBreak(boolean newLowBreak) {
        this.newLowBreak = newLowBreak;
    }

    @Override
    public String toString() {
        return "CurrentCandleData{" +
                "minuteStartTime=" + minuteStartTime +
                ", ticksCount=" + ticksCount +
                ", priceOpen=" + priceOpen +
                ", priceClose=" + priceClose +
                ", high=" + high +
                ", low=" + low +
                ", bodyAbs=" + bodyAbs +
                ", bodySigned=" + bodySigned +
                ", lowVsHighAbs=" + lowVsHighAbs +
                ", upperWick=" + upperWick +
                ", lowerWick=" + lowerWick +
                ", color=" + color +
                ", positionInRange=" + positionInRange +
                ", secondOfMinute=" + secondOfMinute +
                ", currentBid=" + currentBid +
                ", currentAsk=" + currentAsk +
                ", currentSpread=" + currentSpread +
                ", spreadMin=" + spreadMin +
                ", spreadMax=" + spreadMax +
                ", spreadAvg=" + spreadAvg +
                ", newHighBreak=" + newHighBreak +
                ", newLowBreak=" + newLowBreak +
                '}';
    }
}