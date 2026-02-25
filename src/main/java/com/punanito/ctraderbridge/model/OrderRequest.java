package com.punanito.ctraderbridge.model;


public class OrderRequest {
    private String operation;
    private String message;
    private String orderId;
    private long positionId;
    private long id;
    private double sl;
    private double tp;
    private String status;
    private long ctidTraderAccountId;


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getPositionId() {
        return positionId;
    }
    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }


    public String getOperation() {
        return operation;
    }

    public double getSl() {
        return sl;
    }
    public long getId() {
        return id;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
    public void setId(long id) {
        this.id = id;
    }
    public void setSl(double sl) {
        this.sl = sl;
    }
    public void setTp(double tp) {
        this.tp = tp;
    }
    public double getTp() {
        return tp;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    public String getOrderId() {
        return orderId;
    }

    public long getCtidTraderAccountId() {
        return ctidTraderAccountId;
    }
}
