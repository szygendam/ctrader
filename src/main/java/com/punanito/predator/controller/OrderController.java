package com.punanito.predator.controller;

import com.punanito.predator.CTraderWebSocketClient;
import com.punanito.predator.model.OrderRequest;
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
        webSocketClient.sendOrder(orderRequest.getOperation(),orderRequest.getMessage(),orderRequest.getTp(), orderRequest.getSl(), orderRequest.getSymbol());
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

    @PostMapping("/closeNotProtected")
    public ResponseEntity<Void> closeNotProtected() {
        webSocketClient.closeNotProtected();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reconcile")
    public ResponseEntity<Void> reconcile(@RequestBody OrderRequest orderRequest) {
        webSocketClient.reconcile(orderRequest.getPositionId());
        return ResponseEntity.ok().build();
    }
}
