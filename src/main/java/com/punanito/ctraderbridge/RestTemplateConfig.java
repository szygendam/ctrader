package com.punanito.ctraderbridge;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Configuration
public class RestTemplateConfig {

    @Bean(name = "n8nRestTemplate")
    public RestTemplate restTemplate() {
        // --- Pool połączeń ---
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);            // max wszystkich połączeń
        cm.setDefaultMaxPerRoute(50);   // max na jeden host (route)

        // --- Timeouty (ms) ---
        int connectTimeoutMs = 3_000;              // handshake TCP
        int connectionRequestTimeoutMs = 2_000;    // czekanie na połączenie z puli
        int socketReadTimeoutMs = 10_000;          // read timeout

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeoutMs)
                .setConnectionRequestTimeout(connectionRequestTimeoutMs)
                .setSocketTimeout(socketReadTimeoutMs)
                .build();

        // --- Keep-Alive (jeśli serwer nie poda keep-alive w nagłówkach, użyj fallback) ---
        ConnectionKeepAliveStrategy keepAliveStrategy = (response, context) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator(
                    response.headerIterator(HTTP.CONN_KEEP_ALIVE)
            );
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && "timeout".equalsIgnoreCase(param)) {
                    try {
                        return Long.parseLong(value) * 1000L; // sek -> ms
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return 30_000L; // fallback 30s
        };

        // --- HttpClient ---
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .setKeepAliveStrategy(keepAliveStrategy)
                .evictIdleConnections(30, TimeUnit.SECONDS) // wyrzucaj idle
                .evictExpiredConnections()                  // wyrzucaj przeterminowane
                .disableAutomaticRetries()                  // opcjonalnie: bez retry (lepsza kontrola)
                .build();

        // --- Factory dla RestTemplate ---
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        // Dodatkowe zabezpieczenie (nadpisuje requestConfig w razie potrzeby)
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setConnectionRequestTimeout(connectionRequestTimeoutMs);
        factory.setReadTimeout(socketReadTimeoutMs);

        return new RestTemplate(factory);
    }
}