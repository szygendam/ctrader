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

```

## Historia ticków dla n8n

Po uruchomieniu aplikacji na porcie 8081 wywołaj istniejący endpoint
`POST /connect` z danymi aplikacji i tokenem. Poczekaj na autoryzację konta
oraz pobranie listy symboli. Endpoint historii korzysta z tego połączenia.

Gotowy przykład do importu: `n8n/tick-history-every-second.json`.
Używa `Schedule Trigger` z `Seconds Between Triggers = 1`, połączonego
z `HTTP Request`. Zmień symbol w body, zapisz i aktywuj/opublikuj workflow.
Przykład pobiera ruchome okno ostatniej sekundy; nie zapisuje kursora.

Konfiguracja węzła HTTP Request:

- metoda: `POST`
- URL: `http://host.docker.internal:8081/tick/history/get`
- typ body: JSON
- body: `{"symbol":"XAUUSD"}` (lub inny symbol dostępny na koncie)
- timeout: co najmniej 10000 ms

Przykład lokalnego wywołania:

```sh
curl --fail-with-body http://localhost:8081/tick/history/get \
  -H 'Content-Type: application/json' \
  -d '{"symbol":"XAUUSD"}'
```

Bez dat pobierana jest ostatnia sekunda według zegara serwera. Odpowiedź JSON
zawiera `symbol`, `timeFrom`, `timeTo` oraz oddzielne obiekty `bid` i `ask`.
Każdy zawiera `ticks: [{"timestamp": ..., "price": ...}]` (od najnowszego)
oraz `hasMore`. Ceny są przeliczone z jednostek protokołu; czas to Unix ms.
Brak ticków, np. poza sesją, oznacza pustą tablicę, a nie błąd.

Możesz podać `timeFrom` i `timeTo` w Unix ms, maksymalnie tydzień na żądanie.
Aby zachować ciągłość przy opóźnieniach n8n, zapisuj ostatnie udane `timeTo`
i używaj go jako kolejnego `timeFrom`; usuń ewentualne duplikaty na granicy.
Domyślne ruchome okno jednej sekundy nie gwarantuje braku luk ani duplikatów.
`hasMore: true` oznacza niepełny wynik; podziel zakres na mniejsze przedziały.
Endpoint wykonuje dwa żądania historii do cTrader na jedno wywołanie REST.

Błędy HTTP: `400` błędny symbol lub zakres, `503` połączenie nie jest gotowe,
`502` błąd cTrader/transportu, `504` przekroczone 8 sekund oczekiwania.
Nie uruchamiaj wielu równoległych pętli pobierania dla tego samego połączenia.

Budowanie i testy: `mvn test`, `mvn package`. Artefakt:
`target/ctrader.bridge-2.1.0.jar`; uruchomienie:
`java -jar target/ctrader.bridge-2.1.0.jar`.

Źródła protokołu i harmonogramu:
[cTrader tick history](https://help.ctrader.com/open-api/symbol-data/#attain-historical-tick-data),
[n8n Schedule Trigger](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.scheduletrigger/).
