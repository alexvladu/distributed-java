# Asset management service

This is a minimal Jakarta Servlet WAR for Tomcat 11.

## Run with the Homebrew Tomcat

```bash
cd /home/alex/sisteme-distribuite/stock-market-project/asset-management-service
mvn clean package

export CATALINA_HOME="$(brew --prefix tomcat)/libexec"
export CATALINA_BASE="/home/alex/sisteme-distribuite/servere/tomcat-base"
rm -rf "$CATALINA_BASE/webapps/ROOT" "$CATALINA_BASE/webapps/ROOT.war"
cp target/asset-management-service.war "$CATALINA_BASE/webapps/ROOT.war"
"$(brew --prefix tomcat)/bin/catalina" run
```

Open the home page at <http://localhost:8081/>.

From there, open <http://localhost:8081/assets> to view available assets or
<http://localhost:8081/live-market> to view the live market and manage the
portfolio.

Open the live market at
<http://localhost:8081/live-market>. It displays the
latest stock prices received from Kafka and refreshes every five seconds.
The page receives price changes through a live Server-Sent Events connection,
so it updates when the asset-management service consumes a Kafka event.

The live market starts with a $1,000.00 USD cash balance and no holdings.
Search the Kafka-received market catalog and buy fractional shares. The
Holdings can also be sold in full or in fractional quantities at the latest
server-side Kafka price. The account is persisted in
`asset-management-service/src/main/resources/data/account.json`. Override the
location with
`ACCOUNT_DATA_FILE`).

The deploy script removes legacy context deployments and cached JSP output so
Tomcat serves the current root deployment after application changes.

The `/assets` servlet forwards the live portfolio data received from Kafka to
the JSP at `src/main/webapp/WEB-INF/views/assets.jsp`. The JSP is under
`WEB-INF`, so it can only be reached through the servlet.

## Deploy

From the project root:

```bash
./asset-management-service/deploy.sh
```