package com.punanito.ctraderbridge.controller;

import com.punanito.ctraderbridge.CTraderWebSocketClient;
import com.punanito.ctraderbridge.model.AuthRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final CTraderWebSocketClient webSocketClient;

    public AuthController( CTraderWebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
    }

    @PostMapping("/update‑tokens")
    public ResponseEntity<Void> updateTokens(@RequestBody AuthRequest request) {
        logger.info("Received token update request: code={}, access_token={}, refresh_token={}",
                request.getCode(), request.getAccess_token(), request.getRefresh_token());

        webSocketClient.updateAccessToken(request.getAccess_token());
        return ResponseEntity.ok().build();
    }
}
