# Asset management service

Apache TomEE Plus 10.2 WAR for users, portfolios, trading, and the web UI. It stores data in PostgreSQL
(users), SQL Server (portfolios and cash), and Oracle (trade ledger).
See the [project README](../README.md) for the architecture, API, and security model.

## Layout

```text
src/main/java/com/example/asset/
  application/   ApplicationLifecycle (session hardening), AppConfig (environment)
  boundary/
    http/        SecurityFilter, ApiServlet (error mapping), Json, SessionSupport, UnauthorizedException
    page/        PageServlet: HTML pages /, /market, /assets, /portfolios
    api/         JSON endpoints: /auth/login, /auth/register, /auth/logout, /api/session, /api/users,
                 /api/portfolios, /api/trades, /api/market, /dashboard, /health
  control/       AuthControl, UserControl, PortfolioControl, DashboardControl, HealthControl (@Stateless),
                 TradingControl, MarketControl (@Singleton, fixed stock prices), ...
    repository/  @Local repository interfaces, @Stateless JPA implementations (one EntityManager each),
                 Database (@Singleton @Startup: configuration check and database health)
  entity/        JPA entities (UserEntity, CredentialEntity, PortfolioEntity, TradeEntity) and
                 records (User, Credential, Portfolio, Trade, TradeSide)
src/main/resources/
  META-INF/persistence.xml       persistence units postgres, sqlserver, oracle (OpenJPA)
src/main/webapp/
  auth.html                      public login and registration page
  static/css/app.css             shared design system (light and dark)
  static/js/app.js               shared helpers; one script per page
  WEB-INF/views/*.jsp            pages, reachable only through PageServlet
  WEB-INF/views/fragments/*.jspf shared head, header, and footer
  WEB-INF/resources.xml          JTA data sources, configured from environment variables
  WEB-INF/ejb-jar.xml            IllegalArgumentException and SecurityException as application exceptions
```

The TomEE EJB container creates and wires the beans; servlets receive them with `@EJB`, and repositories
receive their `EntityManager` with `@PersistenceContext`. The controls use bean-managed transactions and never
start one, so every repository call runs in its own container-managed JTA transaction on a single database,
and the controls compensate across databases. `NotFoundException`, `UnavailableException`,
`IllegalArgumentException`, and `SecurityException` are application exceptions, so they reach `ApiServlet`
unwrapped and keep their HTTP status codes; an `IllegalArgumentException` from a repository (for example a
duplicate username) also rolls its transaction back.

## Build and deploy

From `stock-market-project`:

```bash
./asset-management-service/deploy.sh
```

This builds the module, then copies the WAR to
`$SERVERS_DIR/tomee-base/webapps/ROOT.war`. Run the unit tests with
`mvn -pl asset-management-service verify`.

The service reads its configuration from environment variables; `run.sh` exports the values from
`.env` before starting TomEE. The data sources in `WEB-INF/resources.xml` resolve them with
`${NAME:-default}` placeholders.
