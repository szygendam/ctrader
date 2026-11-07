package com.punanito.predator.controller;

import com.punanito.predator.CTraderWebSocketClient;
import com.punanito.predator.model.TickHistoryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/tick/history")
public class TickHistoryController {

    private static final Logger logger = LoggerFactory.getLogger(TickHistoryController.class);

    private final CTraderWebSocketClient webSocketClient;

    public TickHistoryController(CTraderWebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
    }

    @PostMapping("/get")
    public ResponseEntity<Void> get(@RequestBody TickHistoryRequest tickHistoryRequest) {
        logger.info("Received /tick/history/get request:  {} {} {}", tickHistoryRequest.getSymbol(), tickHistoryRequest.getTimeFrom(), tickHistoryRequest.getTimeTo());
        webSocketClient.sendGetTickHisty(tickHistoryRequest.getSymbol(), tickHistoryRequest.getTimeFrom(), tickHistoryRequest.getTimeTo());
        return ResponseEntity.ok().build();
    }

}
