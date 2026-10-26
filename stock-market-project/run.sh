#!/usr/bin/env bash
set -euo pipefail

# shellcheck source=scripts/common.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/scripts/common.sh"
# stop.sh identifies this launcher by its working directory.
cd "$PROJECT_DIR"

RUN_LOCK_FILE="/tmp/stock-market-project.run.lock"
RUN_PID_FILE="/tmp/stock-market-project.run.pid"
exec 9>"$RUN_LOCK_FILE"
if ! flock -n 9; then
    echo "Another run.sh instance is already running." >&2
    echo "Stop it with Ctrl+C in its terminal before starting another one." >&2
    exit 1
fi
printf '%s\n' "$$" >"$RUN_PID_FILE"

TOMCAT_LOG="/tmp/asset-management-service-tomcat.log"
JETTY_LOG="/tmp/stock-market-service-jetty.log"
KAFKA_TOPIC="${KAFKA_TOPIC:-stock-prices}"

cleanup() {
    trap - TERM INT EXIT
    rm -f "$RUN_PID_FILE"
    [ -n "${TOMCAT_PID:-}" ] && kill "$TOMCAT_PID" 2>/dev/null || true
    [ -n "${JETTY_PID:-}" ] && kill "$JETTY_PID" 2>/dev/null || true
    docker compose -f "$COMPOSE_FILE" down
}

trap cleanup TERM INT EXIT

kafka_topics() {
    docker compose -f "$COMPOSE_FILE" exec -T kafka /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 "$@"
}

wait_for_health() {
    local name="$1" url="$2"
    if ! command -v curl >/dev/null 2>&1; then
        return 0
    fi
    for attempt in {1..60}; do
        if curl -fsS "$url" >/dev/null 2>&1; then
            echo "$name is healthy: $url"
            return 0
        fi
        if ! kill -0 "$TOMCAT_PID" 2>/dev/null || ! kill -0 "$JETTY_PID" 2>/dev/null; then
            return 0
        fi
        sleep 2
    done
    echo "$name is not healthy yet; check $url and the logs." >&2
}

docker compose -f "$COMPOSE_FILE" up -d --wait kafka postgres sqlserver oracle
docker compose -f "$COMPOSE_FILE" run --rm --build database-migrations
for attempt in {1..30}; do
    if kafka_topics --list >/dev/null 2>&1; then
        break
    fi
    if [ "$attempt" -eq 30 ]; then
        echo "Kafka did not become ready" >&2
        exit 1
    fi
    sleep 1
done
# Compaction keeps the latest price per symbol, so consumers can rebuild the market from the topic.
kafka_topics --create --if-not-exists --topic "$KAFKA_TOPIC" --partitions 1 --replication-factor 1 \
    --config cleanup.policy=compact >/dev/null

build_services asset-management-service,stock-market-service
SKIP_BUILD=1 "$PROJECT_DIR/asset-management-service/deploy.sh"
SKIP_BUILD=1 "$PROJECT_DIR/stock-market-service/deploy.sh"

"$(brew --prefix tomcat)/bin/catalina" run >"$TOMCAT_LOG" 2>&1 &
TOMCAT_PID=$!

(cd "$JETTY_BASE" && exec java -jar "$JETTY_HOME/start.jar" jetty.http.port="$JETTY_PORT") >"$JETTY_LOG" 2>&1 &
JETTY_PID=$!

wait_for_health "Stock market service" "http://localhost:$JETTY_PORT/health"
wait_for_health "Asset management service" "http://localhost:$TOMCAT_PORT/health"

echo "Home:        http://localhost:$TOMCAT_PORT/"
echo "Live market: http://localhost:$TOMCAT_PORT/live-market"
echo "Assets:      http://localhost:$TOMCAT_PORT/assets"
echo "Portfolios:  http://localhost:$TOMCAT_PORT/portfolios"
echo "Health:      http://localhost:$TOMCAT_PORT/health and http://localhost:$JETTY_PORT/health"
echo "Kafka:       localhost:9092 (topic: $KAFKA_TOPIC)"
echo "PostgreSQL:  localhost:${POSTGRES_PORT:-5432} (database: stock_market)"
echo "SQL Server:  localhost:${MSSQL_PORT:-1433} (database: stock_market)"
echo "OracleDB:    localhost:${ORACLE_PORT:-1521} (service: FREEPDB1)"
echo "Logs:        $TOMCAT_LOG and $JETTY_LOG"
echo "Press Ctrl+C to stop both servers and the containers."

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
