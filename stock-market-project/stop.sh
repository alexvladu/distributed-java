#!/usr/bin/env bash
set -euo pipefail

ports=(8081 8082)
found_process=false

for port in "${ports[@]}"; do
    while read -r pid; do
        if [ -z "$pid" ]; then
            continue
        fi
        found_process=true
        echo "Stopping process $pid listening on port $port"
        kill "$pid" 2>/dev/null || true
    done < <(lsof -t -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | sort -u)
done

if [ "$found_process" = false ]; then
    echo "No project process is listening on ports 8081 or 8082."
fi

for port in "${ports[@]}"; do
    for attempt in {1..10}; do
        if ! lsof -t -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
            break
        fi
        sleep 1
    done
done

if lsof -t -iTCP:8081 -sTCP:LISTEN >/dev/null 2>&1 ||
   lsof -t -iTCP:8082 -sTCP:LISTEN >/dev/null 2>&1; then
    echo "A process did not stop cleanly; inspect the listening ports with lsof." >&2
    exit 1
fi

echo "Ports 8081 and 8082 are free."
