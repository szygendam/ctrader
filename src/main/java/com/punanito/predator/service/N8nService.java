package com.punanito.predator.service;

import com.punanito.predator.model.AccountRequest;
import com.punanito.predator.model.ConnectRequest;
import com.punanito.predator.model.PositionRequest;
import com.punanito.predator.model.PriceRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

@Service
public class N8nService {
    private static final Logger logger = LoggerFactory.getLogger(N8nService.class);

    @Qualifier("n8nRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${n8n.webhook.ticks.url}")
    private String n8nWebhookTicksUrl;

    @Value("${n8n.webhook.connect.url}")
    private String n8nWebhookConnectUrl;

    @Value("${n8n.webhook.accountBalance.url}")
    private String n8nWebhookAccountBalanceUrl;

    @Value("${n8n.webhook.order.url}")
    private String n8nWebhookOrderUrl;

    public N8nService(@Qualifier("n8nRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void connectToN8n() {
        logger.info("Sending connect request n8nWebhookConnectUrl: " + n8nWebhookConnectUrl);
         // zakladamy ze sie nie uda bo w response dostajemy jedynie {"message":"Workflow was started"} -  odebranie tick zeruje connectErrorCount
        String url = n8nWebhookConnectUrl;
        ConnectRequest request = new ConnectRequest();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ConnectRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        logger.info(response.getBody());

    }

    @Async("n8nTickExecutor")
    public void sendTicksToN8n(long lastBid, long lastAsk, String symbolName,Integer symbolId) {

//       logger.info("Sending ticks to n8n n8nWebhookTicksUrl: " + n8nWebhookTicksUrl);
        if(symbolId == null) {
            logger.warn("symbolId is null ");
            return;
        }

        String url = n8nWebhookTicksUrl;
        PriceRequest request = new PriceRequest(new BigDecimal(lastBid), new BigDecimal(lastAsk),symbolId, symbolName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PriceRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getBody().contains("Workflow was started")) {
            // skip to save memory
        } else {
            logger.warn(response.getBody());
        }

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


    @Async("n8nOrderExecutor")
    public void sendNotFoundOrderToN8n(long positionId) {
        logger.info("sendNotFoundOrderToN8n " + positionId);
        sendOrderToN8n(0,positionId, StringUtils.EMPTY,StringUtils.EMPTY,StringUtils.EMPTY,StringUtils.EMPTY,0,0,0,0,true, 0,0);
    }

    @Async("n8nOrderExecutor")
    public void sendOrderToN8n(long orderId, long positionId, String clientId, String executionType, String positionStatus,
                                String orderStatus, double priceOpen, double sl, double tp, double execPrice, boolean positionNotFound, double grossProfit, long accountBalance) {
        logger.info("sendOrderToN8n " + orderStatus);
        String url = n8nWebhookOrderUrl;
        PositionRequest request = new PositionRequest(positionId, clientId,orderId,positionStatus,orderStatus,executionType,clientId,priceOpen,tp,sl, execPrice, positionNotFound, grossProfit, accountBalance);
        logger.info("PositionRequest: {}", request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PositionRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getBody().contains("Workflow was started")) {
            // skip to save memory
        } else {
            logger.warn(response.getBody());
        }
    }

}
