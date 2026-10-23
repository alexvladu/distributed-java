#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export CATALINA_HOME="$(brew --prefix tomcat)/libexec"
export CATALINA_BASE="/home/alex/sisteme-distribuite/servere/tomcat-base"
export JETTY_HOME="$(brew --prefix jetty)/libexec"
export JETTY_BASE="/home/alex/sisteme-distribuite/servere/jetty-base"
TOMCAT_LOG="/tmp/asset-management-service-tomcat.log"
JETTY_LOG="/tmp/stock-market-service-jetty.log"

cleanup() {
    trap - TERM INT EXIT
    [ -n "${TOMCAT_PID:-}" ] && kill "$TOMCAT_PID" 2>/dev/null || true
    [ -n "${JETTY_PID:-}" ] && kill "$JETTY_PID" 2>/dev/null || true
}

trap cleanup TERM INT EXIT

"$PROJECT_DIR/asset-management-service/deploy.sh"
"$PROJECT_DIR/stock-market-service/deploy.sh"

"$(brew --prefix tomcat)/bin/catalina" run >"$TOMCAT_LOG" 2>&1 &
TOMCAT_PID=$!

cd "$JETTY_BASE"
java -jar "$JETTY_HOME/start.jar" \
    jetty.http.port=8082 >"$JETTY_LOG" 2>&1 &
JETTY_PID=$!

echo "Tomcat: http://localhost:8081/asset-management-service/assets"
echo "Jetty:  http://localhost:8082/stock-market-service/hello"
echo "Logs:   $TOMCAT_LOG and $JETTY_LOG"
echo "Press Ctrl+C to stop both servers."

wait -n "$TOMCAT_PID" "$JETTY_PID"
