package com.punanito.ctraderbridge.service;

import com.punanito.ctraderbridge.config.CTraderConfig;
import com.xtrader.protocol.proto.commons.ProtoMessage;
import com.xtrader.protocol.openapi.v2.ProtoOASpotEvent;
import com.xtrader.protocol.openapi.v2.model.ProtoOAPayloadType;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CTraderStreamService {

    private final CTraderConfig config;
    private Channel channel;
    private final AtomicBoolean streaming = new AtomicBoolean(false);
    private long subscribedSymbolId = -1;

    @Value("${n8n.webhook.url}")
    private String n8nWebhookUrl;

    private final WebClient webClient;

    public CTraderStreamService(CTraderConfig config, WebClient.Builder webClientBuilder) {
        this.config = config;
        this.webClient = webClientBuilder.build();
    }

    @PostConstruct
    public void init() throws Exception {
        connect();
    }

    @PreDestroy
    public void cleanup() {
        if (channel != null) {
            channel.close();
        }
    }

    private void connect() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new ProtobufVarint32FrameDecoder());
                        p.addLast(new ProtobufDecoder(ProtoMessage.getDefaultInstance()));
                        p.addLast(new ProtobufVarint32LengthFieldPrepender());
                        p.addLast(new ProtobufEncoder());
                        p.addLast(new CTraderStreamHandler());
                    }
                });

        channel = b.connect(config.getHost(), config.getPort()).sync().channel();
        System.out.println("Connected to cTrader API at " + config.getHost() + ":" + config.getPort());
    }

    public void subscribeSpots(long accountId, long symbolId) {
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("Channel not connected");
        }
        if (streaming.get()) {
            System.out.println("Already streaming symbol " + subscribedSymbolId);
            return;
        }

        this.subscribedSymbolId = symbolId;

        // build and send subscribe request …

        streaming.set(true);
        System.out.println("Subscribed to spots for symbolId=" + symbolId);
    }

    private class CTraderStreamHandler extends SimpleChannelInboundHandler<ProtoMessage> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ProtoMessage msg) throws Exception {
            if (ProtoOAPayloadType.PROTO_OA_SPOT_EVENT.equals(msg.getPayloadType() )) {
                ProtoOASpotEvent event = ProtoOASpotEvent.parseFrom(msg.getPayload());
                if (subscribedSymbolId == event.getSymbolId()) {
                    long rawBid = event.getBid();
                    long rawAsk = event.getAsk();
                    double bid = rawBid / 100000.0;
                    double ask = rawAsk / 100000.0;
                    System.out.println("Spot event for symbolId=" + subscribedSymbolId + " bid=" + bid + " ask=" + ask);

                    sendToN8n(subscribedSymbolId, bid, ask);
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }

    private void sendToN8n(long symbolId, double bid, double ask) {
        PriceUpdateDto dto = new PriceUpdateDto(symbolId, bid, ask);

        webClient.post()
                .uri(n8nWebhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                        unused -> System.out.println("Sent price update to n8n"),
                        err -> System.err.println("Error sending price update to n8n: " + err.getMessage())
                );
    }

    private static class PriceUpdateDto {
        private long symbolId;
        private double bid;
        private double ask;

        public PriceUpdateDto(long symbolId, double bid, double ask) {
            this.symbolId = symbolId;
            this.bid = bid;
            this.ask = ask;
        }

        public long getSymbolId() { return symbolId; }
        public double getBid() { return bid; }
        public double getAsk() { return ask; }
    }
}
