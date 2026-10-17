#!/usr/bin/env bash
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [ -f "$ROOT/logs/pids" ]; then
  while read -r pid; do kill "$pid" 2>/dev/null && echo "stopped $pid"; done < "$ROOT/logs/pids"
  : > "$ROOT/logs/pids"
fi
