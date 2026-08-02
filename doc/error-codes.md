# Error codes

Author: Viquar Khan

Returned as MCP tool errors: text `denied: <CODE>` (and richer messages for timeouts / backend).

| Code | Stage | Meaning |
|------|-------|---------|
| `NOT_EXPOSED` | 1 | Tool not in allow-list / not registered |
| `READONLY_CALLER` | 2 | Non-READ while readonly caller set |
| `SCOPE_DENIED` | 2 | jobId / jarId outside scope |
| `POLICY_DENIED` | 3 | Policy file deny rule matched |
| `APPROVAL_REQUIRED` | 4 | Missing/invalid/expired/replayed token |
| `RATE_LIMITED` | 5 | RPS exceeded |
| `BREAKER_OPEN` | 6 | Circuit open for tool |
| `TIMEOUT` | 7 | Backend exceeded `TOOL_TIMEOUT_MS` |
| `BACKEND_ERROR` | 7 | Flink/Gateway/transport failure |
| `INVALID_INPUT` | 7 | Path/id/sql validation failed inside call |
| `SQL_NOT_READONLY` | 9* | `run_sql_readonly` rejected mutation / stacked SQL |

\* Pre-governance guard for SQL readonly.
