# Observability

Author: Viquar Khan

## Logs (stderr only)
Logback pattern includes MDC `trace=`:

```text
2026-08-02T15:12:28.361 INFO  [main] trace=dfc3a5a997b8d6f1 i.g.v.flinkmcp.governance.Governance - allowed tool=list_jobs ms=13
```

Set `MCP_FLINK_LOG_LEVEL=DEBUG` for client HTTP detail.

**Never** write application logs to stdout — it breaks MCP JSON-RPC.

## Metrics
- In-process counters + latency reservoir (p50/p95/p99)
- MCP resources: `flink://metrics`, `flink://metrics/prometheus`
- HTTP scrape: `GET /metrics`

## Audit
Hash-chained ring (max 500). Resource: `flink://audit/recent`.  
Each line: `hash[0:12]  <instant> | trace=… | caller | tool | outcome`.

## Health
| Probe | Behavior |
|-------|----------|
| `GET /healthz` | Always UP if process serving |
| `GET /readyz` | 200 if Flink REST ping succeeds |
| `flink://health` | JSON: mcp version, flink_rest, sql_gateway, writes_unlocked |
