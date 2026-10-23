#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export JETTY_HOME="$(brew --prefix jetty)/libexec"
export JETTY_BASE="/home/alex/sisteme-distribuite/servere/jetty-base"

cd "$PROJECT_DIR"
mvn clean package

mkdir -p "$JETTY_BASE"
if [ ! -f "$JETTY_BASE/start.d/ee10-deploy.ini" ]; then
    cd "$JETTY_BASE"
    java -jar "$JETTY_HOME/start.jar" --add-modules=http,ee10-deploy
fi

rm -rf "$JETTY_BASE/webapps/ROOT" \
       "$JETTY_BASE/webapps/ROOT.war"
cp target/stock-market-service-1.0-SNAPSHOT.war \
   "$JETTY_BASE/webapps/ROOT.war"

echo "Deployed stock-market-service to $JETTY_BASE/webapps"
