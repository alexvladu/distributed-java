# Asset management service

This is a minimal Jakarta Servlet WAR for Tomcat 11.

## Run with the Homebrew Tomcat

```bash
cd /home/alex/sisteme-distribuite/stock-market-project/asset-management-service
mvn clean package

export CATALINA_HOME="$(brew --prefix tomcat)/libexec"
export CATALINA_BASE="/home/alex/sisteme-distribuite/servere/tomcat-base"
rm -rf "$CATALINA_BASE/webapps/asset-management-service" "$CATALINA_BASE/webapps/asset-management-service.war"
cp target/asset-management-service.war "$CATALINA_BASE/webapps/"
"$(brew --prefix tomcat)/bin/catalina" run
```

Open <http://localhost:8081/asset-management-service/assets>.

The `/assets` servlet reads the CSV file at `src/main/resources/assets.csv`
through `AssetService` and forwards the data to the JSP at
`src/main/webapp/WEB-INF/views/assets.jsp`. The JSP is under `WEB-INF`, so it
can only be reached through the servlet.

## Deploy

From the project root:

```bash
./asset-management-service/deploy.sh
```