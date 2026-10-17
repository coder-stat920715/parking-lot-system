#!/usr/bin/env bash
# Starts the whole system in the correct order (build first: mvn clean package).
# Usage: ./scripts/start-all.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOGS="$ROOT/logs"
mkdir -p "$LOGS"
: > "$LOGS/pids"

start() {   # start <module> <port>
  local module=$1 port=$2
  local jar
  jar=$(ls "$ROOT/$module"/target/*.jar | head -1)
  nohup java -jar "$jar" --server.port="$port" > "$LOGS/$module-$port.log" 2>&1 &
  echo $! >> "$LOGS/pids"
  echo "  started $module on :$port (pid $!)"
}

wait_for() {   # wait_for <port> <name>
  for _ in $(seq 1 60); do
    if curl -sf "http://localhost:$1/actuator/health" > /dev/null; then echo "  $2 is UP"; return 0; fi
    sleep 2
  done
  echo "  $2 did not become healthy - see $LOGS"; exit 1
}

if command -v redis-cli >/dev/null 2>&1 && ! redis-cli ping >/dev/null 2>&1; then
  echo "WARNING: Redis is not reachable on localhost:6379 (run: docker compose up -d redis)."
  echo "         Allocation falls back to a JVM-local lock and rate limiting fails open."
fi

echo "[1/5] Eureka";                 start eureka-server 8761;            wait_for 8761 eureka-server
echo "[2/5] Spot allocation x2";     start spot-allocation-service 8081;  wait_for 8081 "spot-allocation :8081"
                                     start spot-allocation-service 8091;  wait_for 8091 "spot-allocation :8091"
echo "[3/5] Ticket service";         start parking-ticket-service 8082;   wait_for 8082 parking-ticket-service
echo "[4/5] Payment service";        start payment-service 8083;          wait_for 8083 payment-service
echo "[5/5] API gateway";            start api-gateway 8080;              wait_for 8080 api-gateway

echo "Waiting for Eureka registry propagation..."; sleep 12
echo "System ready. Gateway: http://localhost:8080   Eureka: http://localhost:8761"
