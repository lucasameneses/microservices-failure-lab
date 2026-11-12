#!/usr/bin/env bash
# Failure lab control script. Run ./lab.sh without arguments for usage.
set -euo pipefail

cd "$(dirname "$0")"

SERVICE_A=http://localhost:8080
SERVICE_B=http://localhost:8081
COMPOSE=(docker compose --profile load)

usage() {
  cat <<'EOF'
Usage: ./lab.sh <command>

  up              build and start the lab with background load
  down            stop and remove the lab
  status          show active scenarios and tc rules
  latency [ms]    delay every service-b /process call (default 2000 ms)
  cycle           make /flow enter the A -> B -> A call cycle
  resources       exhaust CPU and heap in service-a until a real OutOfMemoryError
  network-tc      add delay and packet loss to service-b with tc netem
  network-down    stop service-b
  reset           undo every scenario without recreating the lab
EOF
}

post() {
  curl -sS -X POST -H 'Content-Type: application/json' -d "${2:-{\}}" "$1"
  echo
}

wait_healthy() {
  local url=$1
  until curl -sf "$url/actuator/health" > /dev/null; do sleep 2; done
}

up() {
  "${COMPOSE[@]}" up -d --build --wait --wait-timeout 300
  cat <<'EOF'
Lab is up.
  Grafana     http://localhost:3000  (dashboard "Failure Lab")
  Jaeger      http://localhost:16686
  Prometheus  http://localhost:9090
  service-a   http://localhost:8080/flow
EOF
}

status() {
  curl -sS "$SERVICE_A/chaos" || echo "service-a unreachable"
  echo
  curl -sS "$SERVICE_B/chaos" || echo "service-b unreachable"
  echo
  "${COMPOSE[@]}" exec -T service-b tc qdisc show dev eth0 || true
}

reset() {
  "${COMPOSE[@]}" start service-b
  wait_healthy "$SERVICE_B"
  "${COMPOSE[@]}" exec -T service-b tc qdisc del dev eth0 root 2> /dev/null || true
  curl -sS -X DELETE "$SERVICE_A/chaos"
  echo
  curl -sS -X DELETE "$SERVICE_B/chaos"
  echo
}

case "${1:-}" in
  up) up ;;
  down) "${COMPOSE[@]}" down ;;
  status) status ;;
  latency) post "$SERVICE_B/chaos/latency" "{\"delayMs\": ${2:-2000}}" ;;
  cycle) post "$SERVICE_A/chaos/cycle" ;;
  resources) post "$SERVICE_A/chaos/resource-exhaustion" ;;
  network-tc) "${COMPOSE[@]}" exec -T service-b tc qdisc add dev eth0 root netem delay 800ms 200ms loss 20% ;;
  network-down) "${COMPOSE[@]}" stop service-b ;;
  reset) reset ;;
  *) usage; exit 1 ;;
esac
