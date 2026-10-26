#!/usr/bin/env bash
set -euo pipefail

# shellcheck source=../scripts/common.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/scripts/common.sh"

if [ "${SKIP_BUILD:-0}" != "1" ]; then
    build_services asset-management-service
fi
ensure_tomcat_base

rm -rf "$CATALINA_BASE/webapps/ROOT" \
       "$CATALINA_BASE/webapps/ROOT.war" \
       "$CATALINA_BASE/webapps/asset-management-service" \
       "$CATALINA_BASE/webapps/asset-management-service.war" \
       "$CATALINA_BASE/work/Catalina/localhost/ROOT" \
       "$CATALINA_BASE/work/Catalina/localhost/asset-management-service"
cp "$PROJECT_DIR/asset-management-service/target/asset-management-service.war" \
   "$CATALINA_BASE/webapps/ROOT.war"

echo "Deployed asset-management-service to $CATALINA_BASE/webapps"
