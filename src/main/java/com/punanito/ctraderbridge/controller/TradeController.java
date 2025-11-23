package com.punanito.ctraderbridge.controller;

import com.punanito.ctraderbridge.model.OrderRequest;
import com.punanito.ctraderbridge.model.OrderResponse;
import com.punanito.ctraderbridge.model.ClosePositionRequest;
import com.punanito.ctraderbridge.model.ClosePositionResponse;
import com.punanito.ctraderbridge.service.CTraderTradeService;
import com.punanito.ctraderbridge.service.CTraderAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trade")
public class TradeController {

    private static final Logger logger = LoggerFactory.getLogger(TradeController.class);

    private final CTraderTradeService tradeService;
    private final CTraderAuthService authService;

    public TradeController(CTraderTradeService tradeService, CTraderAuthService authService) {
        this.tradeService = tradeService;
        this.authService = authService;
    }

    @PostMapping("/order")
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        try {
            String accessToken = authService.getAccessToken();
            long accountId = authService.getTraderAccountId();
            logger.info("Received placeOrder request: accountId={}, symbolId={}, side={}, type={}, volume={}",
                    accountId, request.getSymbolId(), request.getTradeSide(), request.getOrderType(), request.getVolume());
            OrderResponse resp = tradeService.placeOrder(accountId, request, accessToken);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Error in placeOrder", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/position/close")
    public ResponseEntity<ClosePositionResponse> closePosition(@RequestBody ClosePositionRequest req) {
        try {
            String accessToken = authService.getAccessToken();
            long accountId = authService.getTraderAccountId();
            logger.info("Received closePosition request: accountId={}, positionId={}", accountId, req.getPositionId());
            ClosePositionResponse resp = tradeService.closePosition(accountId, req, accessToken);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Error in closePosition", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
