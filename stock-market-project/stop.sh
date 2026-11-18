#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
compose_file="$project_dir/docker-compose.yml"
port=8081
found_process=false
launcher_stopped=false
run_pid_file="/tmp/stock-market-project.run.pid"

if [ -f "$run_pid_file" ]; then
    run_pid="$(cat "$run_pid_file")"
    if [[ "$run_pid" =~ ^[0-9]+$ ]] &&
       [ -d "/proc/$run_pid" ] &&
       [ "$(readlink "/proc/$run_pid/cwd" 2>/dev/null || true)" = "$project_dir" ] &&
       tr '\0' ' ' <"/proc/$run_pid/cmdline" 2>/dev/null | grep -Eq '(^|/)(bash|run\.sh)( |$)'; then
        echo "Stopping run.sh process $run_pid"
        kill -TERM "$run_pid" 2>/dev/null || true
        for attempt in {1..15}; do
            if [ ! -d "/proc/$run_pid" ]; then
                break
            fi
            sleep 1
        done
        if [ -d "/proc/$run_pid" ]; then
            echo "run.sh did not stop after 15 seconds; terminating process $run_pid" >&2
            kill -KILL "$run_pid" 2>/dev/null || true
        fi
        launcher_stopped=true
    fi
    rm -f "$run_pid_file"
fi

if [ "$launcher_stopped" = true ]; then
    docker compose -f "$compose_file" down
fi

while read -r pid; do
    if [ -z "$pid" ]; then
        continue
    fi
    found_process=true
    echo "Stopping process $pid listening on port $port"
    kill "$pid" 2>/dev/null || true
done < <(lsof -t -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | sort -u)

if [ "$found_process" = false ]; then
    echo "No project process is listening on port $port."
fi

for attempt in {1..10}; do
    if ! lsof -t -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
        break
    fi
    sleep 1
done

if lsof -t -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "A process did not stop cleanly; inspect port $port with lsof." >&2
    exit 1
fi

echo "Port $port is free."
