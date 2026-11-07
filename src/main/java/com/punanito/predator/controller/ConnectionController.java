package com.punanito.predator.controller;

import com.punanito.predator.CTraderWebSocketClient;
import com.punanito.predator.model.ConnectRequest;
import com.punanito.predator.service.N8nService;
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
    private final N8nService n8nService;

    public ConnectionController(CTraderWebSocketClient webSocketClient, N8nService n8nService) {
        this.webSocketClient = webSocketClient;
        this.n8nService = n8nService;
    }

    @PostMapping(value="connect", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> connect(@RequestBody ConnectRequest connectRequest) {
        logger.info("Received connect request");
       webSocketClient.connect(connectRequest.getClient_id(),connectRequest.getClient_secret(), connectRequest.getAccess_token());
        return ResponseEntity.ok().build();
    }

    @PostMapping(value="connect/init", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> connectInit(@RequestBody ConnectRequest connectRequest) {
        logger.info("Received init connect request");
        n8nService.connectToN8n();
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
