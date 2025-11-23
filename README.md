# cTrader Bridge Service

## 🧠 Wprowadzenie
„cTrader Bridge Service” to serwis napisany w Java (Spring Boot), który realizuje integrację z cTrader Open API v2 i umożliwia:
- autoryzację konta tradera
- otwarcie połączenia streamingowego (notowania np. symbolu XAUUSD)
- przesyłanie aktualizacji kursów do n8n przez endpoint Webhook

## 📦 Wykorzystane technologie
- Java 17
- Spring Boot 3.x
- Netty (do połączenia TCP/WebSocket z cTrader)
- Google Protobuf (klasy wygenerowane z plików `.proto` z cTrader)
- WebClient (Spring WebFlux) – do wywołań REST / Webhook do n8n

## 🛠 Funkcjonalności
- REST endpoint `/auth/complete` który przyjmuje `code`, `access_token` i `refresh_token`
- REST endpoint `/auth/update‑tokens` który przyjmuje nowe wartości `code`, `access_token`, `refresh_token`
- REST endpointy `/trade/order` i `/trade/position/close` do zleceń i zamykania pozycji
- W przypadku otrzymania `ACCESS_DENIED` od cTrader API → wywołanie webhooka w n8n
- Streaming kursów symbolu i wysyłanie ich do n8n

## 📋 Konfiguracja

W pliku `src/main/resources/application.yml` ustaw:

```yaml
ctrader:
  host: your‑ctrader‑host  
  port: 5035                
  clientPublicId: YOUR_CLIENT_PUBLIC_ID
  clientSecret: YOUR_CLIENT_SECRET

n8n:
  webhook:
    url: "http://localhost:5678/webhook/your‑path"
    accessDeniedUrl: "http://localhost:5678/webhook/access‑denied"
