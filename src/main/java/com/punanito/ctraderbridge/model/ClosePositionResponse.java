package com.punanito.ctraderbridge.model;

public class ClosePositionResponse {
    private String positionId;
    private String status;

    public ClosePositionResponse() {}
    public ClosePositionResponse(String positionId, String status) {
        this.positionId = positionId;
        this.status = status;
    }

    public String getPositionId() {
        return positionId;
    }
    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
