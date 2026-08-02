# Documentation

Author: Viquar Khan

End-to-end guide for the **Flink MCP Enterprise Server** — a production Java MCP control plane for Apache Flink.

1. [Getting started](getting-started.md) — build, run, call a tool  
2. [Developer tutorial](tutorial.md) — hands-on path from zero to governed writes  
3. [Architecture](architecture.md) — components & Mermaid diagrams  
4. [Security controls](security-controls.md) — fail-closed pipeline  
5. [Configuration](configuration.md) — every environment variable  
6. [Tools & resources](tools-and-resources.md) — full MCP surface  
7. [Error codes](error-codes.md) — operator / agent error map  
8. [Observability](observability.md) — logs, metrics, audit, health  
9. [Testing](testing.md) — unit suite & example runners  
10. [Publishing](publishing.md) — Maven Central  
11. [Agents & skills](agents-and-skills.md) — Cursor, Copilot, ChatGPT, Gemini  

Hands-on scenarios: [../examples/](../examples/README.md)

Public site: [../site/](../site/) → https://vaquarkhan.github.io/flink-mcp-enterprise-server/

## Product images
![Banner](assets/flink-mcp-banner.svg)

![What's included](assets/flink-mcp-features.svg)

These are documentation packaging images — the server itself has no web UI (ops endpoints `/healthz`, `/readyz`, `/metrics` only in HTTP mode).
