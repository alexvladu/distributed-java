# Asset management service

Tomcat 11 WAR for portfolios, trading, and the web UI. It consumes stock prices from Kafka and
stores data in PostgreSQL (users), SQL Server (portfolios and cash), and Oracle (trade ledger).
See the [project README](../README.md) for the architecture, API, and security model.

## Layout

```text
src/main/java/com/example/asset/
  application/   ApplicationContext (wiring), ApplicationLifecycle (startup/shutdown), AppConfig
  boundary/      SecurityFilter, PageServlet, HealthServlet, DashboardServlet
    api/         /api/session, /api/users, /api/portfolios, /api/trades, /api/market (+ SSE)
    auth/        /auth/login, /auth/register, /auth/logout
    http/        Json, ApiServlet (error mapping), SessionSupport
    messaging/   StockPriceConsumer (Kafka)
    stream/      MarketStreamHub (Server-Sent Events fan-out)
  control/       AuthControl, UserControl, PortfolioControl, TradingControl, MarketControl, ...
  entity/        records and JDBC repositories; database/Database owns the HikariCP pools
src/main/webapp/
  auth.html                      public login and registration page
  static/css/app.css             shared design system (light and dark)
  static/js/app.js               shared helpers; one script per page
  WEB-INF/views/*.jsp            pages, reachable only through PageServlet
  WEB-INF/views/fragments/*.jspf shared head, header, and footer
```

## Build and deploy

From `stock-market-project`:

```bash
./asset-management-service/deploy.sh
```

This builds the module (and the shared contracts module), then copies the WAR to
`$SERVERS_DIR/tomcat-base/webapps/ROOT.war`. Run the unit tests with
`mvn -pl asset-management-service -am verify`.

The service reads its configuration from environment variables; `run.sh` exports the values from
`.env` before starting Tomcat.
