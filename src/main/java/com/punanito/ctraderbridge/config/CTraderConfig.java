package com.punanito.ctraderbridge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CTraderConfig {

    @Value("${ctrader.host}")
    private String host;

    @Value("${ctrader.port}")
    private int port;

    @Value("${ctrader.clientPublicId}")
    private String clientPublicId;

    @Value("${ctrader.clientSecret}")
    private String clientSecret;

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getClientPublicId() { return clientPublicId; }
    public String getClientSecret() { return clientSecret; }
}
