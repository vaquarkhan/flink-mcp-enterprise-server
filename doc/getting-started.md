# Getting started

Author: Viquar Khan

You'll have the shaded jar running against a real Flink JobManager in under ten minutes.

## Prerequisites
- JDK 17+ and Maven 3.9+
- A Flink cluster with REST enabled (default `http://localhost:8081`)
- Optional: SQL Gateway on `http://localhost:8083` for `run_sql_readonly`

### Spin up Flink with Docker
```bash
docker network create flink-net
docker run -d --name flink-jobmanager --network flink-net -p 8081:8081 \
  -e JOB_MANAGER_RPC_ADDRESS=flink-jobmanager flink:1.20-java17 jobmanager
docker run -d --name flink-taskmanager --network flink-net \
  -e JOB_MANAGER_RPC_ADDRESS=flink-jobmanager flink:1.20-java17 taskmanager

curl -s http://localhost:8081/overview
```

## Build
```bash
mvn clean package
# Artifacts:
#   target/flink-mcp-server-0.2.0.jar
#   target/flink-mcp-server-0.2.0-all.jar   ← runnable
```

## Run (stdio — Cursor / Claude Desktop)
```bash
FLINK_REST_URL=http://localhost:8081 \
MCP_FLINK_LOG_LEVEL=INFO \
java -jar target/flink-mcp-server-0.2.0-all.jar
```

- **stdout** = MCP JSON-RPC only  
- **stderr** = logs with `trace=<id>`

### Cursor MCP config
```json
{
  "mcpServers": {
    "flink": {
      "command": "java",
      "args": ["-jar", "/ABS/PATH/flink-mcp-server-0.2.0-all.jar"],
      "env": {
        "FLINK_REST_URL": "http://localhost:8081",
        "MCP_FLINK_GATEWAY_URL": "http://localhost:8083",
        "MCP_FLINK_LOG_LEVEL": "INFO"
      }
    }
  }
}
```

## Run (HTTP)
```bash
MCP_FLINK_TRANSPORT=http \
MCP_FLINK_HTTP_BEARER_TOKEN=change-me \
MCP_FLINK_HTTP_HOST=127.0.0.1 \
MCP_FLINK_HTTP_PORT=8090 \
FLINK_REST_URL=http://localhost:8081 \
java -jar target/flink-mcp-server-0.2.0-all.jar
```

| Endpoint | Purpose |
|----------|---------|
| `/mcp` | Streamable MCP (Bearer required) |
| `/healthz` | Liveness |
| `/readyz` | Readiness (pings Flink) |
| `/metrics` | Prometheus scrape |

## Smoke: initialize + tools/list
From another terminal (PowerShell-friendly newline JSON):

```bash
printf '%s\n' \
  '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"smoke","version":"0.1"}}}' \
  '{"jsonrpc":"2.0","id":2,"method":"notifications/initialized"}' \
  '{"jsonrpc":"2.0","id":3,"method":"tools/list"}' \
| FLINK_REST_URL=http://localhost:8081 java -jar target/flink-mcp-server-0.2.0-all.jar
```

Default profile exposes **12 read tools** (cluster, jobs, jars, SQL readonly). No mutate/destructive tools until writes are unlocked.

## Next
- [Developer tutorial](tutorial.md)  
- [Examples](../examples/README.md)  
- [Configuration](configuration.md)
