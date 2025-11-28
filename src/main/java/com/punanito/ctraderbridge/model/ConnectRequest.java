package com.punanito.ctraderbridge.model;

public class ConnectRequest {

    private String access_token;
    private String client_id;
    private String client_secret;

    public String getClient_id() {
        return client_id;
    }

    public String getClient_secret() {
        return client_secret;
    }

    public String getAccess_token() {
        return access_token;
    }
}
