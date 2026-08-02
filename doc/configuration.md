# Configuration

Author: Viquar Khan

All settings are environment variables. `Config.fromEnv()` parses and `validate()` fails fast on bad values.

| Variable | Default | Notes |
|----------|---------|-------|
| `FLINK_REST_URL` | `http://localhost:8081` | JobManager REST |
| `MCP_FLINK_GATEWAY_URL` | `http://localhost:8083` | SQL Gateway |
| `MCP_FLINK_REST_AUTH_HEADER` | — | Optional `Bearer …` / `Basic …` to Flink |
| `MCP_FLINK_GATEWAY_AUTH_HEADER` | — | Optional auth to Gateway |
| `MCP_FLINK_WRITE_ENABLED` | `false` | Master write switch |
| `MCP_FLINK_APPROVAL_SECRET` | — | Required if writes enabled |
| `MCP_FLINK_APPROVAL_TTL_MS` | `300000` | Default mint TTL guidance |
| `MCP_FLINK_READONLY_CALLER` | `false` | Force deny non-READ |
| `MCP_FLINK_RPS` | `5` | Global rate limit |
| `MCP_FLINK_BREAKER_FAILURES` | `5` | Open after N failures |
| `MCP_FLINK_BREAKER_RESET_MS` | `30000` | Open → half-open |
| `MCP_FLINK_MAX_BYTES` | `65536` | Output bound |
| `MCP_FLINK_DLP_ENABLED` | `true` | Redaction |
| `MCP_FLINK_PROTOCOL_VERSION` | `2024-11-05` | Advertised MCP version |
| `MCP_FLINK_TRANSPORT` | `stdio` | `stdio` \| `http` |
| `MCP_FLINK_HTTP_HOST` | `127.0.0.1` | Bind address |
| `MCP_FLINK_HTTP_PORT` | `8090` | HTTP port |
| `MCP_FLINK_HTTP_BEARER_TOKEN` | — | **Required** for HTTP |
| `MCP_FLINK_TOOL_TIMEOUT_MS` | `30000` | Backend timeout |
| `MCP_FLINK_POLICY_FILE` | — | Deny rules path |
| `MCP_FLINK_TOOLS_ALLOWED` | default reads | CSV; writes auto-merge when write enabled without custom list |
| `MCP_FLINK_SCOPE_JOBS_ALLOW` | `*` | Job allow-list |
| `MCP_FLINK_SCOPE_JARS_ALLOW` | `*` | Jar allow-list |
| `MCP_FLINK_JAR_UPLOAD_ALLOW_DIRS` | — | Required for `upload_jar` |
| `MCP_FLINK_MAX_SQL_CHARS` | `32768` | SQL size cap |
| `MCP_FLINK_SHUTDOWN_TIMEOUT_MS` | `15000` | Drain timeout |
| `MCP_FLINK_LOG_LEVEL` | `INFO` | Logback level |

Policy sample: [`../config/policy.sample`](../config/policy.sample)
