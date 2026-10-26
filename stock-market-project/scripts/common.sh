#!/usr/bin/env bash
# Shared settings for run.sh and the deploy scripts. Source this file; do not execute it.

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_DIR="$(cd "$PROJECT_DIR/.." && pwd)"
COMPOSE_FILE="$PROJECT_DIR/docker-compose.yml"
ENV_FILE="$PROJECT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
    cp "$PROJECT_DIR/.env.example" "$ENV_FILE"
    echo "Created $ENV_FILE from .env.example with development defaults." >&2
fi
set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

# The Tomcat and Jetty base directories default to <repo>/servere (ignored by git).
SERVERS_DIR="${SERVERS_DIR:-$REPO_DIR/servere}"
TOMCAT_PORT="${TOMCAT_PORT:-8081}"
JETTY_PORT="${JETTY_PORT:-8082}"
export CATALINA_HOME="$(brew --prefix tomcat)/libexec"
export CATALINA_BASE="$SERVERS_DIR/tomcat-base"
export JETTY_HOME="$(brew --prefix jetty)/libexec"
export JETTY_BASE="$SERVERS_DIR/jetty-base"

build_services() {
    mvn -B -ntp -f "$PROJECT_DIR/pom.xml" -pl "$1" -am clean package -DskipTests
}

ensure_tomcat_base() {
    if [ -f "$CATALINA_BASE/conf/server.xml" ]; then
        return
    fi
    if [ ! -f "$CATALINA_HOME/conf/server.xml" ]; then
        echo "Cannot create $CATALINA_BASE: $CATALINA_HOME/conf/server.xml was not found." >&2
        echo "Create a Tomcat base listening on port $TOMCAT_PORT there, or set SERVERS_DIR." >&2
        exit 1
    fi
    echo "Creating Tomcat base $CATALINA_BASE (HTTP port $TOMCAT_PORT)"
    mkdir -p "$CATALINA_BASE/conf" "$CATALINA_BASE/logs" "$CATALINA_BASE/temp" \
             "$CATALINA_BASE/webapps" "$CATALINA_BASE/work"
    cp -R "$CATALINA_HOME/conf/." "$CATALINA_BASE/conf/"
    # HTTP on $TOMCAT_PORT; the shutdown port is disabled because run.sh stops Tomcat by PID.
    sed -e "s/port=\"8080\"/port=\"$TOMCAT_PORT\"/" -e 's/port="8005"/port="-1"/' \
        "$CATALINA_HOME/conf/server.xml" >"$CATALINA_BASE/conf/server.xml"
}

ensure_jetty_base() {
    if [ -f "$JETTY_BASE/start.d/ee10-deploy.ini" ]; then
        return
    fi
    echo "Creating Jetty base $JETTY_BASE"
    mkdir -p "$JETTY_BASE"
    (cd "$JETTY_BASE" && java -jar "$JETTY_HOME/start.jar" --add-modules=http,ee10-deploy)
}
