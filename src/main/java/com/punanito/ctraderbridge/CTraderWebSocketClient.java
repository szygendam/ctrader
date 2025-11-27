package com.punanito.ctraderbridge;

import com.google.protobuf.InvalidProtocolBufferException;
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

//@Singleton
public class CTraderWebSocketClient {

    private static final String ACCESS_TOKEN = "TWOJ_ACCESS_TOKEN";
    private static final String CLIENT_ID = "TWOJ_CLIENT_ID";
    private static final String CLIENT_SECRET = "TWOJ_CLIENT_SECRET";

    WebSocket webSocket;
    long accountId;
    private final Map<Long, ProtoOASymbol> symbolDetails = new HashMap<>();
    private final Map<String, Long> symbolByName = new HashMap<>();
    private double accountBalance = 0.0;
    private double lastBid = 0;
    private double lastAsk = 0;
    private ScheduledExecutorService heartbeatScheduler;



    public void connect() {

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create("wss://demo.ctraderapi.com:5036"), new WebSocket.Listener() {

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        System.out.println("Połączono z cTrader");

                        CTraderWebSocketClient.this.webSocket = webSocket;


                        sendApplicationAuth();
                        sendSymbolList();
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

        ProtoOAGetAccountListByAccessTokenReq req =
                ProtoOAGetAccountListByAccessTokenReq.newBuilder()
                        .setAccessToken(ACCESS_TOKEN)
                        .build();

        send(req, ProtoOAPayloadType.PROTO_OA_GET_ACCOUNTS_BY_ACCESS_TOKEN_REQ_VALUE);
    }

    //  Autoryzuj konkretne konto
    private void sendAccountAuth() {

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

        ProtoOASubscribeSpotsReq req = ProtoOASubscribeSpotsReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .addSymbolId(symbolId)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_SUBSCRIBE_SPOTS_REQ_VALUE);
    }


    private void sendMarketOrder(long symbolId, boolean isBuy, long volume) {


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

                // np. EURUSD
                long EURUSD_SYMBOL_ID = 1; // <- zmień na właściwy symbol z Twojego serwera

//                subscribeToTicks(EURUSD_SYMBOL_ID); //TODO
            }
            break;

            case ProtoOAPayloadType.PROTO_OA_SYMBOLS_LIST_RES_VALUE:  {
                System.out.println("Received PROTO_OA_SYMBOLS_LIST_RES_VALUE");

                ProtoOASymbolsListRes res =
                        ProtoOASymbolsListRes.parseFrom(message.getPayload());

                for (ProtoOALightSymbol symbol : res.getSymbolList()) {
                    symbolByName.put(symbol.getSymbolName(), symbol.getSymbolId());
                }
                System.out.println("Załadowano " + symbolByName.size() + " symboli");

                sendSymbolById(findSymbolByName("XAUUSD"));

//                if (goldId != null) {
//                    System.out.println("XAUUSD ID: " + goldId);
//                    subscribeToTicks(goldId);
//                }
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
            }
            break;

            // TU beda splywac EVENTY
            case ProtoOAPayloadType.PROTO_OA_SPOT_EVENT_VALUE: {
                System.out.println("Received PROTO_OA_SPOT_EVENT_VALUE");
                ProtoOASpotEvent event = ProtoOASpotEvent.parseFrom(message.getPayload());

                System.out.println(
                        "TICK | symbolId=" + event.getSymbolId()
                                + " BID PRICE=" + event.getBid()
                                + " ASK=" + event.getAsk());

                lastBid = event.getBid();
                lastAsk = event.getAsk();

                ProtoOASymbol symbol = symbolDetails.get(event.getSymbolId());

                if (symbol != null && accountBalance > 0) {

                    double stopLossPips = 20;
                    long symbolId = event.getSymbolId();

                    double entry = lastAsk;
                    double sl = entry - (stopLossPips / Math.pow(10, symbol.getDigits()));
                    double tp = entry + (stopLossPips / Math.pow(10, symbol.getDigits())); // RR 1:1 ✅

                    long volume = calculateDynamicVolume(symbol);

                    System.out.println("AUTO BUY " + symbolByName.get(symbolId));
                    System.out.println("Entry=" + entry + " SL=" + sl + " TP=" + tp + " Vol=" + volume);

                    sendMarketOrder(symbolId, true, volume);

                    // Żeby nie spamowało zleceń:
                    unsubscribeFromSpots(symbolId);
                }
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
                System.out.println("Message type: " + message.getPayloadType());
            }
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

    private long calculateDynamicVolume(
            ProtoOASymbol symbol) {
        double riskPercent = 1.0;
        double stopLossPips = 20;
        double riskAmount = accountBalance * (riskPercent / 100.0);

        double pipValue = 1.0; //XAU, BTC
        long lotSize = symbol.getLotSize();

        double lots = riskAmount / (stopLossPips * pipValue);
        double units = lots * lotSize;

        return Math.max(1000, Math.round(units));
    }

    private void unsubscribeFromSpots(long symbolId) {

        ProtoOAUnsubscribeSpotsReq req = ProtoOAUnsubscribeSpotsReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .addSymbolId(symbolId)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_UNSUBSCRIBE_SPOTS_REQ_VALUE);
    }

    public Long findSymbolByName(String name) {
        return symbolByName.get(name);
    }



}
