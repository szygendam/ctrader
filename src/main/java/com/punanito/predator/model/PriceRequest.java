package com.punanito.predator.model;


import java.math.BigDecimal;

public class PriceRequest extends SymbolRequest {
    private BigDecimal lastBid;
    private BigDecimal lastAsk;
    private BigDecimal spread;
    private long currentTime;


    public PriceRequest(BigDecimal lastBid, BigDecimal lastAsk, long symbolId, String symbolName) {
         this(lastBid, lastAsk, symbolId, symbolName, System.currentTimeMillis());
    }

    public PriceRequest(BigDecimal lastBid, BigDecimal lastAsk, long symbolId, String symbolName,long currentTime) {
        super(symbolId,symbolName);
        this.lastBid = lastBid;
        this.lastAsk = lastAsk;
        if(lastBid.compareTo(new BigDecimal(0)) == 0 ||
                lastAsk.compareTo(new BigDecimal(0)) == 0){
            this.spread = new BigDecimal(0);
        } else {
            this.spread =  (lastAsk.subtract(lastBid)).divide(new BigDecimal(100000)) ;
        }
        this.currentTime = currentTime;
    }

    public BigDecimal getLastBid() {
        return lastBid;
    }

    public BigDecimal getLastAsk() {
        return lastAsk;
    }
    public BigDecimal getSpread() {
        return spread;
    }
    public void setLastBid(BigDecimal lastBid) {
        this.lastBid = lastBid;
    }
    public void setLastAsk(BigDecimal lastAsk) {
        this.lastAsk = lastAsk;
    }
    public void setSpread(BigDecimal spread) {
        this.spread = spread;
    }
    public void setCurrentTime(long currentTime) {this.currentTime = currentTime;}
    public long getCurrentTime() {return currentTime;}

    @Override
    public String toString() {
        return "PriceRequest{" +
                "symbolId=" + getId() +
                ", symbolName='" + getName() + '\'' +
                ", lastBid=" + lastBid +
                ", lastAsk=" + lastAsk +
                ", spread=" + spread +
                ", currentTime=" + currentTime +
                '}';
    }
}
