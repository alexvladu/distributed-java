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

## Deploy

From the project root:

```bash
./stock-market-service/deploy.sh
```
