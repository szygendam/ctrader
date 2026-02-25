package com.punanito.ctraderbridge.controller;

import com.punanito.ctraderbridge.CTraderWebSocketClient;
import com.punanito.ctraderbridge.model.ConnectRequest;
import com.punanito.ctraderbridge.model.OrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/order")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final CTraderWebSocketClient webSocketClient;

    public OrderController(CTraderWebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
    }

    @PostMapping("/new")
    public ResponseEntity<Void> newOrder(@RequestBody OrderRequest orderRequest) {
        logger.info("Received new order request: isBuy {} message {} ", orderRequest.getOperation(), orderRequest.getMessage());
        webSocketClient.sendGoldOrder(orderRequest.getOperation(),orderRequest.getMessage());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/protect")
    public ResponseEntity<Void> protect(@RequestBody OrderRequest orderRequest) {
        webSocketClient.protect(orderRequest.getSl(),orderRequest.getTp(),orderRequest.getPositionId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/close")
    public ResponseEntity<Void> close(@RequestBody OrderRequest orderRequest) {
        webSocketClient.close(orderRequest.getPositionId());
        return ResponseEntity.ok().build();
    }
}
