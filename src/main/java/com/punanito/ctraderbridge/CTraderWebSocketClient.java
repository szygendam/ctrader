package com.punanito.ctraderbridge;

import com.google.protobuf.InvalidProtocolBufferException;
import com.punanito.ctraderbridge.model.AccountRequest;
import com.punanito.ctraderbridge.model.ConnectRequest;
import com.punanito.ctraderbridge.model.PriceRequest;
import com.xtrader.protocol.openapi.v2.ProtoOAAccountAuthReq;
import com.xtrader.protocol.openapi.v2.ProtoOAAccountLogoutReq;
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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    long lastTickTime = 0;
    private Map<Long, ProtoOASymbol> symbolDetails = new HashMap<>();
    private Map<String, Long> symbolByName = new HashMap<>();
    private Map<Long, String> symbolById = new HashMap<>();
    private Map<Long, Integer> symbolDigits= new HashMap<>();
    private Map<Long, Long> symbolLotSize= new HashMap<>();
    private double accountBalance = 0.0;
    private double accountBalanceHalf = 0.0;
    private double lastBid = 0;
    private double lastAsk = 0;
    private ScheduledExecutorService heartbeatScheduler;
    private ScheduledExecutorService goldSubscriptionScheduler;
    private ScheduledExecutorService ticksWatcher;
    private RestTemplate restTemplate = new RestTemplate();
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();


    @Value("${n8n.webhook.ticks.url}")
    private String n8nWebhookTicksUrl;

    @Value("${n8n.webhook.connect.url}")
    private String n8nWebhookConnectUrl;

    @Value("${n8n.webhook.accountBalance.url}")
    private String n8nWebhookAccountBalanceUrl;

    public void updateAccessToken(String accessToken) {
        ACCESS_TOKEN = accessToken;
    }

    public void connect(String clientId, String clientSecret, String accessToken) {

        CLIENT_ID = clientId;
        CLIENT_SECRET = clientSecret;
        ACCESS_TOKEN = accessToken;

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(
                        URI.create("wss://demo.ctraderapi.com:5035"),
                        new WebSocket.Listener() {

                            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                            @Override
                            public void onOpen(WebSocket webSocket) {
                                System.out.println("Połączono z cTrader");
                                CTraderWebSocketClient.this.webSocket = webSocket;

                                sendApplicationAuth();
                                startHeartbeat();
//                                startGoldSubscription();

                                WebSocket.Listener.super.onOpen(webSocket);
                            }

                            @Override
                            public CompletionStage<?> onBinary(
                                    WebSocket webSocket,
                                    ByteBuffer data,
                                    boolean last
                            ) {
                                byte[] chunk = new byte[data.remaining()];
                                data.get(chunk);

                                buffer.write(chunk, 0, chunk.length);

                                if (last) {
                                    byte[] fullMessage = buffer.toByteArray();
                                    buffer.reset();

                                    try {
                                        ProtoMessage msg = ProtoMessage.parseFrom(fullMessage);
                                        handleMessage(msg);

                                    } catch (Exception e) {
                                        System.err.println(
                                                "INVALID PROTOBUF (" + fullMessage.length + " bytes)"
                                        );
                                        System.err.println(toHex(fullMessage));
                                        e.printStackTrace();
                                    }
                                }

                                return WebSocket.Listener.super.onBinary(webSocket, data, last);
                            }
                        }
                ).join();
    }


    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void startTickWatcher() {

        ticksWatcher = Executors.newScheduledThreadPool(1);
        ticksWatcher.scheduleAtFixedRate(() -> {
            System.out.println("startTickWatcher");
            if(System.currentTimeMillis() - lastTickTime > 2000) {
                if(lastTickTime != 0) {
                    logout();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                connectToN8n();
            }
        }, 2, 30, TimeUnit.SECONDS);
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
        System.out.println("sendSymbolById: " +  id);
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
//
//    private void subscribeTrader() {
//        System.out.println("subscribeTrader ");
//        ProtoOASubscribe req = ProtoOASubscribeSpotsReq.newBuilder()
//                .setCtidTraderAccountId(accountId)
//                .addSymbolId(symbolId)
//                .build();
//
//        send(req, ProtoOAPayloadType.PROTO_OA_SUBSCRIBE_SPOTS_REQ_VALUE);
//    }


    private void unsubscribeFromSpots(long symbolId) {
        System.out.println("unsubscribeFromSpots " + symbolId);
        ProtoOAUnsubscribeSpotsReq req = ProtoOAUnsubscribeSpotsReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .addSymbolId(symbolId)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_UNSUBSCRIBE_SPOTS_REQ_VALUE);
    }

    public void sendGoldOrder(boolean isBuy, String messageId, String riskLvl) {
        System.out.println("sendGoldOrder");
        if(accountBalanceHalf > 0) {

            double takeProfitPips = 2; //range
//            double takeProfitPips = 5; // trend
//            double stopLossPips = calculateStopLoss(takeProfitPips, riskLvl);
            double stopLossPips = takeProfitPips * 10;
            long goldId = findSymbolByName("XAUUSD");
            double sl = 0;
            double tp = 0;

            double entry;
            if(isBuy){
                entry = lastAsk; // cena, po której ktoś chce sprzedać, czyli Ty jako kupujący musisz ją zaakceptować.
                sl = entry - (stopLossPips / Math.pow(10, symbolDigits.get(goldId)));
                tp = entry + (takeProfitPips / Math.pow(10, symbolDigits.get(goldId)));
            } else {
                entry = lastBid; // cena, po której ktoś chce kupić, a Ty sprzedajesz po tej ofercie.
                sl = entry + (stopLossPips / Math.pow(10, symbolDigits.get(goldId)));
                tp = entry - (takeProfitPips / Math.pow(10, symbolDigits.get(goldId)));
            }

            long volume = calculateDynamicVolume(symbolLotSize.get(goldId), stopLossPips);

            System.out.println("NEW Entry=" + entry + " SL=" + sl + " TP=" + tp + " Vol=" + volume);
//            sendMarketOrder(goldId, isBuy, volume, messageId);
        }
    }

    private double calculateStopLoss(double takeProfitPips, String riskLvl) {
        switch(riskLvl){
            case "LOW":
                return  takeProfitPips * 3; // RR TP 1:3 SL
            case "MEDIUM":
                return  takeProfitPips * 6; // RR TP 1:6 SL
            case "HIGH":
                return  takeProfitPips * 9; // RR TP 1:9 SL
                default:
            System.out.println("ERROR calculateStopLoss ");
                   return  takeProfitPips;
        }
    }

    private void sendMarketOrder(long symbolId, boolean isBuy, long volume, String messageId) {
        System.out.println("sendMarketOrder ");

        ProtoOANewOrderReq req = ProtoOANewOrderReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .setSymbolId(symbolId)
                .setOrderType(ProtoOAOrderType.MARKET)
                .setTradeSide(isBuy
                        ? ProtoOATradeSide.BUY
                        : ProtoOATradeSide.SELL)
                .setVolume(volume)
                .setClientOrderId(messageId)
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
//                    System.out.println("SymbolName: " + symbol.getSymbolName());
                    symbolByName.putIfAbsent(symbol.getSymbolName(), symbol.getSymbolId());
                    symbolById.putIfAbsent(symbol.getSymbolId(),symbol.getSymbolName());
                }
                System.out.println("Załadowano " + symbolByName.size() + " symboli");

                sendSymbolById(findSymbolByName("XAUUSD"));
//                sendSymbolById(findSymbolByName("US 500"));

            }
            break;

            case ProtoOAPayloadType.PROTO_OA_TRADER_UPDATE_EVENT_VALUE: {
                System.out.println("Received PROTO_OA_TRADER_UPDATE_EVENT_VALUE");
                ProtoOATraderUpdatedEvent event =
                        ProtoOATraderUpdatedEvent.parseFrom(message.getPayload());


                accountBalance = event.getTrader().getBalance();
                accountBalanceHalf = accountBalance / 2;
                System.out.println("BALANCE = " + accountBalance);
//                System.out.println("BALANCE HALF = " + accountBalanceHalf);
            }
            break;

            case ProtoOAPayloadType.PROTO_OA_SYMBOL_BY_ID_RES_VALUE: {
                System.out.println("Received PROTO_OA_SYMBOL_BY_ID_RES_VALUE");
                ProtoOASymbolByIdRes event = ProtoOASymbolByIdRes.parseFrom(message.getPayload());
                for (ProtoOASymbol protoOASymbol : event.getSymbolList()) {
                    symbolDetails.putIfAbsent(protoOASymbol.getSymbolId(), protoOASymbol);
                    System.out.println("protoOASymbol.getSymbolId() " + protoOASymbol.getSymbolId());
                }
                System.out.println("Załadowano " + symbolDetails.size() + " symboli");

                subscribeGold();
            }
            break;

            // TU beda splywac EVENTY
            case ProtoOAPayloadType.PROTO_OA_SPOT_EVENT_VALUE: {
//                System.out.println("Received PROTO_OA_SPOT_EVENT_VALUE");

                ProtoOASpotEvent event = ProtoOASpotEvent.parseFrom(message.getPayload());

//                System.out.println("TICK | symboName=" + symbolById.get(event.getSymbolId())
//                                + " BID PRICE=" + event.getBid()
//                                + " ASK=" + event.getAsk());

                lastBid = event.getBid();
                lastAsk = event.getAsk();

                ProtoOASymbol symbol = symbolDetails.get(event.getSymbolId());

                if (symbol != null ) {
//                    System.out.println(" symbol.getDigits()" + symbol.getDigits());
//                    System.out.println(" symbol.getLotSize()" + symbol.getLotSize());
                    symbolDigits.putIfAbsent(symbol.getSymbolId(), symbol.getDigits());
                    symbolLotSize.putIfAbsent(symbol.getSymbolId(), symbol.getLotSize());
                }

                sendTicksToN8n(lastBid, lastAsk ,symbolById.get(event.getSymbolId()));
            }

            case ProtoOAPayloadType.PROTO_OA_EXECUTION_EVENT_VALUE: {
                ProtoOAExecutionEvent event = null;
                try {
                     event = ProtoOAExecutionEvent.parseFrom(message.getPayload());
                    System.out.println("Received PROTO_OA_EXECUTION_EVENT_VALUE");
                } catch (InvalidProtocolBufferException e) {
//                    System.out.println("Received empty PROTO_OA_EXECUTION_EVENT_VALUE - ignoring");
                    break;
                }

                System.out.println("EXECUTION: " + event.getExecutionType());
                if (event.hasOrder()) {
                    System.out.println("ORDER ID: " + event.getOrder().getOrderId());
                }
                if (event.hasPosition()) {
                    System.out.println("POSITION ID: " + event.getPosition().getPositionId());
                    System.out.println("POSITION STATUS: " + event.getPosition().getPositionStatus());
                    System.out.println("POSITION SL: " + event.getPosition().getStopLoss());
                    System.out.println("POSITION TP: " + event.getPosition().getTakeProfit());
                    System.out.println("POSITION PRICE: " + event.getPosition().getPrice());
                    System.out.println("POSITION SWAP: " + event.getPosition().getSwap());

                    long positionId = event.getPosition().getPositionId();

//                    ProtoOASymbol symbol = symbolDetails.get(event.getPosition().getTradeData().getSymbolId());
//
//                    double stopLossPips = 3;
//                    double entry = event.getPosition().getPrice();
//                    double sl = entry - (stopLossPips / Math.pow(10, symbol.getDigits()));
//                    double tp = entry + (stopLossPips / Math.pow(10, symbol.getDigits())); // RR 1:1
//
//                    setStopLossAndTakeProfit(positionId, sl, tp);

                }
            }
            break;

            default: {
                if (message != null && message.getPayload() != null && message.getPayloadType() == 2128) {
                    //ignore to save logs/memore
                } else {
                    System.out.println("UNKNOWN Message type: " + message.getPayloadType() + " payload: " + message.getPayload());
                }
            }
        }
    }

    private void subscribeGold() {
        if (!symbolByName.isEmpty() && symbolByName.get("XAUUSD") != null){
            subscribeToTicks(symbolByName.get("XAUUSD"));
        }
    }

//    public void startGoldSubscription() {
//
//        goldSubscriptionScheduler = Executors.newSingleThreadScheduledExecutor();
//        goldSubscriptionScheduler.scheduleAtFixedRate(() -> {
//            try {
//                System.out.println(">>> XAUUSD subscribe START");
//                System.out.println(">>> XAUUSD unsubscribe");
//                unsubscribeFromSpots(symbolByName.get("XAUUSD"));
//
//                Thread.sleep(1000);
//
//                System.out.println(">>> XAUUSD subscribe");
//                subscribeToTicks(symbolByName.get("XAUUSD"));
//                System.out.println(">>> XAUUSD subscribe END");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }, 30, 30, TimeUnit.MINUTES);
//    }

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
        unsubscribeFromSpots(symbolByName.get("XAUUSD"));
        logout();
    }

    @PostConstruct
    public void init() {
        startTickWatcher();
    }

    public void logout() {
        System.out.println("logout ");
        ProtoOAAccountLogoutReq req = ProtoOAAccountLogoutReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_ACCOUNT_LOGOUT_REQ_VALUE);
    }


    private long calculateDynamicVolume(Long  lotSize,  double stopLossPips) {
        double riskPercent = 10.0;
        double riskAmount = accountBalanceHalf * (riskPercent / 100.0);

        double pipValue = 1.0; //XAU, BTC

        double lots = riskAmount / (stopLossPips * pipValue);
        double units = lots * lotSize;

        return Math.max(1000, Math.round(units));
    }


    public Long findSymbolByName(String name) {
        return symbolByName.get(name);
    }

    private void sendTicksToN8n(double lastBid, double lastAsk, String symbolName) {
//       System.out.println("Sending ticks to n8n n8nWebhookTicksUrl: " + n8nWebhookTicksUrl);

        lastTickTime = System.currentTimeMillis();
        String url = n8nWebhookTicksUrl;
        PriceRequest request = new PriceRequest(lastBid, lastAsk, symbolByName.get(symbolName) ,symbolName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PriceRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getBody().contains("Workflow was started")) {
            // skip to save memory
        } else {
            System.out.println(response.getBody());
        }

    }

    private void connectToN8n() {
       System.out.println("Sending connect request n8nWebhookConnectUrl: " + n8nWebhookConnectUrl);
        String url = n8nWebhookConnectUrl;
        ConnectRequest request = new ConnectRequest();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ConnectRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getBody().contains("Workflow was started")) {
            // skip to save memory
        } else {
            System.out.println(response.getBody());
        }

    }

    private void sendAccountBalanceToN8n(double accountBalance) {
        System.out.println("Sending account balance to n8n n8nWebhookAccountBalanceUrl: " + n8nWebhookAccountBalanceUrl);

        String url = n8nWebhookAccountBalanceUrl;
        AccountRequest request = new AccountRequest(accountBalance);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AccountRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        System.out.println(response.getBody());
    }



}
