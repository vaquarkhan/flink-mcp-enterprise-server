#!/usr/bin/env bash
# Author: Viquar Khan
# Download-and-run: build jar, start Flink, run every example.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export FLINK_REST_URL="${FLINK_REST_URL:-http://localhost:8081}"
echo "== Flink MCP examples (bash) =="
mvn -q -DskipTests package
docker compose -f examples/docker-compose.yml up -d
python3 examples/run_all.py --skip-build
echo "== done =="
