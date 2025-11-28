package com.punanito.ctraderbridge;

import com.google.protobuf.InvalidProtocolBufferException;
import com.punanito.ctraderbridge.model.PriceRequest;
import com.xtrader.protocol.openapi.v2.ProtoOAAccountAuthReq;
import com.xtrader.protocol.openapi.v2.ProtoOAAmendPositionSLTPReq;
import com.xtrader.protocol.openapi.v2.ProtoOAApplicationAuthReq;
import com.xtrader.protocol.openapi.v2.ProtoOAExecutionEvent;
import com.xtrader.protocol.openapi.v2.ProtoOAGetAccountListByAccessTokenReq;
import com.xtrader.protocol.openapi.v2.ProtoOAGetAccountListByAccessTokenRes;
import com.xtrader.protocol.openapi.v2.ProtoOANewOrderReq;
import com.xtrader.protocol.openapi.v2.ProtoOASpotEvent;
import com.xtrader.protocol.openapi.v2.ProtoOASubscribeSpotsReq;
import com.xtrader.protocol.openapi.v2.ProtoOASymbolByIdReq;
import com.xtrader.protocol.openapi.v2.ProtoOASymbolByIdRes;
import com.xtrader.protocol.openapi.v2.ProtoOASymbolsListReq;
import com.xtrader.protocol.openapi.v2.ProtoOASymbolsListRes;
import com.xtrader.protocol.openapi.v2.ProtoOATraderUpdatedEvent;
import com.xtrader.protocol.openapi.v2.ProtoOAUnsubscribeSpotsReq;
import com.xtrader.protocol.openapi.v2.model.ProtoOALightSymbol;
import com.xtrader.protocol.openapi.v2.model.ProtoOAOrderType;
import com.xtrader.protocol.openapi.v2.model.ProtoOAPayloadType;
import com.xtrader.protocol.openapi.v2.model.ProtoOASymbol;
import com.xtrader.protocol.openapi.v2.model.ProtoOATradeSide;
import com.xtrader.protocol.proto.commons.ProtoHeartbeatEvent;
import com.xtrader.protocol.proto.commons.ProtoMessage;
import com.xtrader.protocol.proto.commons.model.ProtoPayloadType;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class CTraderWebSocketClient {

    private static String ACCESS_TOKEN = Strings.EMPTY;
    private static String CLIENT_ID = Strings.EMPTY;
    private static String CLIENT_SECRET = Strings.EMPTY;

    WebSocket webSocket;
    long accountId;
    private Map<Long, ProtoOASymbol> symbolDetails = new HashMap<>();
    private Map<String, Long> symbolByName = new HashMap<>();
    private Map<Long, String> symbolById = new HashMap<>();
    private Map<Long, Integer> symbolDigits= new HashMap<>();
    private Map<Long, Long> symbolLotSize= new HashMap<>();
    private double accountBalance = 0.0;
    private double lastBid = 0;
    private double lastAsk = 0;
    private ScheduledExecutorService heartbeatScheduler;
    private RestTemplate restTemplate = new RestTemplate();


    @Value("${n8n.webhook.url}")
    private String n8nWebhookUrl;

    public void updateAccessToken(String accessToken) {
        ACCESS_TOKEN = accessToken;
    }

    public void connect(String clientId, String clientSecret, String accessToken) {
        System.out.println("clientId " + clientId);
        System.out.println("CLIENT_SECRET " + clientSecret);
        System.out.println("ACCESS_TOKEN " + accessToken);
        CLIENT_ID = clientId;
        CLIENT_SECRET = clientSecret;
        ACCESS_TOKEN = accessToken;

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create("wss://demo.ctraderapi.com:5035"), new WebSocket.Listener() {

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        System.out.println("Połączono z cTrader");

                        CTraderWebSocketClient.this.webSocket = webSocket;

                        sendApplicationAuth();
                        startHeartbeat();
                        WebSocket.Listener.super.onOpen(webSocket);
                    }

                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {

                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes);

                        try {
                            ProtoMessage message = ProtoMessage.parseFrom(bytes);
                            handleMessage(message);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return WebSocket.Listener.super.onBinary(webSocket, data, last);
                    }
                }).join();
    }

    // Autoryzacja aplikacji
    private void sendApplicationAuth() {
        System.out.println("sendApplicationAuth");
        ProtoOAApplicationAuthReq req = ProtoOAApplicationAuthReq.newBuilder()
                .setClientId(CLIENT_ID)
                .setClientSecret(CLIENT_SECRET)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_APPLICATION_AUTH_REQ_VALUE);
    }

    // Pobranie symbolList
    private void sendSymbolList() {
        System.out.println("sendSymbolList");
        ProtoOASymbolsListReq req = ProtoOASymbolsListReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_SYMBOLS_LIST_REQ_VALUE);
    }

    // Pobranie szczegółów
    private void sendSymbolById(long id) {
        System.out.println("sendSymbolById");
        ProtoOASymbolByIdReq req = ProtoOASymbolByIdReq.newBuilder()
                .addSymbolId(id)
                .setCtidTraderAccountId(accountId)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_SYMBOL_BY_ID_REQ_VALUE);
    }

    //  Pobierz listę kont
    private void sendGetAccountList() {
        System.out.println("sendGetAccountList ");
        ProtoOAGetAccountListByAccessTokenReq req =
                ProtoOAGetAccountListByAccessTokenReq.newBuilder()
                        .setAccessToken(ACCESS_TOKEN)
                        .build();

        send(req, ProtoOAPayloadType.PROTO_OA_GET_ACCOUNTS_BY_ACCESS_TOKEN_REQ_VALUE);
    }

    //  Autoryzuj konkretne konto
    private void sendAccountAuth() {
        System.out.println("sendAccountAuth ");
        ProtoOAAccountAuthReq req = ProtoOAAccountAuthReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .setAccessToken(ACCESS_TOKEN)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_ACCOUNT_AUTH_REQ_VALUE);
    }

    // 🔌 Wysyłanie wiadomości
    private void send(com.google.protobuf.Message payload, int payloadType) {

        ProtoMessage message = ProtoMessage.newBuilder()
                .setPayloadType(payloadType)
                .setClientMsgId(UUID.randomUUID().toString())
                .setPayload(payload.toByteString())
                .build();

        webSocket.sendBinary(ByteBuffer.wrap(message.toByteArray()), true);
    }

    private void subscribeToTicks(long symbolId) {
        System.out.println("subscribeToTicks ");
        ProtoOASubscribeSpotsReq req = ProtoOASubscribeSpotsReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .addSymbolId(symbolId)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_SUBSCRIBE_SPOTS_REQ_VALUE);
    }


    private void unsubscribeFromSpots(long symbolId) {
        System.out.println("unsubscribeFromSpots ");
        ProtoOAUnsubscribeSpotsReq req = ProtoOAUnsubscribeSpotsReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .addSymbolId(symbolId)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_UNSUBSCRIBE_SPOTS_REQ_VALUE);
    }

    public void sendGoldOrder(boolean isBuy) {
        System.out.println("sendGoldOrder");
        if(accountBalance > 0) {

            double stopLossPips = 20;
            long goldId = findSymbolByName("XAUUSD");

            double entry;
            if(isBuy){
                entry = lastAsk;
            } else {
                entry = lastBid;
            }

            double sl = entry - (stopLossPips / Math.pow(10, symbolDigits.get(goldId)));
            double tp = entry + (stopLossPips / Math.pow(10, symbolDigits.get(goldId))); // RR 1:1

            long volume = calculateDynamicVolume(symbolLotSize.get(goldId));

            System.out.println("AUTO BUY " + symbolById.get(goldId));
            System.out.println("Entry=" + entry + " SL=" + sl + " TP=" + tp + " Vol=" + volume);
            sendMarketOrder(goldId, isBuy, volume);
        }
    }

    private void sendMarketOrder(long symbolId, boolean isBuy, long volume) {
        System.out.println("sendMarketOrder ");

        ProtoOANewOrderReq req = ProtoOANewOrderReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .setSymbolId(symbolId)
                .setOrderType(ProtoOAOrderType.MARKET)
                .setTradeSide(isBuy
                        ? ProtoOATradeSide.BUY
                        : ProtoOATradeSide.SELL)
                .setVolume(volume)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_NEW_ORDER_REQ_VALUE);
    }

    private void setStopLossAndTakeProfit(long positionId, double stopLoss, double takeProfit) {
        System.out.println("setStopLossAndTakeProfit ");
        ProtoOAAmendPositionSLTPReq req = ProtoOAAmendPositionSLTPReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .setPositionId(positionId)
                .setStopLoss(stopLoss)
                .setTakeProfit(takeProfit)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_AMEND_POSITION_SLTP_REQ_VALUE);
    }



    // 📥 Odbieranie wiadomości
    private void handleMessage(ProtoMessage message) throws InvalidProtocolBufferException {

        switch (message.getPayloadType()) {

            case ProtoOAPayloadType.PROTO_OA_APPLICATION_AUTH_RES_VALUE:  {
                System.out.println("Received PROTO_OA_APPLICATION_AUTH_RES_VALUE");
                System.out.println("Application authenticated ✅");
                sendGetAccountList();
            }
            break;

            case ProtoOAPayloadType.PROTO_OA_GET_ACCOUNTS_BY_ACCESS_TOKEN_RES_VALUE: {
                System.out.println("Received PROTO_OA_GET_ACCOUNTS_BY_ACCESS_TOKEN_RES_VALUE");
                ProtoOAGetAccountListByAccessTokenRes res =
                        ProtoOAGetAccountListByAccessTokenRes.parseFrom(message.getPayload());

                accountId = res.getCtidTraderAccountList().get(0).getCtidTraderAccountId();
                System.out.println("Wybrano konto: " + accountId);

                sendAccountAuth();
            }
            break;

            case ProtoOAPayloadType.PROTO_OA_ACCOUNT_AUTH_RES_VALUE: {
                System.out.println("Received PROTO_OA_ACCOUNT_AUTH_RES_VALUE");
                System.out.println("Konto autoryzowane ");
                System.out.println("Gotowy do subskrypcji i nasłuchiwania danych...");
                sendSymbolList();
            }
            break;

            case ProtoOAPayloadType.PROTO_OA_SYMBOLS_LIST_RES_VALUE:  {
                System.out.println("Received PROTO_OA_SYMBOLS_LIST_RES_VALUE");

                ProtoOASymbolsListRes res =
                        ProtoOASymbolsListRes.parseFrom(message.getPayload());

                for (ProtoOALightSymbol symbol : res.getSymbolList()) {
                    symbolByName.put(symbol.getSymbolName(), symbol.getSymbolId());
                    symbolById.put(symbol.getSymbolId(),symbol.getSymbolName());
                }
                System.out.println("Załadowano " + symbolByName.size() + " symboli");

                sendSymbolById(findSymbolByName("XAUUSD"));

            }
            break;

            case ProtoOAPayloadType.PROTO_OA_TRADER_UPDATE_EVENT_VALUE: {
                System.out.println("Received PROTO_OA_TRADER_UPDATE_EVENT_VALUE");
                ProtoOATraderUpdatedEvent event =
                        ProtoOATraderUpdatedEvent.parseFrom(message.getPayload());

                accountBalance = event.getTrader().getBalance();
                System.out.println("BALANCE = " + accountBalance);
            }
            break;

            case ProtoOAPayloadType.PROTO_OA_SYMBOL_BY_ID_RES_VALUE: {
                System.out.println("Received PROTO_OA_SYMBOL_BY_ID_RES_VALUE");
                ProtoOASymbolByIdRes event = ProtoOASymbolByIdRes.parseFrom(message.getPayload());
                for (ProtoOASymbol protoOASymbol : event.getSymbolList()) {
                    symbolDetails.putIfAbsent(protoOASymbol.getSymbolId(), protoOASymbol);
                }
                System.out.println("Załadowano " + symbolDetails.size() + " symboli");

                subscribeGold();
            }
            break;

            // TU beda splywac EVENTY
            case ProtoOAPayloadType.PROTO_OA_SPOT_EVENT_VALUE: {
                System.out.println("Received PROTO_OA_SPOT_EVENT_VALUE");

                ProtoOASpotEvent event = ProtoOASpotEvent.parseFrom(message.getPayload());

                System.out.println("TICK | symbolId=" + event.getSymbolId()
                                + " BID PRICE=" + event.getBid()
                                + " ASK=" + event.getAsk());

                lastBid = event.getBid();
                lastAsk = event.getAsk();

                ProtoOASymbol symbol = symbolDetails.get(event.getSymbolId());

                if (symbol != null ) {
                    System.out.println(" symbol.getDigits()" + symbol.getDigits());
                    System.out.println(" symbol.getLotSize()" + symbol.getLotSize());
                    symbolDigits.replace(symbol.getSymbolId(), symbol.getDigits());
                    symbolLotSize.replace(symbol.getSymbolId(), symbol.getLotSize());
                }

                sendDataToN8n(lastBid, lastAsk);
            }

            case ProtoOAPayloadType.PROTO_OA_EXECUTION_EVENT_VALUE: {
                System.out.println("Received PROTO_OA_EXECUTION_EVENT_VALUE");
                ProtoOAExecutionEvent event =
                        ProtoOAExecutionEvent.parseFrom(message.getPayload());

                System.out.println("EXECUTION: " + event.getExecutionType());
                if (event.hasOrder()) {
                    System.out.println("ORDER ID: " + event.getOrder().getOrderId());
                }
                if (event.hasPosition()) {
                    System.out.println("POSITION ID: " + event.getPosition().getPositionId());

                    long positionId = event.getPosition().getPositionId();
                    double entry = event.getPosition().getPrice();
                    long symbolId = event.getPosition().getTradeData().getSymbolId();

                    ProtoOASymbol symbol = symbolDetails.get(symbolId);
                    double stopLossPips = 20;

                    double sl = entry - (stopLossPips / Math.pow(10, symbol.getDigits()));
                    double tp = entry + (stopLossPips / Math.pow(10, symbol.getDigits())); // RR 1:1

                    setStopLossAndTakeProfit(positionId, sl, tp);

                }
            }
            break;

            default: {
                System.out.println("Message type: " + message.getPayloadType() + " payload: " + message.getPayload());
            }
        }
    }

    private void subscribeGold() {
        if (!symbolByName.isEmpty() && symbolByName.get("XAUUSD") != null){
            subscribeToTicks(symbolByName.get("XAUUSD"));
        }
    }


    public void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                ProtoHeartbeatEvent hb = ProtoHeartbeatEvent.newBuilder().build();
                send(hb, ProtoPayloadType.HEARTBEAT_EVENT_VALUE);
                System.out.println(">>> heartbeat sent");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stopHeartbeat() {
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
        }
    }


    private static double round(double price, int digits) {
        double pow = Math.pow(10, digits);
        return Math.round(price * pow) / pow;
    }

    private long calculateDynamicVolume(Long  lotSize) {
        double riskPercent = 0.1;
        double stopLossPips = 20;
        double riskAmount = accountBalance * (riskPercent / 100.0);

        double pipValue = 1.0; //XAU, BTC

        double lots = riskAmount / (stopLossPips * pipValue);
        double units = lots * lotSize;

        return Math.max(1000, Math.round(units));
    }


    public Long findSymbolByName(String name) {
        return symbolByName.get(name);
    }

    private void sendDataToN8n(double lastBid, double lastAsk) {
       System.out.println("Sending data to n8n n8nWebhookUrl");

        String url = n8nWebhookUrl;
        PriceRequest request = new PriceRequest(lastBid, lastAsk);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PriceRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        System.out.println(response.getBody());
    }



}
