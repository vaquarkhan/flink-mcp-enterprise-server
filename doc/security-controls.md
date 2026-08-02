# Security controls

Author: Viquar Khan

Broker / Flink ACLs (if configured) remain authoritative. This MCP layer **complements** them with an agent-facing control plane.

## Fail-closed defaults
| Control | Default |
|---------|---------|
| Writes | Off (`MCP_FLINK_WRITE_ENABLED=false`) |
| HTTP | Refuses start without Bearer token |
| Policy missing file | Deny all |
| Jar upload dirs empty | `upload_jar` rejects paths |
| Write enabled without secret | Startup validation error |

## Pipeline stages
1. **NOT_EXPOSED** — tool not in `MCP_FLINK_TOOLS_ALLOWED`  
2. **READONLY_CALLER / SCOPE_DENIED** — caller or job/jar outside scope  
3. **POLICY_DENIED** — `deny tool` / `deny job` globs  
4. **APPROVAL_REQUIRED** — HMAC token missing, wrong tool/scope, expired, or replayed  
5. **RATE_LIMITED** — global RPS window  
6. **BREAKER_OPEN** — consecutive backend failures  
7. **TIMEOUT / BACKEND_ERROR / INVALID_INPUT** — execute phase  

SQL readonly pre-check (`SQL_NOT_READONLY`) runs **before** governance for `run_sql_readonly`.

## Approvals
```text
token = base64url(payload) + "." + base64url(hmac_sha256(payload))
payload = tool|scope|expEpochMillis|nonce
```

Mint:

```bash
java -cp target/flink-mcp-server-0.2.0-all.jar \
  io.github.vaquarkhan.flinkmcp.security.ApprovalTokens \
  <secret> stop_job <jobId> 300
```

Scope binds to `jobId` when present, else `jarId`, else `*`.

## Output DLP
When `MCP_FLINK_DLP_ENABLED=true`, responses redact:

- `api_key` / `secret` / `password` / `token` assignments  
- JWT-shaped strings  
- PEM private key headers  
- Email addresses  
- `Bearer …` credentials  

Then truncate to `MCP_FLINK_MAX_BYTES`.

## Input hygiene
- IDs: `[A-Za-z0-9._-]{1,256}`, no `..`  
- Parallelism: digits only  
- Jar paths: must resolve under `MCP_FLINK_JAR_UPLOAD_ALLOW_DIRS`  
- SQL: length capped; stacked statements rejected for readonly

## Threat model (summary)
| Threat | Mitigation |
|--------|------------|
| Prompt-injected stop/cancel | Tools unregistered + approval + policy |
| Path injection in REST | `Inputs.requireId` / `requireInt` |
| Local file exfil via upload | Directory allow-list |
| Token replay | Nonce store + TTL |
| Secret leakage in tool output | DLP + bound |
| Unauthenticated HTTP MCP | Fail-closed Bearer |
| Protocol/log confusion | stderr-only logging |
