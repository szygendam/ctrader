package com.punanito.ctraderbridge.model;

public class PriceRequest {
    private double lastBid;
    private double lastAsk;
    private double spread;

    public PriceRequest(double lastBid, double lastAsk) {
        this.lastBid = lastBid;
        this.lastAsk = lastAsk;
        this.spread = lastBid - lastAsk;
    }

    public double getLastBid() {
        return lastBid;
    }

    public double getLastAsk() {
        return lastAsk;
    }
    public double getSpread() {
        return spread;
    }
    public void setLastBid(double lastBid) {
        this.lastBid = lastBid;
    }
    public void setLastAsk(double lastAsk) {
        this.lastAsk = lastAsk;
    }
    public void setSpread(double spread) {
        this.spread = spread;
    }

}
