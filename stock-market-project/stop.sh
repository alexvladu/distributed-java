#!/usr/bin/env bash
set -euo pipefail

# Stops the containers first, then TomEE; a run.sh still open in another terminal then exits on its own.
cd "$(dirname "${BASH_SOURCE[0]}")"
docker compose down
lsof -t -iTCP:8081 -sTCP:LISTEN 2>/dev/null | xargs -r kill || true
