#!/usr/bin/env bash
set -euo pipefail

# shellcheck source=../scripts/common.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/scripts/common.sh"

if [ "${SKIP_BUILD:-0}" != "1" ]; then
    build_services stock-market-service
fi
ensure_jetty_base

rm -rf "$JETTY_BASE/webapps/ROOT" \
       "$JETTY_BASE/webapps/ROOT.war" \
       "$JETTY_BASE/webapps/stock-market-service" \
       "$JETTY_BASE/webapps/stock-market-service.war"
cp "$PROJECT_DIR/stock-market-service/target/stock-market-service.war" \
   "$JETTY_BASE/webapps/ROOT.war"

echo "Deployed stock-market-service to $JETTY_BASE/webapps"
