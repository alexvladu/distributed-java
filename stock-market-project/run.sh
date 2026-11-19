#!/usr/bin/env bash
set -euo pipefail

# shellcheck source=scripts/common.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/scripts/common.sh"

TOMEE_LOG="/tmp/asset-management-service-tomee.log"

cleanup() {
    trap - TERM INT EXIT
    [ -n "${TOMEE_PID:-}" ] && kill "$TOMEE_PID" 2>/dev/null || true
    docker compose -f "$COMPOSE_FILE" down
}

trap cleanup TERM INT EXIT

docker compose -f "$COMPOSE_FILE" up -d --wait postgres sqlserver oracle
docker compose -f "$COMPOSE_FILE" run --rm --build database-migrations

"$PROJECT_DIR/asset-management-service/deploy.sh"

"$CATALINA_HOME/bin/catalina.sh" run >"$TOMEE_LOG" 2>&1 &
TOMEE_PID=$!

echo "TomEE is starting; the pages are available once the WAR is deployed (see the log)."
echo "Home:        http://localhost:$TOMCAT_PORT/"
echo "Market:      http://localhost:$TOMCAT_PORT/market"
echo "Assets:      http://localhost:$TOMCAT_PORT/assets"
echo "Portfolios:  http://localhost:$TOMCAT_PORT/portfolios"
echo "PostgreSQL:  localhost:${POSTGRES_PORT:-5432} (database: stock_market)"
echo "SQL Server:  localhost:${MSSQL_PORT:-1433} (database: stock_market)"
echo "OracleDB:    localhost:${ORACLE_PORT:-1521} (service: FREEPDB1)"
echo "Log:         $TOMEE_LOG"
echo "Press Ctrl+C to stop TomEE and the containers."

set +e
wait "$TOMEE_PID"
EXIT_STATUS=$?
set -e

echo "TomEE stopped (exit status $EXIT_STATUS); see $TOMEE_LOG." >&2
exit "$EXIT_STATUS"
