package com.punanito.ctraderbridge.model;

import java.util.Date;

public class PriceRequest extends SymbolRequest {
    private double lastBid;
    private double lastAsk;
    private double spread;
    private long currentTime;


    public PriceRequest(double lastBid, double lastAsk, long symbolId, String symbolName) {
       super(symbolId,symbolName);
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
    public void setCurrentTime(long currentTime) {this.currentTime = currentTime;}
    public long getCurrentTime() {return currentTime;}

}
