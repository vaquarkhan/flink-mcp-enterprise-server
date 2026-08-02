---
name: flink-mcp-enterprise
description: >
  Guides work on the flink-mcp-enterprise-server (Java MCP for Apache Flink):
  Maven build/tests, stdio/HTTP transport, governance pipeline, config, and
  publishing. Use when the user mentions Flink MCP, flink-mcp-server,
  tools/call, approval tokens, DLP, or this repository.
---

# Flink MCP Enterprise — agent skill

Author: Viquar Khan

## Quick facts

- Artifact: `io.github.vaquarkhan:flink-mcp-server:0.2.0`
- Main: `io.github.vaquarkhan.flinkmcp.FlinkMcpServer`
- Java 17 · MCP SDK 1.1.3 · Jetty 12 · Logback stderr-only

## Always do

1. Read root `AGENTS.md`.
2. Prefer existing packages (`governance`, `security`, `client`, …) over new frameworks.
3. After code changes: `mvn test`.
4. Preserve secure-by-default writes and stderr-only logging.
5. Use MCP SDK APIs from the verified 1.1.3 map (no inventing constructors).
6. UTF-8 **without BOM** for all `.java` sources.

## Common workflows

### Validate

```bash
mvn clean test
mvn -DskipTests package
python examples/run_all.py --skip-build
```

### Run server

```bash
FLINK_REST_URL=http://localhost:8081 java -jar target/flink-mcp-server-0.2.0-all.jar
```

### Config / security docs

- `doc/configuration.md`
- `doc/security-controls.md`
- `doc/error-codes.md`
