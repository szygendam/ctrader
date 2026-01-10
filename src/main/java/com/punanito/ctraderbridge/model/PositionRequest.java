package com.punanito.ctraderbridge.model;

public class PositionRequest {
    private long positionId;
    private String status;

    public long getPositionId() {
        return positionId;
    }
    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
