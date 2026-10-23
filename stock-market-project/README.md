# Plain Tomcat and Jetty projects

This folder contains two independent Java web projects:

- `asset-management-service`: a Jakarta Servlet WAR deployed to Tomcat 11
- `stock-market-service`: a Jakarta Servlet WAR deployed to Jetty 12

## Run locally with Homebrew

Build, deploy, and start both servers from one terminal:

```bash
./run.sh
```

The endpoints are:

- Tomcat: <http://localhost:8081/asset-management-service/hello>
- Jetty: <http://localhost:8082/stock-market-service/hello>

`run.sh` stops both servers when you press `Ctrl+C`.
