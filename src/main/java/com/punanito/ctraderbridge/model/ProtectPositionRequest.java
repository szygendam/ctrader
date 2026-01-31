package com.punanito.ctraderbridge.model;

public class ProtectPositionRequest {
    private long positionId;
    private long orderId;
    private String clientId;
    private String status;

    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
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

    public void setStatus(String status) {
        this.status = status;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
