package com.punanito.ctraderbridge.controller;

import com.punanito.ctraderbridge.CTraderWebSocketClient;
import com.punanito.ctraderbridge.model.ConnectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/")
public class ConnectionController {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionController.class);

    private final CTraderWebSocketClient webSocketClient;

    public ConnectionController(CTraderWebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
    }

    @PostMapping("connect")
    public ResponseEntity<Void> connect(@RequestBody ConnectRequest connectRequest) {
        logger.info("Received connect request");
       webSocketClient.connect(connectRequest.getClient_id(),connectRequest.getClient_secret(), connectRequest.getAccess_token());
        return ResponseEntity.ok().build();
    }
}
