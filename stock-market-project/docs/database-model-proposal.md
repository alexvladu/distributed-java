# Proposed model split across databases

The application should keep each database responsible for a different
business area while connecting records through stable numeric identifiers.
This satisfies the requirement that the tables are different and related
without pretending that a foreign key can span database engines.

## PostgreSQL: users

`users` is the source of identity:

- `user_id` — primary key
- `username` — unique login/display name
- `email` — unique contact address
- `created_at`

User creation and account-level validation belong to the PostgreSQL
repository.

## Microsoft SQL Server: portfolios

`portfolios` owns the user's investment containers:

- `portfolio_id` — primary key
- `user_id` — logical reference to `postgresql.users.user_id`
- `portfolio_name`
- `cash_balance` — the portfolio's cash; a check constraint keeps it non-negative
- `created_at`

The application must verify that `user_id` exists in PostgreSQL before
creating or updating a portfolio. SQL Server is a good fit for the
transaction-oriented portfolio CRUD operations.

## OracleDB: trades

`trades` owns the immutable trading history:

- `trade_id` — primary key
- `portfolio_id` — logical reference to `sqlserver.portfolios.portfolio_id`
- `symbol`
- `side` (`BUY` or `SELL`)
- `quantity`
- `price` — execution price per share (null only for trades recorded before it existed)
- `executed_at`

The application validates the portfolio before inserting a trade. A trade
should normally be append-only; updates and deletes can be implemented for
the academic CRUD requirement with authorization and audit logging.

## Cross-database page and CRUD boundary

The UI can expose:

1. a user CRUD page backed by PostgreSQL;
2. a portfolio CRUD page backed by SQL Server;
3. a trade CRUD page backed by OracleDB;
4. a dashboard that loads the user from PostgreSQL, portfolios from SQL
   Server, and trades from OracleDB, joining the results in the application
   service by `user_id` and `portfolio_id`.

The EJB/JPA layer should use one persistence unit and repository/transaction
boundary per database (or three explicitly configured persistence units).
The Servlet/JSP layer should call an application service that performs the
cross-database read; it should not issue SQL directly. Cross-database writes
should validate in dependency order and report partial failures explicitly.

## Implementation

The asset-management service implements this model with one JDBC repository
and one HikariCP pool per database, called only from the control layer:

- A market order moves cash in SQL Server with a conditional update that
  cannot make the balance negative, then inserts the trade in Oracle. If the
  insert fails, the cash movement is reversed (a compensating action).
- Trade corrections and voids settle the cash difference at the original
  price and are refused if a later sale would exceed the shares held.
- Deleting a portfolio removes the SQL Server row first and then its Oracle
  trades; deleting a user removes portfolios and trades before the
  PostgreSQL identity.
- Holdings and cost basis are derived from the trade ledger.
