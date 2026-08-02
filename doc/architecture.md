# Architecture

Author: Viquar Khan

## Big picture
```mermaid
flowchart LR
  Agent["Agent / IDE<br/>Cursor · Claude · Copilot"]
  MCP["flink-mcp-server<br/>stdio or HTTP /mcp"]
  Gov["Governance<br/>expose · scope · policy · approval · rate · breaker"]
  Flink["Flink REST<br/>:8081"]
  GW["SQL Gateway<br/>:8083"]
  Agent -->|"MCP tools/call"| MCP
  MCP --> Gov
  Gov -->|"GET/POST/PATCH"| Flink
  Gov -->|"sessions/statements"| GW
```

## Process layout
| Layer | Package | Role |
|-------|---------|------|
| Entry | `FlinkMcpServer` | Wire config, tools, resources, transport |
| Config | `config.Config` | Twelve-factor env + fail-fast validate |
| Governance | `governance.*` | Ordered deny pipeline + backend pool |
| Security | `security.*` | Approvals, Bearer, policy, nonces |
| Clients | `client.*` | Flink REST + SQL Gateway + SQL guard |
| Observability | `observability.*` | Trace/MDC, metrics, hash-chained audit |
| Transport | `transport.HttpTransportServer` | Jetty + `/healthz` `/readyz` `/metrics` |

## Governance evaluation order
```mermaid
flowchart TB
  A["1 Exposure allow-list"] --> B["2 Readonly caller / job·jar scope"]
  B --> C["3 Policy deny file"]
  C --> D["4 Approval token (non-READ)"]
  D --> E["5 Rate limit"]
  E --> F["6 Circuit breaker"]
  F --> G["7 Timed backend execute"]
  G --> H["8 DLP + size bound + audit ALLOWED"]
```

First denial wins. Codes: [error-codes.md](error-codes.md).

## Transports
| Mode | Binding | Auth |
|------|---------|------|
| `stdio` (default) | process pipes | OS process isolation |
| `http` | `MCP_FLINK_HTTP_HOST:PORT` | Bearer required (fail-closed) |

Stdout is reserved for MCP JSON-RPC. All application logs go to **stderr**.
