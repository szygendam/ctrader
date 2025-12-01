package com.punanito.ctraderbridge.model;

import java.util.Date;

public class PriceRequest {
    private double lastBid;
    private double lastAsk;
    private double spread;
    private long currentTime;
    public PriceRequest() {
    }

    public PriceRequest(double lastBid, double lastAsk) {
        this.lastBid = lastBid;
        this.lastAsk = lastAsk;
        if(lastBid == 0 || lastAsk == 0){
            this.spread = 0.0;
        } else {
            this.spread =  lastAsk - lastBid;
        }
        currentTime = System.currentTimeMillis();
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
