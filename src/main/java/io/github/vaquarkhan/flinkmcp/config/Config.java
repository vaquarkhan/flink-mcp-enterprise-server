package io.github.vaquarkhan.flinkmcp.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class Config {

    public static final Set<String> DEFAULT_READ_TOOLS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "list_jobs",
            "get_job",
            "get_job_status",
            "get_job_exceptions",
            "get_job_metrics",
            "list_checkpoints",
            "list_jars",
            "run_sql_readonly"
    )));

    private final String flinkRestUrl;
    private final String gatewayUrl;
    private final boolean writeEnabled;
    private final String approvalSecret;
    private final long approvalTtlMillis;
    private final boolean readonlyCaller;
    private final int rps;
    private final int breakerFailures;
    private final long breakerResetMillis;
    private final int maxBytes;
    private final boolean dlpEnabled;
    private final String protocolVersion;
    private final String transport;
    private final int httpPort;
    private final String httpBearerToken;
    private final long toolTimeoutMillis;
    private final String policyFile;
    private final Set<String> toolsAllowed;
    private final Set<String> allowedJobs;
    private final Set<String> allowedJars;

    private Config(Builder b) {
        this.flinkRestUrl = b.flinkRestUrl;
        this.gatewayUrl = b.gatewayUrl;
        this.writeEnabled = b.writeEnabled;
        this.approvalSecret = b.approvalSecret;
        this.approvalTtlMillis = b.approvalTtlMillis;
        this.readonlyCaller = b.readonlyCaller;
        this.rps = b.rps;
        this.breakerFailures = b.breakerFailures;
        this.breakerResetMillis = b.breakerResetMillis;
        this.maxBytes = b.maxBytes;
        this.dlpEnabled = b.dlpEnabled;
        this.protocolVersion = b.protocolVersion;
        this.transport = b.transport;
        this.httpPort = b.httpPort;
        this.httpBearerToken = b.httpBearerToken;
        this.toolTimeoutMillis = b.toolTimeoutMillis;
        this.policyFile = b.policyFile;
        this.toolsAllowed = Collections.unmodifiableSet(new LinkedHashSet<>(b.toolsAllowed));
        this.allowedJobs = Collections.unmodifiableSet(new LinkedHashSet<>(b.allowedJobs));
        this.allowedJars = Collections.unmodifiableSet(new LinkedHashSet<>(b.allowedJars));
    }

    public static Config fromEnv() {
        Builder b = builder().defaults();
        b.flinkRestUrl = env("FLINK_REST_URL", b.flinkRestUrl);
        b.gatewayUrl = env("MCP_FLINK_GATEWAY_URL", b.gatewayUrl);
        b.writeEnabled = Boolean.parseBoolean(env("MCP_FLINK_WRITE_ENABLED", "false"));
        b.approvalSecret = System.getenv("MCP_FLINK_APPROVAL_SECRET");
        b.approvalTtlMillis = Long.parseLong(env("MCP_FLINK_APPROVAL_TTL_MS", String.valueOf(b.approvalTtlMillis)));
        b.readonlyCaller = Boolean.parseBoolean(env("MCP_FLINK_READONLY_CALLER", "false"));
        b.rps = Integer.parseInt(env("MCP_FLINK_RPS", String.valueOf(b.rps)));
        b.breakerFailures = Integer.parseInt(env("MCP_FLINK_BREAKER_FAILURES", String.valueOf(b.breakerFailures)));
        b.breakerResetMillis = Long.parseLong(env("MCP_FLINK_BREAKER_RESET_MS", String.valueOf(b.breakerResetMillis)));
        b.maxBytes = Integer.parseInt(env("MCP_FLINK_MAX_BYTES", String.valueOf(b.maxBytes)));
        b.dlpEnabled = Boolean.parseBoolean(env("MCP_FLINK_DLP_ENABLED", "true"));
        b.protocolVersion = env("MCP_FLINK_PROTOCOL_VERSION", b.protocolVersion);
        b.transport = env("MCP_FLINK_TRANSPORT", b.transport);
        b.httpPort = Integer.parseInt(env("MCP_FLINK_HTTP_PORT", String.valueOf(b.httpPort)));
        b.httpBearerToken = System.getenv("MCP_FLINK_HTTP_BEARER_TOKEN");
        b.toolTimeoutMillis = Long.parseLong(env("MCP_FLINK_TOOL_TIMEOUT_MS", String.valueOf(b.toolTimeoutMillis)));
        b.policyFile = System.getenv("MCP_FLINK_POLICY_FILE");
        String tools = System.getenv("MCP_FLINK_TOOLS_ALLOWED");
        if (tools != null && !tools.isBlank()) {
            b.toolsAllowed = parseCsv(tools);
        }
        b.allowedJobs = parseCsv(env("MCP_FLINK_SCOPE_JOBS_ALLOW", "*"));
        b.allowedJars = parseCsv(env("MCP_FLINK_SCOPE_JARS_ALLOW", "*"));
        return b.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    private static Set<String> parseCsv(String csv) {
        Set<String> set = new LinkedHashSet<>();
        for (String part : csv.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                set.add(t);
            }
        }
        return set;
    }

    public String flinkRestUrl() { return flinkRestUrl; }
    public String gatewayUrl() { return gatewayUrl; }
    public boolean writeEnabled() { return writeEnabled; }
    public String approvalSecret() { return approvalSecret; }
    public long approvalTtlMillis() { return approvalTtlMillis; }
    public boolean readonlyCaller() { return readonlyCaller; }
    public int rps() { return rps; }
    public int breakerFailures() { return breakerFailures; }
    public long breakerResetMillis() { return breakerResetMillis; }
    public int maxBytes() { return maxBytes; }
    public boolean dlpEnabled() { return dlpEnabled; }
    public String protocolVersion() { return protocolVersion; }
    public String transport() { return transport; }
    public int httpPort() { return httpPort; }
    public String httpBearerToken() { return httpBearerToken; }
    public long toolTimeoutMillis() { return toolTimeoutMillis; }
    public String policyFile() { return policyFile; }
    public Set<String> toolsAllowed() { return toolsAllowed; }
    public Set<String> allowedJobs() { return allowedJobs; }
    public Set<String> allowedJars() { return allowedJars; }

    public boolean httpAuthConfigured() {
        return httpBearerToken != null && !httpBearerToken.isBlank();
    }

    public boolean writesUnlocked() {
        return writeEnabled && approvalSecret != null && !approvalSecret.isBlank();
    }

    public boolean jobInScope(String id) {
        return allowedJobs.contains("*") || allowedJobs.contains(id);
    }

    public boolean jarInScope(String id) {
        return allowedJars.contains("*") || allowedJars.contains(id);
    }

    public static final class Builder {
        private String flinkRestUrl = "http://localhost:8081";
        private String gatewayUrl = "http://localhost:8083";
        private boolean writeEnabled = false;
        private String approvalSecret;
        private long approvalTtlMillis = 300_000L;
        private boolean readonlyCaller = false;
        private int rps = 5;
        private int breakerFailures = 5;
        private long breakerResetMillis = 30_000L;
        private int maxBytes = 65_536;
        private boolean dlpEnabled = true;
        private String protocolVersion = "2024-11-05";
        private String transport = "stdio";
        private int httpPort = 8090;
        private String httpBearerToken;
        private long toolTimeoutMillis = 30_000L;
        private String policyFile;
        private Set<String> toolsAllowed = new LinkedHashSet<>(DEFAULT_READ_TOOLS);
        private Set<String> allowedJobs = new LinkedHashSet<>(Set.of("*"));
        private Set<String> allowedJars = new LinkedHashSet<>(Set.of("*"));

        public Builder defaults() {
            return this;
        }

        public Builder toolTimeoutMillis(long toolTimeoutMillis) {
            this.toolTimeoutMillis = toolTimeoutMillis;
            return this;
        }

        public Builder toolsAllowed(Set<String> toolsAllowed) {
            this.toolsAllowed = new LinkedHashSet<>(toolsAllowed);
            return this;
        }

        public Config build() {
            return new Config(this);
        }
    }
}
