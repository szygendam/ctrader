package com.punanito.ctraderbridge.service;

import com.punanito.ctraderbridge.model.OrderRequest;
import com.punanito.ctraderbridge.model.OrderResponse;
import com.punanito.ctraderbridge.model.ClosePositionRequest;
import com.punanito.ctraderbridge.model.ClosePositionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;

@Service
public class CTraderTradeService {

    private static final Logger logger = LoggerFactory.getLogger(CTraderTradeService.class);

    @Value("${n8n.webhook.accessDeniedUrl}")
    private String n8nAccessDeniedWebhookUrl;

    private final WebClient webClient;

    public CTraderTradeService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public OrderResponse placeOrder(long accountId, OrderRequest req, String accessToken) throws Exception {
        logger.info("Placing order: accountId={}, symbolId={}, side={}, type={}, volume={}",
            accountId, req.getSymbolId(), req.getTradeSide(), req.getOrderType(), req.getVolume());

        // Tutaj: buduj request Protobuf i wyślij do API cTrader
        // Przykład pseudokodu:
        // ApiResponse response = sendProtoOrderRequest(...);

        // Mockowa odpowiedź dla przykładu
        ApiResponseMock response = mockSendOrder(accountId, req, accessToken);

        if ("ACCESS_DENIED".equals(response.getErrorCode())) {
            logger.warn("Received ACCESS_DENIED from cTrader API");
            notifyN8nAccessDenied();
            throw new AccessDeniedException("ACCESS_DENIED from cTrader API");
        }

        // Zakładamy, że odpowiedź zawiera orderId i status
        OrderResponse orderResponse = new OrderResponse(response.getOrderId(), response.getStatus());
        logger.info("Order placed: orderId={}, status={}", orderResponse.getOrderId(), orderResponse.getStatus());
        return orderResponse;
    }

    public ClosePositionResponse closePosition(long accountId, ClosePositionRequest req, String accessToken) throws Exception {
        logger.info("Closing position: accountId={}, positionId={}", accountId, req.getPositionId());

        // Pseudokod:
        ApiResponseMock response = mockClosePosition(accountId, req, accessToken);

        if ("ACCESS_DENIED".equals(response.getErrorCode())) {
            logger.warn("Received ACCESS_DENIED from cTrader API on closePosition");
            notifyN8nAccessDenied();
            throw new AccessDeniedException("ACCESS_DENIED from cTrader API");
        }

        ClosePositionResponse resp = new ClosePositionResponse(String.valueOf(req.getPositionId()), response.getStatus());
        logger.info("Position closed: positionId={}, status={}", resp.getPositionId(), resp.getStatus());
        return resp;
    }

    private void notifyN8nAccessDenied() {
        logger.info("Notifying n8n about ACCESS_DENIED");
        webClient.post()
            .uri(n8nAccessDeniedWebhookUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "event", "ACCESS_DENIED",
                "timestamp", Instant.now().toString()
            ))
            .retrieve()
            .bodyToMono(Void.class)
            .subscribe(
                unused -> logger.info("Notified n8n successfully"),
                err -> logger.error("Error notifying n8n", err)
            );
    }

    // Pseudokodowe metody mockujące – w rzeczywistej implementacji zastąp faktycznymi requestami Protobuf
    private ApiResponseMock mockSendOrder(long accountId, OrderRequest req, String accessToken) {
        // zwróć jakieś dane; czasem zwróć errorCode "ACCESS_DENIED"
        return new ApiResponseMock("okOrder123", "PLACED", null);
    }

    private ApiResponseMock mockClosePosition(long accountId, ClosePositionRequest req, String accessToken) {
        return new ApiResponseMock("pos123", "CLOSED", null);
    }

    // Klasa pomocnicza
    private static class ApiResponseMock {
        private final String orderId;
        private final String status;
        private final String errorCode;

        public ApiResponseMock(String orderId, String status, String errorCode) {
            this.orderId = orderId;
            this.status = status;
            this.errorCode = errorCode;
        }

        public String getOrderId() {
            return orderId;
        }
        public String getStatus() {
            return status;
        }
        public String getErrorCode() {
            return errorCode;
        }
    }
}
