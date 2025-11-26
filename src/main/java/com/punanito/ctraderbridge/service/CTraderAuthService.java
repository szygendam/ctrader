package com.punanito.ctraderbridge.service;

import com.punanito.ctraderbridge.config.CTraderConfig;
import com.punanito.ctraderbridge.model.AuthResult;
import com.xtrader.protocol.openapi.v2.ProtoOAApplicationAuthReq;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class CTraderAuthService {
    private static final Logger logger = LoggerFactory.getLogger(CTraderAuthService.class);

    private final CTraderConfig config;
    @Getter
    private String accessToken;
    @Getter
    private String refreshToken;
    @Getter
    private String authorizationCode;
    @Getter
    private long traderAccountId;

    public CTraderAuthService(CTraderConfig config) {
        this.config = config;
    }

    public void updateTokens(String code, String incomingAccessToken, String incomingRefreshToken) {
        logger.info("Updating tokens: code={}, incomingAccessToken={}, incomingRefreshToken={}",
                code, incomingAccessToken, incomingRefreshToken);
        if (incomingAccessToken != null && !incomingAccessToken.isEmpty()) {
            this.accessToken = incomingAccessToken;
        }
        if (incomingRefreshToken != null && !incomingRefreshToken.isEmpty()) {
            this.refreshToken = incomingRefreshToken;
        }
        if (code != null && !code.isEmpty()) {
            this.authorizationCode = code;
        }
        // Opcjonalnie możesz wywołać od razu authenticateWithTokens lub zostawić do oddzielnego endpointu
    }

    public AuthResult authenticateWithTokens(String code, String incomingAccessToken, String incomingRefreshToken) throws Exception {
        updateTokens(code, incomingAccessToken, incomingRefreshToken);
        // reszta logiki autoryzacji …
        logger.info("Starting authentication flow with tokens or code");
//        performApplicationAuth();
//        performGetAccountList();
//        performAccountAuth();
        return new AuthResult(this.accessToken, this.refreshToken, this.traderAccountId);
    }

    public void authApplication(String clientId, String clientSecret) {
        ProtoOAApplicationAuthReq authReq = ProtoOAApplicationAuthReq.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret).build();



    }

    // ... pozostałe metody (performApplicationAuth, performGetAccountList, etc.) ...
}
