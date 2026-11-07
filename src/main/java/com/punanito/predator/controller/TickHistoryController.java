package com.punanito.predator.controller;

import com.punanito.predator.CTraderWebSocketClient;
import com.punanito.predator.model.TickHistoryRequest;
import com.punanito.predator.model.TickHistoryResponse;
import com.xtrader.protocol.openapi.v2.model.ProtoOAQuoteType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/tick/history")
public class TickHistoryController {
    private final CTraderWebSocketClient webSocketClient;

    public TickHistoryController(CTraderWebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
    }

    @PostMapping("/get")
    public CompletableFuture<TickHistoryResponse> get(@RequestBody TickHistoryRequest request) {
        String symbol = request.getSymbol();
        if (symbol == null || symbol.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol is required");
        }
        long to = request.getTimeTo() == 0 ? System.currentTimeMillis() : request.getTimeTo();
        long from = request.getTimeFrom() == 0 ? to - 1000 : request.getTimeFrom();
        if (from < 0 || from >= to || to > 2147483646000L || to - from > 604800000L) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use Unix milliseconds, timeFrom < timeTo and a range no longer than one week");
        }
        String name = symbol.trim();
        return webSocketClient.getTickHistory(name, from, to, ProtoOAQuoteType.BID)
                .thenCombine(webSocketClient.getTickHistory(name, from, to, ProtoOAQuoteType.ASK),
                        (bid, ask) -> new TickHistoryResponse(name, from, to, bid, ask))
                .handle((response, error) -> {
                    if (error == null) return response;
                    Throwable cause = error;
                    while (cause instanceof CompletionException && cause.getCause() != null) {
                        cause = cause.getCause();
                    }
                    throw new ResponseStatusException(cause instanceof TimeoutException
                            ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY,
                            "Could not retrieve cTrader tick history", cause);
                });
    }
}
