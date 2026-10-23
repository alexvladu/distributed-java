#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CATALINA_HOME="$(brew --prefix tomcat)/libexec"
CATALINA_BASE="/home/alex/sisteme-distribuite/servere/tomcat-base"

cd "$PROJECT_DIR"
mvn clean package

rm -rf "$CATALINA_BASE/webapps/asset-management-service" \
       "$CATALINA_BASE/webapps/asset-management-service.war"
cp target/asset-management-service.war \
   "$CATALINA_BASE/webapps/asset-management-service.war"

echo "Deployed asset-management-service to $CATALINA_BASE/webapps"
