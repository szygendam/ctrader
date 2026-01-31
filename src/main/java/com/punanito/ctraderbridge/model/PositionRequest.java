package com.punanito.ctraderbridge.model;

public class PositionRequest {
    private long positionId;
    private long orderId;
    private double sl;
    private double tp;
    private double priceOpen;
    private String clientId;
    private String positionStatus;
    private String orderStatus;
    private String executionType;

    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public String getPosistionStatus() {
        return positionStatus;
    }

    public String getExecutionType() {
        return executionType;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public String getClientId() {
        return clientId;
    }

    public long getOrderId() {
        return orderId;
    }

    public long getPositionId() {
        return positionId;
    }

    public void setPosistionStatus(String status) {
        this.positionStatus = status;
    }

    public void setOrderStatus(String status) {
        this.orderStatus = status;
    }

    public void setExecutionType(String type) {
        this.executionType = type;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public double getSl() {
        return sl;
    }
    public double getTp() {
        return tp;
    }
    public double getPriceOpen() {
        return priceOpen;
    }
    public void setSl(double sl) {
        this.sl = sl;
    }
    public void setTp(double tp) {
        this.tp = tp;
    }
    public void setPriceOpen(double priceOpen) {
        this.priceOpen = priceOpen;
    }
    public void setPositionStatus(String status) {
        this.positionStatus = status;
    }
}
