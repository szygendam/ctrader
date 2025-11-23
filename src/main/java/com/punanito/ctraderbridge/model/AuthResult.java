package com.punanito.ctraderbridge.model;

public class AuthResult {
    private String accessToken;
    private String refreshToken;
    private long traderAccountId;

    public AuthResult(String accessToken, String refreshToken, long traderAccountId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.traderAccountId = traderAccountId;
    }

    public String getAccessToken() {
        return accessToken;
    }
    public String getRefreshToken() {
        return refreshToken;
    }
    public long getTraderAccountId() {
        return traderAccountId;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    public void setTraderAccountId(long traderAccountId) {
        this.traderAccountId = traderAccountId;
    }
}
