package com.punanito.predator.controller;

import com.punanito.predator.CTraderWebSocketClient;
import com.punanito.predator.model.ConnectRequest;
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

    @PostMapping(value="connect", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> connect(@RequestBody ConnectRequest connectRequest) {
        logger.info("Received connect request");
       webSocketClient.connect(connectRequest.getClient_id(),connectRequest.getClient_secret(), connectRequest.getAccess_token());
        return ResponseEntity.ok().build();
    }

    @PostMapping(value="logout", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> logout() {
        logger.info("logout request");
        webSocketClient.logout();
        System.exit(0);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value="stop", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> stop() {
        logger.info("stop request");
        webSocketClient.stop();
        System.exit(0);
        return ResponseEntity.ok().build();
    }
}
