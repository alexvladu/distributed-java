# Asset management service

Apache TomEE Plus 10.2 WAR for portfolios, trading, and the web UI. It consumes stock prices from Kafka and
stores data in PostgreSQL (users), SQL Server (portfolios and cash), and Oracle (trade ledger).
See the [project README](../README.md) for the architecture, API, and security model.

## Layout

```text
src/main/java/com/example/asset/
  application/   ApplicationLifecycle (session hardening), AppConfig, DataSourceConfig
  boundary/
    http/        SecurityFilter, ApiServlet (error mapping), Json, SessionSupport, UnauthorizedException
    page/        PageServlet: HTML pages /, /live-market, /assets, /portfolios
    api/         JSON endpoints: /auth/login, /auth/register, /auth/logout, /api/session, /api/users,
                 /api/portfolios, /api/trades, /api/market (+ SSE), /dashboard, /health
    stockevents/ StockPriceConsumer (Kafka in, @Singleton @Startup EJB),
                 MarketStreamHub (Server-Sent Events out, @Singleton EJB)
  control/       AuthControl, UserControl, PortfolioControl, DashboardControl, HealthControl (@Stateless),
                 TradingControl, MarketControl (@Singleton), ...
    repository/  @Local repository interfaces, @Stateless JDBC implementations,
                 Database (@Singleton @Startup, owns the HikariCP pools)
  entity/        records: User, Credential, Portfolio, Trade, TradeSide
src/main/webapp/
  auth.html                      public login and registration page
  static/css/app.css             shared design system (light and dark)
  static/js/app.js               shared helpers; one script per page
  WEB-INF/views/*.jsp            pages, reachable only through PageServlet
  WEB-INF/views/fragments/*.jspf shared head, header, and footer
  WEB-INF/ejb-jar.xml            IllegalArgumentException and SecurityException as application exceptions
```

The TomEE EJB container creates and wires the beans; servlets receive them with `@EJB`. Every bean uses
bean-managed transactions because the HikariCP pools are not enlisted in JTA: each repository call is a local
JDBC transaction, and the controls compensate across databases. `NotFoundException`,
`UnavailableException`, `IllegalArgumentException`, and `SecurityException` are application exceptions, so they
reach `ApiServlet` unwrapped and keep their HTTP status codes.

## Build and deploy

From `stock-market-project`:

```bash
./asset-management-service/deploy.sh
```

This builds the module (and the shared contracts module), then copies the WAR to
`$SERVERS_DIR/tomee-base/webapps/ROOT.war`. Run the unit tests with
`mvn -pl asset-management-service -am verify`.

The service reads its configuration from environment variables; `run.sh` exports the values from
`.env` before starting TomEE.
