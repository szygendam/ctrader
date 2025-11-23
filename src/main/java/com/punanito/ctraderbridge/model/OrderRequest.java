package com.punanito.ctraderbridge.model;

public class OrderRequest {
    private long symbolId;
    private double volume;
    private String tradeSide;   // "BUY" or "SELL"
    private String orderType;   // "MARKET" or "LIMIT"
    private Double stopLoss;
    private Double takeProfit;

    // getters and setters
    public long getSymbolId() {
        return symbolId;
    }
    public void setSymbolId(long symbolId) {
        this.symbolId = symbolId;
    }
    public double getVolume() {
        return volume;
    }
    public void setVolume(double volume) {
        this.volume = volume;
    }
    public String getTradeSide() {
        return tradeSide;
    }
    public void setTradeSide(String tradeSide) {
        this.tradeSide = tradeSide;
    }
    public String getOrderType() {
        return orderType;
    }
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }
    public Double getStopLoss() {
        return stopLoss;
    }
    public void setStopLoss(Double stopLoss) {
        this.stopLoss = stopLoss;
    }
    public Double getTakeProfit() {
        return takeProfit;
    }
    public void setTakeProfit(Double takeProfit) {
        this.takeProfit = takeProfit;
    }
}
