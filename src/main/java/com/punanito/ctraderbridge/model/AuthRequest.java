package com.punanito.ctraderbridge.model;

public class AuthRequest {
    private String code;
    private String access_token;
    private String refresh_token;
    private String client_id;
    private String client_secret;

    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public String getAccess_token() {
        return access_token;
    }
    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }
    public String getRefresh_token() {
        return refresh_token;
    }
    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public String getClient_secret() {
        return client_secret;
    }

    public String getClient_id() {
        return client_id;
    }
}
