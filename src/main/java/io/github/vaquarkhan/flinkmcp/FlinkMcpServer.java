package io.github.vaquarkhan.flinkmcp;

import io.github.vaquarkhan.flinkmcp.client.FlinkRestClient;
import io.github.vaquarkhan.flinkmcp.client.SqlGatewayClient;
import io.github.vaquarkhan.flinkmcp.client.SqlReadonlyGuard;
import io.github.vaquarkhan.flinkmcp.config.Config;
import io.github.vaquarkhan.flinkmcp.governance.CircuitBreaker;
import io.github.vaquarkhan.flinkmcp.governance.Governance;
import io.github.vaquarkhan.flinkmcp.governance.OutputControls;
import io.github.vaquarkhan.flinkmcp.governance.RateLimiter;
import io.github.vaquarkhan.flinkmcp.governance.ToolClass;
import io.github.vaquarkhan.flinkmcp.observability.AuditLog;
import io.github.vaquarkhan.flinkmcp.observability.Metrics;
import io.github.vaquarkhan.flinkmcp.security.ApprovalTokens;
import io.github.vaquarkhan.flinkmcp.security.BearerAuthFilter;
import io.github.vaquarkhan.flinkmcp.security.NonceStore;
import io.github.vaquarkhan.flinkmcp.security.PolicyEngine;
import io.github.vaquarkhan.flinkmcp.transport.HttpTransportServer;
import io.github.vaquarkhan.flinkmcp.util.Inputs;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlinkMcpServer {

    public static final String VERSION = "0.2.0";
    private static final Logger LOG = LoggerFactory.getLogger(FlinkMcpServer.class);

    private FlinkMcpServer() {}

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) ->
                LoggerFactory.getLogger("uncaught").error("uncaught in {}", t.getName(), e));
        try {
            run(args);
        } catch (IllegalArgumentException e) {
            LOG.error("configuration error: {}", e.getMessage());
            System.exit(2);
        } catch (Throwable t) {
            LOG.error("fatal startup error", t);
            System.exit(1);
        }
    }

    static void run(String[] args) throws Exception {
        Config config = Config.fromEnv();
        applyLogLevel(config.logLevel());
        LOG.info("starting flink-mcp-server version={} transport={} protocol={} writesUnlocked={}",
                VERSION, config.transport(), config.protocolVersion(), config.writesUnlocked());

        Metrics metrics = new Metrics();
        FlinkRestClient flink = new FlinkRestClient(config.flinkRestUrl(), metrics, config.flinkAuthHeader());
        AuditLog audit = new AuditLog();
        OutputControls output = new OutputControls(config.maxBytes(), config.dlpEnabled());
        NonceStore nonces = new NonceStore();
        ApprovalTokens approvals = new ApprovalTokens(config.approvalSecret(), nonces);
        PolicyEngine policy = PolicyEngine.load(config.policyFile());
        String callerLabel = "http".equals(config.transport()) ? "http" : "stdio";
        Governance gov = new Governance(
                config,
                new RateLimiter(config.rps()),
                new CircuitBreaker(config.breakerFailures(), config.breakerResetMillis()),
                output,
                audit,
                approvals,
                metrics,
                policy,
                callerLabel);
        SqlReadonlyGuard sqlGuard = new SqlReadonlyGuard();
        McpJsonMapper json = new JacksonMcpJsonMapperSupplier().get();
        SqlGatewayClient gateway = new SqlGatewayClient(config.gatewayUrl(), json, metrics, config.gatewayAuthHeader());

        List<McpServerFeatures.SyncToolSpecification> tools = new ArrayList<>();

        // --- Read tools (cluster + jobs + jars + SQL) ---
        read(tools, config, gov, json, "get_cluster_info", "Cluster overview (slots, TMs, jobs)",
                "{\"type\":\"object\",\"properties\":{}}", r -> "/overview", flink);
        read(tools, config, gov, json, "get_flink_config", "JobManager /config",
                "{\"type\":\"object\",\"properties\":{}}", r -> "/config", flink);
        read(tools, config, gov, json, "list_taskmanagers", "List TaskManagers and resources",
                "{\"type\":\"object\",\"properties\":{}}", r -> "/taskmanagers", flink);
        read(tools, config, gov, json, "list_jobs", "List Flink jobs",
                "{\"type\":\"object\",\"properties\":{}}", r -> "/jobs/overview", flink);
        read(tools, config, gov, json, "get_job", "Get Flink job details",
                "{\"type\":\"object\",\"properties\":{\"jobId\":{\"type\":\"string\"}},\"required\":[\"jobId\"]}",
                r -> "/jobs/" + Inputs.requireId(arg(r, "jobId")), flink);
        read(tools, config, gov, json, "get_job_status", "Get Flink job status",
                "{\"type\":\"object\",\"properties\":{\"jobId\":{\"type\":\"string\"}},\"required\":[\"jobId\"]}",
                r -> "/jobs/" + Inputs.requireId(arg(r, "jobId")) + "/status", flink);
        read(tools, config, gov, json, "get_job_config", "Get Flink job configuration",
                "{\"type\":\"object\",\"properties\":{\"jobId\":{\"type\":\"string\"}},\"required\":[\"jobId\"]}",
                r -> "/jobs/" + Inputs.requireId(arg(r, "jobId")) + "/config", flink);
        read(tools, config, gov, json, "get_job_exceptions", "Get Flink job exceptions",
                "{\"type\":\"object\",\"properties\":{\"jobId\":{\"type\":\"string\"}},\"required\":[\"jobId\"]}",
                r -> "/jobs/" + Inputs.requireId(arg(r, "jobId")) + "/exceptions", flink);
        read(tools, config, gov, json, "get_job_metrics", "Get Flink job metrics",
                "{\"type\":\"object\",\"properties\":{\"jobId\":{\"type\":\"string\"}},\"required\":[\"jobId\"]}",
                r -> "/jobs/" + Inputs.requireId(arg(r, "jobId")) + "/metrics", flink);
        read(tools, config, gov, json, "list_checkpoints", "List Flink job checkpoints",
                "{\"type\":\"object\",\"properties\":{\"jobId\":{\"type\":\"string\"}},\"required\":[\"jobId\"]}",
                r -> "/jobs/" + Inputs.requireId(arg(r, "jobId")) + "/checkpoints", flink);
        read(tools, config, gov, json, "list_jars", "List uploaded jars",
                "{\"type\":\"object\",\"properties\":{}}", r -> "/jars", flink);

        if (config.toolsAllowed().contains("run_sql_readonly")) {
            McpSchema.Tool tool = McpSchema.Tool.builder()
                    .name("run_sql_readonly")
                    .description("Execute read-only SQL via Flink SQL Gateway")
                    .inputSchema(json, "{\"type\":\"object\",\"properties\":{\"sql\":{\"type\":\"string\"}},\"required\":[\"sql\"]}")
                    .build();
            tools.add(McpServerFeatures.SyncToolSpecification.builder()
                    .tool(tool)
                    .callHandler((exchange, request) -> {
                        try {
                            String sql = Inputs.requireSql(arg(request, "sql"), config.maxSqlChars());
                            if (!sqlGuard.isReadOnly(sql)) {
                                audit.append(callerLabel, "run_sql_readonly", "DENIED:SQL_NOT_READONLY:step9");
                                return McpSchema.CallToolResult.builder()
                                        .isError(true)
                                        .addTextContent("denied: SQL_NOT_READONLY")
                                        .build();
                            }
                            return gov.run("run_sql_readonly", ToolClass.READ, request, () -> gateway.execute(sql));
                        } catch (Inputs.InvalidInput e) {
                            return McpSchema.CallToolResult.builder()
                                    .isError(true)
                                    .addTextContent("denied: INVALID_INPUT")
                                    .build();
                        }
                    })
                    .build());
        }

        // --- Write tools (gated) ---
        write(tools, config, gov, json, "trigger_savepoint", "Trigger a savepoint", ToolClass.MUTATE,
                "{\"type\":\"object\",\"properties\":{\"jobId\":{\"type\":\"string\"},\"targetDirectory\":{\"type\":\"string\"},\"approvalToken\":{\"type\":\"string\"}},\"required\":[\"jobId\",\"approvalToken\"]}",
                r -> {
                    String jobId = Inputs.requireId(arg(r, "jobId"));
                    String dir = arg(r, "targetDirectory");
                    String body = dir == null || dir.isBlank()
                            ? "{}"
                            : "{\"targetDirectory\":\"" + Inputs.jsonEscape(dir) + "\"}";
                    return flink.post("/jobs/" + jobId + "/savepoints", body);
                });
        write(tools, config, gov, json, "rescale_job", "Rescale a Flink job", ToolClass.MUTATE,
                "{\"type\":\"object\",\"properties\":{\"jobId\":{\"type\":\"string\"},\"parallelism\":{\"type\":\"string\"},\"approvalToken\":{\"type\":\"string\"}},\"required\":[\"jobId\",\"parallelism\",\"approvalToken\"]}",
                r -> flink.patch(
                        "/jobs/" + Inputs.requireId(arg(r, "jobId"))
                                + "/rescaling?parallelism=" + Inputs.requireInt(arg(r, "parallelism")),
                        "{}"));
        write(tools, config, gov, json, "cancel_job", "Cancel a Flink job", ToolClass.DESTRUCTIVE,
                "{\"type\":\"object\",\"properties\":{\"jobId\":{\"type\":\"string\"},\"approvalToken\":{\"type\":\"string\"}},\"required\":[\"jobId\",\"approvalToken\"]}",
                r -> flink.patch("/jobs/" + Inputs.requireId(arg(r, "jobId")), "{\"mode\":\"cancel\"}"));
        write(tools, config, gov, json, "upload_jar", "Upload a jar (path must be under allow-listed dirs)", ToolClass.MUTATE,
                "{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"approvalToken\":{\"type\":\"string\"}},\"required\":[\"path\",\"approvalToken\"]}",
                r -> flink.uploadJar(Inputs.requireJarPath(arg(r, "path"), config.jarUploadAllowDirs())));
        write(tools, config, gov, json, "run_jar", "Run an uploaded jar", ToolClass.DESTRUCTIVE,
                "{\"type\":\"object\",\"properties\":{\"jarId\":{\"type\":\"string\"},\"entryClass\":{\"type\":\"string\"},\"programArgs\":{\"type\":\"string\"},\"parallelism\":{\"type\":\"string\"},\"approvalToken\":{\"type\":\"string\"}},\"required\":[\"jarId\",\"approvalToken\"]}",
                r -> {
                    String jarId = Inputs.requireId(arg(r, "jarId"));
                    StringBuilder body = new StringBuilder("{");
                    boolean first = true;
                    String entry = arg(r, "entryClass");
                    if (entry != null && !entry.isBlank()) {
                        body.append("\"entryClass\":\"").append(Inputs.jsonEscape(entry)).append('"');
                        first = false;
                    }
                    String progArgs = arg(r, "programArgs");
                    if (progArgs != null && !progArgs.isBlank()) {
                        if (!first) {
                            body.append(',');
                        }
                        body.append("\"programArgs\":\"").append(Inputs.jsonEscape(progArgs)).append('"');
                        first = false;
                    }
                    String parallelism = arg(r, "parallelism");
                    if (parallelism != null && !parallelism.isBlank()) {
                        if (!first) {
                            body.append(',');
                        }
                        body.append("\"parallelism\":").append(Inputs.requireInt(parallelism));
                    }
                    body.append('}');
                    return flink.post("/jars/" + jarId + "/run", body.toString());
                });
        write(tools, config, gov, json, "stop_job", "Stop a job with savepoint", ToolClass.DESTRUCTIVE,
                "{\"type\":\"object\",\"properties\":{\"jobId\":{\"type\":\"string\"},\"targetDirectory\":{\"type\":\"string\"},\"approvalToken\":{\"type\":\"string\"}},\"required\":[\"jobId\",\"approvalToken\"]}",
                r -> {
                    String jobId = Inputs.requireId(arg(r, "jobId"));
                    String dir = arg(r, "targetDirectory");
                    String body = dir == null || dir.isBlank()
                            ? "{}"
                            : "{\"targetDirectory\":\"" + Inputs.jsonEscape(dir) + "\"}";
                    return flink.post("/jobs/" + jobId + "/stop", body);
                });

        if (config.writesUnlocked() && config.toolsAllowed().contains("run_sql_ddl_dml")) {
            McpSchema.Tool tool = McpSchema.Tool.builder()
                    .name("run_sql_ddl_dml")
                    .description("Execute DDL/DML SQL via Flink SQL Gateway")
                    .inputSchema(json, "{\"type\":\"object\",\"properties\":{\"sql\":{\"type\":\"string\"},\"approvalToken\":{\"type\":\"string\"}},\"required\":[\"sql\",\"approvalToken\"]}")
                    .build();
            tools.add(McpServerFeatures.SyncToolSpecification.builder()
                    .tool(tool)
                    .callHandler((exchange, request) ->
                            gov.run("run_sql_ddl_dml", ToolClass.DESTRUCTIVE, request, () ->
                                    gateway.execute(Inputs.requireSql(arg(request, "sql"), config.maxSqlChars()))))
                    .build());
        }

        List<McpServerFeatures.SyncResourceSpecification> resources = List.of(
                restResource("flink://cluster/overview", "cluster-overview", "application/json",
                        callerLabel, audit, output, () -> flink.get("/overview")),
                restResource("flink://jobs", "jobs", "application/json",
                        callerLabel, audit, output, () -> flink.get("/jobs/overview")),
                restResource("flink://health", "health", "application/json",
                        callerLabel, audit, output, () -> {
                            boolean flinkOk = flink.ping();
                            boolean gwOk = gateway.ping();
                            return "{\"mcp\":\"" + VERSION + "\",\"flink_rest\":" + flinkOk
                                    + ",\"sql_gateway\":" + gwOk + ",\"writes_unlocked\":"
                                    + config.writesUnlocked() + "}";
                        }),
                new McpServerFeatures.SyncResourceSpecification(
                        McpSchema.Resource.builder()
                                .uri("flink://audit/recent")
                                .name("audit-recent")
                                .mimeType("text/plain")
                                .build(),
                        (exchange, request) -> new McpSchema.ReadResourceResult(List.of(
                                new McpSchema.TextResourceContents(
                                        "flink://audit/recent", "text/plain", String.join("\n", audit.recent()))))),
                new McpServerFeatures.SyncResourceSpecification(
                        McpSchema.Resource.builder()
                                .uri("flink://metrics")
                                .name("metrics")
                                .mimeType("application/json")
                                .build(),
                        (exchange, request) -> new McpSchema.ReadResourceResult(List.of(
                                new McpSchema.TextResourceContents(
                                        "flink://metrics", "application/json", metrics.toJson())))),
                new McpServerFeatures.SyncResourceSpecification(
                        McpSchema.Resource.builder()
                                .uri("flink://metrics/prometheus")
                                .name("metrics-prometheus")
                                .mimeType("text/plain")
                                .build(),
                        (exchange, request) -> new McpSchema.ReadResourceResult(List.of(
                                new McpSchema.TextResourceContents(
                                        "flink://metrics/prometheus", "text/plain", metrics.toPrometheus()))))
        );

        McpSchema.ServerCapabilities caps = McpSchema.ServerCapabilities.builder()
                .tools(true)
                .resources(false, false)
                .build();

        AtomicReference<Server> httpServer = new AtomicReference<>();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("shutdown hook: draining backend pool and http");
            gov.shutdown(config.shutdownTimeoutMillis());
            HttpTransportServer.stopQuietly(httpServer.get(), config.shutdownTimeoutMillis());
        }, "flink-mcp-shutdown"));

        LOG.info("registered tools={} resources={} default_read_profile={}",
                tools.size(), resources.size(), !config.writesUnlocked());

        if ("http".equals(config.transport())) {
            if (!config.httpAuthConfigured()) {
                LOG.error("HTTP transport requires MCP_FLINK_HTTP_BEARER_TOKEN; refusing to start");
                System.exit(2);
            }
            HttpServletStreamableServerTransportProvider httpTransport =
                    HttpServletStreamableServerTransportProvider.builder()
                            .jsonMapper(json)
                            .mcpEndpoint("/mcp")
                            .build();
            McpServer.sync(httpTransport)
                    .serverInfo("flink-mcp-server", VERSION)
                    .capabilities(caps)
                    .tools(tools)
                    .resources(resources)
                    .build();
            Server server = HttpTransportServer.start(
                    config.httpHost(),
                    config.httpPort(),
                    "/mcp/*",
                    httpTransport,
                    new BearerAuthFilter(config.httpBearerToken()),
                    metrics,
                    () -> flink.ping());
            httpServer.set(server);
            server.join();
        } else {
            StdioServerTransportProvider stdio = new StdioServerTransportProvider(json);
            McpServer.sync(stdio)
                    .serverInfo("flink-mcp-server", VERSION)
                    .capabilities(caps)
                    .tools(tools)
                    .resources(resources)
                    .build();
            LOG.info("stdio transport ready (stdout=MCP JSON-RPC, stderr=logs)");
            Thread.currentThread().join();
        }
    }

    private static void applyLogLevel(String level) {
        try {
            Object factory = LoggerFactory.getILoggerFactory();
            if (!factory.getClass().getName().contains("LoggerContext")) {
                return;
            }
            Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
            Object lv = levelClass.getMethod("toLevel", String.class, levelClass)
                    .invoke(null, level, levelClass.getField("INFO").get(null));
            Object root = factory.getClass().getMethod("getLogger", String.class)
                    .invoke(factory, Logger.ROOT_LOGGER_NAME);
            root.getClass().getMethod("setLevel", levelClass).invoke(root, lv);
            Object app = factory.getClass().getMethod("getLogger", String.class)
                    .invoke(factory, "io.github.vaquarkhan.flinkmcp");
            app.getClass().getMethod("setLevel", levelClass).invoke(app, lv);
        } catch (Exception e) {
            // binder may not be logback in tests
        }
    }

    private static void read(
            List<McpServerFeatures.SyncToolSpecification> tools,
            Config config,
            Governance gov,
            McpJsonMapper json,
            String name,
            String desc,
            String schema,
            Function<McpSchema.CallToolRequest, String> pathFn,
            FlinkRestClient flink) {
        if (!config.toolsAllowed().contains(name)) {
            return;
        }
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(name)
                .description(desc)
                .inputSchema(json, schema)
                .build();
        tools.add(McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) ->
                        gov.run(name, ToolClass.READ, request, () -> flink.get(pathFn.apply(request))))
                .build());
    }

    private static void write(
            List<McpServerFeatures.SyncToolSpecification> tools,
            Config config,
            Governance gov,
            McpJsonMapper json,
            String name,
            String desc,
            ToolClass cls,
            String schema,
            Function<McpSchema.CallToolRequest, String> backendFn) {
        if (!config.writesUnlocked() || !config.toolsAllowed().contains(name)) {
            return;
        }
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(name)
                .description(desc)
                .inputSchema(json, schema)
                .build();
        tools.add(McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) ->
                        gov.run(name, cls, request, () -> backendFn.apply(request)))
                .build());
    }

    private static McpServerFeatures.SyncResourceSpecification restResource(
            String uri,
            String name,
            String mime,
            String callerLabel,
            AuditLog audit,
            OutputControls output,
            java.util.concurrent.Callable<String> fetch) {
        return new McpServerFeatures.SyncResourceSpecification(
                McpSchema.Resource.builder().uri(uri).name(name).mimeType(mime).build(),
                (exchange, request) -> {
                    try {
                        String body = fetch.call();
                        String safe = output.boundAndRedact(body);
                        audit.append(callerLabel, uri, "ALLOWED");
                        return new McpSchema.ReadResourceResult(
                                List.of(new McpSchema.TextResourceContents(uri, mime, safe)));
                    } catch (Exception e) {
                        audit.append(callerLabel, uri, "ERROR");
                        LOG.warn("resource {} error: {}", uri, e.getMessage());
                        String msg = output.boundAndRedact(
                                e.getMessage() == null ? e.getClass().getName() : e.getMessage());
                        return new McpSchema.ReadResourceResult(
                                List.of(new McpSchema.TextResourceContents(uri, mime, msg)));
                    }
                });
    }

    private static String arg(McpSchema.CallToolRequest request, String key) {
        Map<String, Object> args = request.arguments();
        if (args == null) {
            return null;
        }
        Object v = args.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
