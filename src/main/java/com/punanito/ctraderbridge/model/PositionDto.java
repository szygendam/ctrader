package com.punanito.ctraderbridge.model;


public class PositionDto {

    private boolean isBuy;
    private double priceOpen;
    private double tp;
    private double sl;
    private long positionId;
    private boolean reprotectAlreadySend;

    public PositionDto(double sl, double tp) {
        this.tp = tp;
        this.sl = sl;
    }

    public boolean isReprotectAlreadySend() {
        return reprotectAlreadySend;
    }
    public void setReprotectAlreadySend(boolean reprotectAlreadySend) {
        this.reprotectAlreadySend = reprotectAlreadySend;
    }
    public boolean isBuy() {
        return isBuy;
    }
    public double getPriceOpen() {
        return priceOpen;
    }
    public double getTp() {
        return tp;
    }
    public double getSl() {
        return sl;
    }
    public long getPositionId() {
        return positionId;
    }
    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }
    public void setBuy(boolean isBuy) {
        this.isBuy = isBuy;
    }
    public void setPriceOpen(double priceOpen) {
        this.priceOpen = priceOpen;
    }
    public void setTp(double tp) {
        this.tp = tp;
    }
    public void setSl(double sl) {
        this.sl = sl;
    }

}
