#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$PROJECT_DIR/docker-compose.yml"
RUN_LOCK_FILE="/tmp/stock-market-project.run.lock"
exec 9>"$RUN_LOCK_FILE"
if ! flock -n 9; then
    echo "Another run.sh instance is already running." >&2
    echo "Stop it with Ctrl+C in its terminal before starting another one." >&2
    exit 1
fi

export CATALINA_HOME="$(brew --prefix tomcat)/libexec"
export CATALINA_BASE="/home/alex/sisteme-distribuite/servere/tomcat-base"
export JETTY_HOME="$(brew --prefix jetty)/libexec"
export JETTY_BASE="/home/alex/sisteme-distribuite/servere/jetty-base"
export ACCOUNT_DATA_FILE="$PROJECT_DIR/asset-management-service/src/main/resources/data/account.json"
TOMCAT_LOG="/tmp/asset-management-service-tomcat.log"
JETTY_LOG="/tmp/stock-market-service-jetty.log"

cleanup() {
    trap - TERM INT EXIT
    [ -n "${TOMCAT_PID:-}" ] && kill "$TOMCAT_PID" 2>/dev/null || true
    [ -n "${JETTY_PID:-}" ] && kill "$JETTY_PID" 2>/dev/null || true
    docker compose -f "$COMPOSE_FILE" down
}

trap cleanup TERM INT EXIT

docker compose -f "$COMPOSE_FILE" up -d --wait kafka postgres sqlserver oracle
docker compose -f "$COMPOSE_FILE" run --rm --build database-migrations
for attempt in {1..30}; do
    if docker compose -f "$COMPOSE_FILE" exec -T kafka /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 --list >/dev/null 2>&1; then
        break
    fi
    if [ "$attempt" -eq 30 ]; then
        echo "Kafka did not become ready" >&2
        exit 1
    fi
    sleep 1
done

"$PROJECT_DIR/asset-management-service/deploy.sh"
"$PROJECT_DIR/stock-market-service/deploy.sh"

"$(brew --prefix tomcat)/bin/catalina" run >"$TOMCAT_LOG" 2>&1 &
TOMCAT_PID=$!

cd "$JETTY_BASE"
java -jar "$JETTY_HOME/start.jar" \
    jetty.http.port=8082 >"$JETTY_LOG" 2>&1 &
JETTY_PID=$!

echo "Tomcat: http://localhost:8081/assets"
echo "Home:   http://localhost:8081/"
echo "Jetty:  stock-market producer running on localhost:8082"
echo "Kafka:  localhost:9092 (topic: stock-prices)"
echo "PostgreSQL: localhost:5432 (database: stock_market)"
echo "SQL Server:  localhost:1433 (database: stock_market)"
echo "OracleDB:    localhost:1521 (service: FREEPDB1)"
echo "Live market: http://localhost:8081/live-market"
echo "Logs:   $TOMCAT_LOG and $JETTY_LOG"
echo "Press Ctrl+C to stop both servers and Kafka."

set +e
wait -n -p EXITED_PID "$TOMCAT_PID" "$JETTY_PID"
EXIT_STATUS=$?
set -e

if [ "$EXITED_PID" = "$TOMCAT_PID" ]; then
    EXITED_SERVICE="Tomcat"
    EXITED_LOG="$TOMCAT_LOG"
else
    EXITED_SERVICE="Jetty"
    EXITED_LOG="$JETTY_LOG"
fi

echo "$EXITED_SERVICE stopped unexpectedly (exit status $EXIT_STATUS)." >&2
echo "Check $EXITED_LOG for the failure details." >&2
exit "$EXIT_STATUS"
