# Flink MCP Server

Production-grade governed MCP server for Apache Flink.

## Build

```bash
mvn clean package
```

Artifacts:
- `target/flink-mcp-server-0.1.0.jar` (thin)
- `target/flink-mcp-server-0.1.0-all.jar` (runnable shaded jar)

## Run (stdio, read-only default)

```bash
FLINK_REST_URL=http://localhost:8081 java -jar target/flink-mcp-server-0.1.0-all.jar
```

## Run (authenticated HTTP)

```bash
MCP_FLINK_TRANSPORT=http MCP_FLINK_HTTP_BEARER_TOKEN=secret \
FLINK_REST_URL=http://localhost:8081 java -jar target/flink-mcp-server-0.1.0-all.jar
```

## Enable writes

Destructive tools require `MCP_FLINK_WRITE_ENABLED=true` and `MCP_FLINK_APPROVAL_SECRET`. Mint tokens with:

```bash
java -cp target/flink-mcp-server-0.1.0-all.jar \
  io.github.vaquarkhan.flinkmcp.security.ApprovalTokens <secret> stop_job <jobId> 300
```

## License

Apache License 2.0