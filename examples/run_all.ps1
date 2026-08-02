# Author: Viquar Khan
# Download-and-run on Windows: build jar, start Flink, run every example.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
$env:FLINK_REST_URL = if ($env:FLINK_REST_URL) { $env:FLINK_REST_URL } else { "http://localhost:8081" }
Write-Host "== Flink MCP examples (PowerShell) =="
mvn -q -DskipTests package
docker compose -f examples/docker-compose.yml up -d
python examples/run_all.py --skip-build
Write-Host "== done =="
