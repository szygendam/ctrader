package com.punanito.ctraderbridge.model;

public class OrderRequest {
    private Boolean buy;
    private String messageId;
    private String riskLvl;

    public boolean isBuy() {
        return Boolean.TRUE.equals(buy);
    }

    public String getMessageId() {
        return messageId;
    }

    public String getRiskLvl() {
        return riskLvl;
    }

}
