# Plain Tomcat, Jetty, Kafka, and three-database project

This folder contains two independent Java web projects:

- `asset-management-service`: a Jakarta Servlet WAR deployed to Tomcat 11
- `stock-market-service`: a Jakarta Servlet WAR deployed to Jetty 12
- Kafka, PostgreSQL, Microsoft SQL Server, and OracleDB run in Docker Compose.
  Kafka carries `StockCompany` price updates on `stock-prices`.

## Run locally with Homebrew and Docker

Build, deploy, and start both servers from one terminal:

```bash
./run.sh
```

Docker must be running. `run.sh` starts Kafka, waits until its broker is
ready, starts all three databases, builds and deploys both services, and stops
the containers and both servers on `Ctrl+C`. Copy `.env.example` to `.env` and
replace the development passwords before exposing the ports outside localhost.
All Docker services belong to the explicitly named Compose project
`stock-market`, including the persistent volumes and the
`database-migrations` service.

The endpoints are:

- Home: <http://localhost:8081/>
- Assets: <http://localhost:8081/assets>
- Live market: <http://localhost:8081/live-market>
- Jetty: stock-market producer on <http://localhost:8082>

The stock-market service starts with a base list of stocks, publishes the
complete list to `stock-prices`, then updates one stock every 5–15 seconds with
a small random price movement and publishes each updated DTO. The
asset-management service consumes the events and logs the received updates.

Open <http://localhost:8081/live-market> to search received stocks, view the
live USD account balance, and buy fractional shares. New accounts start with
$1,000.00 and no holdings; account state is stored locally in
`asset-management-service/src/main/resources/data/account.json`.

`run.sh` stops both servers when you press `Ctrl+C`.

## Database layout

The `database-migrations` Maven module applies versioned Flyway migrations
after the database containers become healthy:

- PostgreSQL: `users` is the identity/source table.
- SQL Server: `portfolios` stores portfolios and references `users.user_id`
  through the logical `user_id` value.
- OracleDB: `trades` stores executions and references
  `portfolios.portfolio_id` through the logical `portfolio_id` value.

The IDs are deliberately logical cross-database references. A foreign key
cannot be enforced by PostgreSQL, SQL Server, or Oracle across separate
database engines, so the application service must validate the referenced
record before writes and use compensating/error handling when a referenced
record is missing. The deployment workflow is
`.github/workflows/deploy.yml`.

The asset-management service now exposes the BCE database API:

```text
GET/POST        /api/users
GET/PUT/DELETE  /api/users/{id}
GET/POST        /api/portfolios
GET/PUT/DELETE  /api/portfolios/{id}
GET/POST        /api/trades
GET/PUT/DELETE  /api/trades/{id}
GET             /dashboard
```

The boundaries call Control classes, Controls coordinate repository
implementations, and repositories access PostgreSQL, SQL Server, and OracleDB.
Authentication is available at `/auth.html`; registration creates the
PostgreSQL user and credentials, and `/api/*` plus `/dashboard` require an
authenticated session. The authenticated user is taken from the server-side
session rather than from a client-provided `userId`.

To reset the databases and replay all migrations:

```bash
docker compose down -v
docker compose up -d --wait kafka postgres sqlserver oracle
docker compose run --rm --build database-migrations
```

To stop the launcher, its Docker Compose services, and any remaining processes
on the application ports from another terminal, run:

```bash
./stop.sh
```
