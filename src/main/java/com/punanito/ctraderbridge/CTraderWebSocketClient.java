package com.punanito.ctraderbridge;

import com.google.protobuf.InvalidProtocolBufferException;
import com.punanito.ctraderbridge.controller.ConnectionController;
import com.punanito.ctraderbridge.model.AccountRequest;
import com.punanito.ctraderbridge.model.ConnectRequest;
import com.punanito.ctraderbridge.model.PositionRequest;
import com.punanito.ctraderbridge.model.PriceRequest;
import com.xtrader.protocol.openapi.v2.ProtoOAAccountAuthReq;
import com.xtrader.protocol.openapi.v2.ProtoOAAccountLogoutReq;
import com.xtrader.protocol.openapi.v2.ProtoOAAmendPositionSLTPReq;
import com.xtrader.protocol.openapi.v2.ProtoOAApplicationAuthReq;
import com.xtrader.protocol.openapi.v2.ProtoOAClosePositionReq;
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
import com.xtrader.protocol.openapi.v2.model.ProtoOAExecutionType;
import com.xtrader.protocol.openapi.v2.model.ProtoOALightSymbol;
import com.xtrader.protocol.openapi.v2.model.ProtoOAOrder;
import com.xtrader.protocol.openapi.v2.model.ProtoOAOrderType;
import com.xtrader.protocol.openapi.v2.model.ProtoOAPayloadType;
import com.xtrader.protocol.openapi.v2.model.ProtoOASymbol;
import com.xtrader.protocol.openapi.v2.model.ProtoOATradeSide;
import com.xtrader.protocol.proto.commons.ProtoHeartbeatEvent;
import com.xtrader.protocol.proto.commons.ProtoMessage;
import com.xtrader.protocol.proto.commons.model.ProtoPayloadType;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(CTraderWebSocketClient.class);

    private static String ACCESS_TOKEN = Strings.EMPTY;
    private static String CLIENT_ID = Strings.EMPTY;
    private static String CLIENT_SECRET = Strings.EMPTY;

    WebSocket webSocket;
    long accountId;
    long lastTickTime = 0;
    int connectErrorCount = 0;
    private Map<Long, ProtoOASymbol> symbolDetails = new HashMap<>();
    private Map<String, Long> symbolByName = new HashMap<>();
    private Map<Long, String> symbolById = new HashMap<>();
    private Map<Long, Integer> symbolDigits = new HashMap<>();
    private Map<Long, Long> symbolLotSize = new HashMap<>();
    private double accountBalance = 0.0;
    private double accountBalanceHalf = 500;
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

    @Value("${n8n.webhook.order.url}")
    private String n8nWebhookOrderUrl;

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
                                logger.info("Połączono z cTrader");
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
            logger.info("startTickWatcher");
            if (System.currentTimeMillis() - lastTickTime > 2000) {
                if (lastTickTime != 0) {
                    logout();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (connectErrorCount < 3) {
                    connectToN8n();
                } else {
                    logger.info("connectErrorCount > 3 skipping connectToN8n");
                    logout();
                }
            }
        }, 2, 20, TimeUnit.SECONDS);
    }

    // Autoryzacja aplikacji
    private void sendApplicationAuth() {
        logger.info("sendApplicationAuth");
        ProtoOAApplicationAuthReq req = ProtoOAApplicationAuthReq.newBuilder()
                .setClientId(CLIENT_ID)
                .setClientSecret(CLIENT_SECRET)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_APPLICATION_AUTH_REQ_VALUE);
    }

    // Pobranie symbolList
    private void sendSymbolList() {
        logger.info("sendSymbolList");
        ProtoOASymbolsListReq req = ProtoOASymbolsListReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_SYMBOLS_LIST_REQ_VALUE);
    }

    // Pobranie szczegółów
    private void sendSymbolById(long id) {
        logger.info("sendSymbolById: " + id);
        ProtoOASymbolByIdReq req = ProtoOASymbolByIdReq.newBuilder()
                .addSymbolId(id)
                .setCtidTraderAccountId(accountId)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_SYMBOL_BY_ID_REQ_VALUE);
    }

    //  Pobierz listę kont
    private void sendGetAccountList() {
        logger.info("sendGetAccountList ");
        ProtoOAGetAccountListByAccessTokenReq req =
                ProtoOAGetAccountListByAccessTokenReq.newBuilder()
                        .setAccessToken(ACCESS_TOKEN)
                        .build();

        send(req, ProtoOAPayloadType.PROTO_OA_GET_ACCOUNTS_BY_ACCESS_TOKEN_REQ_VALUE);
    }

    //  Autoryzuj konkretne konto
    private void sendAccountAuth() {
        logger.info("sendAccountAuth ");
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

    private void subscribeToTicks(long symbolId, String symbolName) {
        logger.info("subscribeToTicks " + symbolName);
        ProtoOASubscribeSpotsReq req = ProtoOASubscribeSpotsReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .addSymbolId(symbolId)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_SUBSCRIBE_SPOTS_REQ_VALUE);
    }


    private void unsubscribeFromSpots(long symbolId) {
        logger.info("unsubscribeFromSpots " + symbolId);
        ProtoOAUnsubscribeSpotsReq req = ProtoOAUnsubscribeSpotsReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .addSymbolId(symbolId)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_UNSUBSCRIBE_SPOTS_REQ_VALUE);
    }

    public void sendGoldOrder(String operation, String message) {
        logger.info("sendGoldOrder");
        boolean isBuy = operation.equals("LONG");
        if (accountBalanceHalf > 0) {

            double takeProfitPips = 4; //range
//            double takeProfitPips = 5; // trend
//            double stopLossPips = calculateStopLoss(takeProfitPips);
            double stopLossPips = takeProfitPips * 3;
            long goldId = findSymbolByName("XAUUSD");
//            double sl = 0;
//            double tp = 0;
//
//            double entry;
//            if (isBuy) {
//                entry = lastAsk; // cena, po której ktoś chce sprzedać, czyli Ty jako kupujący musisz ją zaakceptować.
//                sl = entry - (stopLossPips / Math.pow(10, symbolDigits.get(goldId)));
//                tp = entry + (takeProfitPips / Math.pow(10, symbolDigits.get(goldId)));
//            } else {
//                entry = lastBid; // cena, po której ktoś chce kupić, a Ty sprzedajesz po tej ofercie.
//                sl = entry + (stopLossPips / Math.pow(10, symbolDigits.get(goldId)));
//                tp = entry - (takeProfitPips / Math.pow(10, symbolDigits.get(goldId)));
//            }

//            long volume = calculateDynamicVolume(symbolLotSize.get(goldId), stopLossPips);
            long volume = 100;

//            logger.info("NEW Entry=" + entry + " SL=" + sl + " TP=" + tp + " Vol=" + volume);
//            ProtoOASymbol xauusd = symbolDetails.get(41);
//            logger.info("XAUUSD =" + xauusd.toString());
            sendMarketOrder(goldId, isBuy, volume, message);
        }
    }


    private void sendMarketOrder(long symbolId, boolean isBuy, long volume, String message) {
        logger.info("sendMarketOrder message: {} volume: {}", message, volume);

        ProtoOANewOrderReq req = ProtoOANewOrderReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .setSymbolId(symbolId)
                .setOrderType(ProtoOAOrderType.MARKET)
                .setTradeSide(isBuy
                        ? ProtoOATradeSide.BUY
                        : ProtoOATradeSide.SELL)
                .setVolume(volume)
                .setClientOrderId(message)
                .build();
        send(req, ProtoOAPayloadType.PROTO_OA_NEW_ORDER_REQ_VALUE);
    }


    public void protect(double sl, double tp, long positionId) {
        logger.info("protect sl:{}, tp:{}, positionId:{} ", sl, tp, positionId);

        ProtoOAAmendPositionSLTPReq req = ProtoOAAmendPositionSLTPReq.newBuilder()
                .setStopLoss(sl)
                .setTakeProfit(tp)
                .setPositionId(positionId)
                .setCtidTraderAccountId(accountId)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_AMEND_POSITION_SLTP_REQ_VALUE);
    }

    // 📥 Odbieranie wiadomości
    private void handleMessage(ProtoMessage message) throws InvalidProtocolBufferException {

        switch (message.getPayloadType()) {

            case ProtoOAPayloadType.PROTO_OA_APPLICATION_AUTH_RES_VALUE: {
                logger.info("Received PROTO_OA_APPLICATION_AUTH_RES_VALUE");
                logger.info("Application authenticated ✅");
                sendGetAccountList();
            }
            break;

            case ProtoOAPayloadType.PROTO_OA_GET_ACCOUNTS_BY_ACCESS_TOKEN_RES_VALUE: {
                logger.info("Received PROTO_OA_GET_ACCOUNTS_BY_ACCESS_TOKEN_RES_VALUE");
                ProtoOAGetAccountListByAccessTokenRes res =
                        ProtoOAGetAccountListByAccessTokenRes.parseFrom(message.getPayload());

                accountId = res.getCtidTraderAccountList().get(0).getCtidTraderAccountId();
                logger.info("Wybrano konto: " + accountId);

                sendAccountAuth();
            }
            break;

            case ProtoOAPayloadType.PROTO_OA_ACCOUNT_AUTH_RES_VALUE: {
                logger.info("Received PROTO_OA_ACCOUNT_AUTH_RES_VALUE");
                logger.info("Konto autoryzowane ");
                logger.info("Gotowy do subskrypcji i nasłuchiwania danych...");
                sendSymbolList();
            }
            break;

            case ProtoOAPayloadType.PROTO_OA_SYMBOLS_LIST_RES_VALUE: {
                logger.info("Received PROTO_OA_SYMBOLS_LIST_RES_VALUE");

                ProtoOASymbolsListRes res =
                        ProtoOASymbolsListRes.parseFrom(message.getPayload());

                for (ProtoOALightSymbol symbol : res.getSymbolList()) {
//                    logger.info("SymbolName: " + symbol.getSymbolName());
                    symbolByName.putIfAbsent(symbol.getSymbolName(), symbol.getSymbolId());
                    symbolById.putIfAbsent(symbol.getSymbolId(), symbol.getSymbolName());
                }
                logger.info("Załadowano " + symbolByName.size() + " symboli");

                sendSymbolById(findSymbolByName("XAUUSD"));
//                sendSymbolById(findSymbolByName("US 500"));

            }
            break;

            case ProtoOAPayloadType.PROTO_OA_TRADER_UPDATE_EVENT_VALUE: {
                logger.info("Received PROTO_OA_TRADER_UPDATE_EVENT_VALUE");
                ProtoOATraderUpdatedEvent event =
                        ProtoOATraderUpdatedEvent.parseFrom(message.getPayload());


                accountBalance = event.getTrader().getBalance();
                accountBalanceHalf = accountBalance / 2;
                logger.info("BALANCE = " + accountBalance);
//                logger.info("BALANCE HALF = " + accountBalanceHalf);
            }
            break;

            case ProtoOAPayloadType.PROTO_OA_SYMBOL_BY_ID_RES_VALUE: {
                logger.info("Received PROTO_OA_SYMBOL_BY_ID_RES_VALUE");
                ProtoOASymbolByIdRes event = ProtoOASymbolByIdRes.parseFrom(message.getPayload());
                for (ProtoOASymbol protoOASymbol : event.getSymbolList()) {
                    symbolDetails.putIfAbsent(protoOASymbol.getSymbolId(), protoOASymbol);
                    logger.info("protoOASymbol.getSymbolId() {} lotSize {} maxVolume {} mixVolume {} digits {} getMeasurementUnits {} ", protoOASymbol.getSymbolId(), protoOASymbol.getLotSize(), protoOASymbol.getMaxVolume(), protoOASymbol.getMinVolume(), protoOASymbol.getDigits(), protoOASymbol.getMeasurementUnits());
                    switch ((int) protoOASymbol.getSymbolId()) {
                        case 41:
                            subscribeGold();
                            break;
                        case 21499:
                            subscribeUS500();
                            break;
                    }
                }
//                logger.info("Załadowano " + symbolDetails.size() + " symboli");
            }
            break;

            // TU beda splywac EVENTY
            case ProtoOAPayloadType.PROTO_OA_SPOT_EVENT_VALUE: {
//                logger.info("Received PROTO_OA_SPOT_EVENT_VALUE");

                ProtoOASpotEvent event = ProtoOASpotEvent.parseFrom(message.getPayload());

//                logger.info("TICK | symboName=" + symbolById.get(event.getSymbolId())
//                                + " BID PRICE=" + event.getBid()
//                                + " ASK=" + event.getAsk());

                lastBid = event.getBid();
                lastAsk = event.getAsk();

                ProtoOASymbol symbol = symbolDetails.get(event.getSymbolId());

                if (symbol != null) {
//                    logger.info(" symbol.getDigits()" + symbol.getDigits());
//                    logger.info(" symbol.getLotSize()" + symbol.getLotSize());
                    symbolDigits.putIfAbsent(symbol.getSymbolId(), symbol.getDigits());
                    symbolLotSize.putIfAbsent(symbol.getSymbolId(), symbol.getLotSize());
                }

                sendTicksToN8n(lastBid, lastAsk, symbolById.get(event.getSymbolId()));
            }

            case ProtoOAPayloadType.PROTO_OA_EXECUTION_EVENT_VALUE: {
                ProtoOAExecutionEvent event = null;
                try {
                    event = ProtoOAExecutionEvent.parseFrom(message.getPayload());
                    logger.info("Received PROTO_OA_EXECUTION_EVENT_VALUE");
                } catch (InvalidProtocolBufferException e) {
//                    logger.info("Received empty PROTO_OA_EXECUTION_EVENT_VALUE - ignoring");
                    break;
                }

                if(event.getExecutionType() == ProtoOAExecutionType.ORDER_REPLACED){
                    logger.debug("Received PROTO_OA_EXECUTION_EVENT_VALUE ORDER REPLACED");
                    return;
                }
                logger.info("EXECUTION: " + event.getExecutionType());
                double execPrice = 0.0;
                if (event.hasOrder()) {
                    logger.info("ORDER ID: " + event.getOrder().getOrderId());
                    if (event.hasPosition()) {
                        logger.info("ORDER STOP PRICE: " + event.getOrder().getStopPrice());
                        logger.info("ORDER CLIENT ID: " + event.getOrder().getClientOrderId());
                        logger.info("ORDER EXECUTION PRICE: " + event.getOrder().getExecutionPrice());
                        execPrice = event.getOrder().getExecutionPrice();
                        logger.info("ORDER STOP PRICE: " + event.getOrder().getStopPrice());
                        logger.info("ORDER ISS TOP OUT: " + event.getOrder().getIsStopOut());
                        logger.info("ORDER SL: " + event.getOrder().getStopLoss());
                        logger.info("ORDER TP: " + event.getOrder().getTakeProfit());
                        logger.info("ORDER STATUS: " + event.getOrder().getOrderStatus());
                    }
                }

                if (event.hasPosition()) {
                    logger.info("POSITION ID: " + event.getPosition().getPositionId());
                    logger.info("POSITION STATUS: " + event.getPosition().getPositionStatus());
                    logger.info("POSITION SL: " + event.getPosition().getStopLoss());
                    logger.info("POSITION TP: " + event.getPosition().getTakeProfit());
                    logger.info("POSITION PRICE: " + event.getPosition().getPrice());
                    logger.info("POSITION SWAP: " + event.getPosition().getSwap());
                    logger.info("POSITION COMISSION: " + event.getPosition().getCommission());
                    logger.info("POSITION MARGIN RATE: " + event.getPosition().getMarginRate());
                    logger.info("POSITION USED MARGIN: " + event.getPosition().getUsedMargin());

                    sendOrderToN8n(event.getOrder().getOrderId(),
                            event.getPosition().getPositionId(),
                            event.getOrder().getClientOrderId(),
                            event.getExecutionType().name(),
                            event.getPosition().getPositionStatus().name(),
                            event.getOrder().getOrderStatus().name(),
                            event.getPosition().getPrice(),
                            event.getPosition().getStopLoss(),
                            event.getPosition().getTakeProfit(),
                            execPrice
                    );
                }
            }
            break;

            default: {
                if (message != null && message.getPayload() != null && message.getPayloadType() == 2128) {
                    //ignore to save logs/memore
                } else  {
                    logger.info("UNKNOWN Message type: " + message.getPayloadType() + " payload: " + message.getPayload());
                }
            }
        }
    }

    private void subscribeGold() {
        if (!symbolByName.isEmpty() && symbolByName.get("XAUUSD") != null){
            subscribeToTicks(symbolByName.get("XAUUSD"), "XAUUSD");
        }
    }

    private void subscribeUS500() {
        if (!symbolByName.isEmpty() && symbolByName.get("US 500") != null){
            subscribeToTicks(symbolByName.get("US 500"),"US 500");
        }
    }

    public void startHeartbeat() {
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
        }
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                ProtoHeartbeatEvent hb = ProtoHeartbeatEvent.newBuilder().build();
                send(hb, ProtoPayloadType.HEARTBEAT_EVENT_VALUE);
                logger.info(">>> heartbeat sent");
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
        unsubscribeFromSpots(symbolByName.get("US 500"));
        logout();
    }

    @PostConstruct
    public void init() {
        startTickWatcher();
    }

    public void logout() {
        logger.info("logout ");
        ProtoOAAccountLogoutReq req = ProtoOAAccountLogoutReq.newBuilder()
                .setCtidTraderAccountId(accountId)
                .build();

        send(req, ProtoOAPayloadType.PROTO_OA_ACCOUNT_LOGOUT_REQ_VALUE);
    }


    public Long findSymbolByName(String name) {
        return symbolByName.get(name);
    }

    private void sendTicksToN8n(double lastBid, double lastAsk, String symbolName) {
        lastTickTime = System.currentTimeMillis();
        connectErrorCount = 0;
//       logger.info("Sending ticks to n8n n8nWebhookTicksUrl: " + n8nWebhookTicksUrl + " lastTickTime: " + lastTickTime);

        String url = n8nWebhookTicksUrl;
        PriceRequest request = new PriceRequest(lastBid, lastAsk, symbolByName.get(symbolName) ,symbolName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PriceRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getBody().contains("Workflow was started")) {
            // skip to save memory
        } else {
            logger.info(response.getBody());
        }

    }

    private void sendOrderToN8n(long orderId, long positionId, String clientId, String executionType, String positionStatus,
                                String orderStatus, double priceOpen, double sl, double tp, double execPrice) {
        logger.info("sendOrderToN8n " + orderStatus);
        String url = n8nWebhookOrderUrl;
        PositionRequest request = new PositionRequest(positionId, clientId,orderId,positionStatus,orderStatus,executionType,clientId,priceOpen,tp,sl, execPrice);
        logger.info("PositionRequest: {}", request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PositionRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getBody().contains("Workflow was started")) {
            // skip to save memory
        } else {
            logger.info(response.getBody());
        }

    }

    private void connectToN8n() {
       logger.info("Sending connect request n8nWebhookConnectUrl: " + n8nWebhookConnectUrl);
        connectErrorCount++; // zakladamy ze sie nie uda bo w response dostajemy jedynie {"message":"Workflow was started"} -  odebranie tick zeruje connectErrorCount
        String url = n8nWebhookConnectUrl;
        ConnectRequest request = new ConnectRequest();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ConnectRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        logger.info(response.getBody());

    }

    private void sendAccountBalanceToN8n(double accountBalance) {
        logger.info("Sending account balance to n8n n8nWebhookAccountBalanceUrl: " + n8nWebhookAccountBalanceUrl);

        String url = n8nWebhookAccountBalanceUrl;
        AccountRequest request = new AccountRequest(accountBalance);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AccountRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        logger.info(response.getBody());
    }


    public void close(long positionId) {
            logger.info("close positionId:{} ", positionId);

            ProtoOAClosePositionReq req = ProtoOAClosePositionReq.newBuilder()
                    .setPositionId(positionId)
                    .setCtidTraderAccountId(accountId)
                    .build();

            send(req, ProtoOAPayloadType.PROTO_OA_CLOSE_POSITION_REQ_VALUE);
        }
    }

