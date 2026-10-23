# Plain Tomcat, Jetty, and Kafka projects

This folder contains two independent Java web projects:

- `asset-management-service`: a Jakarta Servlet WAR deployed to Tomcat 11
- `stock-market-service`: a Jakarta Servlet WAR deployed to Jetty 12
- Kafka runs in Docker Compose and carries `StockCompany` price updates on `stock-prices`

## Run locally with Homebrew and Docker

Build, deploy, and start both servers from one terminal:

```bash
./run.sh
```

Docker must be running. `run.sh` starts Kafka, waits until its broker is
ready, builds and deploys both services, and stops Kafka and both servers on
`Ctrl+C`.

The endpoints are:

- Tomcat: <http://localhost:8081/asset-management-service/hello>
- Live market: <http://localhost:8081/live-market>
- Jetty: stock-market producer on <http://localhost:8082>
- Stock-market service: producer-only on <http://localhost:8082>
The stock-market service starts with a base list of stocks, publishes the
complete list to `stock-prices`, then updates one stock every 5–15 seconds with
a small random price movement and publishes each updated DTO. The
asset-management service consumes the events and logs the received updates.

`run.sh` stops both servers when you press `Ctrl+C`.
