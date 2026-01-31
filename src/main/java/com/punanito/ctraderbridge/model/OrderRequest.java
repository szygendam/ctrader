package com.punanito.ctraderbridge.model;


public class OrderRequest {
    private String operation;
    private String messageId;
    private String orderId;
    private long id;
    private double sl;
    private double tp;
    private String status;


    public String getMessageId() {
        return messageId;
    }

    public String getOperation() {
        return operation;
    }

    public double getSl() {
        return sl;
    }

    public double getTp() {
        return tp;
    }

    public String getOrderId() {
        return orderId;
    }

}
