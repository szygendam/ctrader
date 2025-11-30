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
    public ResponseEntity<Void> connect(@RequestBody OrderRequest orderRequest) {
        logger.info("Received new order request");
        webSocketClient.sendGoldOrder(orderRequest.isBuy(),orderRequest.getMessageId(), orderRequest.getRiskLvl());
        return ResponseEntity.ok().build();
    }
}
