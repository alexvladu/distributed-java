# Stock market service

Jetty 12 WAR that simulates the market. It starts with AAPL, AMZN, MSFT, NVDA, and TSLA, publishes
the full list, and then changes one random price every 5–15 seconds (a small Gaussian move,
never below $0.01). Every price is published as a `StockPriceEvent` (see `stock-market-contracts`)
to the `stock-prices` Kafka topic, keyed by symbol.

- The first publication runs in the background, so Jetty starts even if Kafka is slow.
- `GET /health` reports `UP` when a price was published in the last minute, `STARTING` before the
  first one, and `DOWN` (HTTP 503) otherwise, with the last error.
- `KAFKA_BOOTSTRAP_SERVERS` and `KAFKA_TOPIC` override `localhost:9092` and `stock-prices`.

The listener and the health servlet are declared in `WEB-INF/web.xml` because the Jetty base does
not enable annotation scanning.

## Build and deploy

From `stock-market-project`:

```bash
./stock-market-service/deploy.sh
```

This builds the module (and the shared contracts module), creates the Jetty base with the `http`
and `ee10-deploy` modules if needed, and copies the WAR to
`$SERVERS_DIR/jetty-base/webapps/ROOT.war`. Start Jetty with:

```bash
cd "$SERVERS_DIR/jetty-base"
java -jar "$(brew --prefix jetty)/libexec/start.jar" jetty.http.port=8082
```
