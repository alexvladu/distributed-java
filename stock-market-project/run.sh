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

TOMEE_LOG="/tmp/asset-management-service-tomee.log"

cleanup() {
    trap - TERM INT EXIT
    rm -f "$RUN_PID_FILE"
    [ -n "${TOMEE_PID:-}" ] && kill "$TOMEE_PID" 2>/dev/null || true
    docker compose -f "$COMPOSE_FILE" down
}

trap cleanup TERM INT EXIT

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
        if ! kill -0 "$TOMEE_PID" 2>/dev/null; then
            return 0
        fi
        sleep 2
    done
    echo "$name is not healthy yet; check $url and the logs." >&2
}

docker compose -f "$COMPOSE_FILE" up -d --wait postgres sqlserver oracle
docker compose -f "$COMPOSE_FILE" run --rm --build database-migrations

"$PROJECT_DIR/asset-management-service/deploy.sh"

"$CATALINA_HOME/bin/catalina.sh" run >"$TOMEE_LOG" 2>&1 &
TOMEE_PID=$!

wait_for_health "Asset management service" "http://localhost:$TOMCAT_PORT/health"

echo "Home:        http://localhost:$TOMCAT_PORT/"
echo "Market:      http://localhost:$TOMCAT_PORT/market"
echo "Assets:      http://localhost:$TOMCAT_PORT/assets"
echo "Portfolios:  http://localhost:$TOMCAT_PORT/portfolios"
echo "Health:      http://localhost:$TOMCAT_PORT/health"
echo "PostgreSQL:  localhost:${POSTGRES_PORT:-5432} (database: stock_market)"
echo "SQL Server:  localhost:${MSSQL_PORT:-1433} (database: stock_market)"
echo "OracleDB:    localhost:${ORACLE_PORT:-1521} (service: FREEPDB1)"
echo "Log:         $TOMEE_LOG"
echo "Press Ctrl+C to stop TomEE and the containers."

set +e
wait "$TOMEE_PID"
EXIT_STATUS=$?
set -e

echo "TomEE stopped unexpectedly (exit status $EXIT_STATUS)." >&2
echo "Check $TOMEE_LOG for the failure details." >&2
exit "$EXIT_STATUS"
