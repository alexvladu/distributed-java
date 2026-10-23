# Stock market service

This is a minimal Jakarta Servlet WAR for Jetty 12.

## Build

```bash
cd /home/alex/sisteme-distribuite/stock-market-project/stock-market-service
mvn clean package
```

## Run with the Homebrew Jetty

Create a Jetty base directory once:

```bash
export JETTY_HOME="$(brew --prefix jetty)/libexec"
export JETTY_BASE="/home/alex/sisteme-distribuite/servere/jetty-base"
mkdir -p "$JETTY_BASE"
cd "$JETTY_BASE"
java -jar "$JETTY_HOME/start.jar" --add-modules=http,ee10-deploy
```

Deploy the WAR:

```bash
rm -rf "$JETTY_BASE/webapps/stock-market-service" "$JETTY_BASE/webapps/stock-market-service.war"
cp /home/alex/sisteme-distribuite/stock-market-project/stock-market-service/target/stock-market-service-1.0-SNAPSHOT.war "$JETTY_BASE/webapps/stock-market-service.war"
java -jar "$JETTY_HOME/start.jar" jetty.http.port=8082
```

Open <http://localhost:8082/stock-market-service/hello>.

The stock-market service is producer-only. It starts with AAPL, AMZN, MSFT,
NVDA, and TSLA, publishes the complete base list to Kafka during startup, and
then updates one stock every 5–15 seconds using a small random price
distribution. Each initial stock and subsequent update is published as a
`StockCompany` DTO to the `stock-prices` Kafka topic. Set
`KAFKA_BOOTSTRAP_SERVERS` or `KAFKA_TOPIC` to override the defaults.

The Kafka publisher is registered explicitly in `WEB-INF/web.xml` so startup
publishing works with Jetty deployments that do not scan `@WebListener`
annotations.

## Deploy

From the project root:

```bash
./stock-market-service/deploy.sh
```
