package io.github.vaquarkhan.flinkmcp.config;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Twelve-factor configuration: all runtime settings come from the environment.
 * Call {@link #validate()} at startup (fail-fast).
 */
/**
 * @author Viquar Khan
 */
public final class Config {

    public static final Set<String> DEFAULT_READ_TOOLS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "list_jobs",
            "get_job",
            "get_job_status",
            "get_job_exceptions",
            "get_job_metrics",
            "list_checkpoints",
            "list_jars",
            "run_sql_readonly",
            "get_cluster_info",
            "list_taskmanagers",
            "get_job_config",
            "get_flink_config"
    )));

    public static final Set<String> WRITE_TOOLS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "trigger_savepoint",
            "rescale_job",
            "upload_jar",
            "run_jar",
            "stop_job",
            "cancel_job",
            "run_sql_ddl_dml"
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
    private final String httpHost;
    private final String httpBearerToken;
    private final long toolTimeoutMillis;
    private final String policyFile;
    private final Set<String> toolsAllowed;
    private final Set<String> allowedJobs;
    private final Set<String> allowedJars;
    private final String flinkAuthHeader;
    private final String gatewayAuthHeader;
    private final Set<String> jarUploadAllowDirs;
    private final int maxSqlChars;
    private final long shutdownTimeoutMillis;
    private final String logLevel;

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
        this.httpHost = b.httpHost;
        this.httpBearerToken = b.httpBearerToken;
        this.toolTimeoutMillis = b.toolTimeoutMillis;
        this.policyFile = b.policyFile;
        this.toolsAllowed = Collections.unmodifiableSet(new LinkedHashSet<>(b.toolsAllowed));
        this.allowedJobs = Collections.unmodifiableSet(new LinkedHashSet<>(b.allowedJobs));
        this.allowedJars = Collections.unmodifiableSet(new LinkedHashSet<>(b.allowedJars));
        this.flinkAuthHeader = b.flinkAuthHeader;
        this.gatewayAuthHeader = b.gatewayAuthHeader;
        this.jarUploadAllowDirs = Collections.unmodifiableSet(new LinkedHashSet<>(b.jarUploadAllowDirs));
        this.maxSqlChars = b.maxSqlChars;
        this.shutdownTimeoutMillis = b.shutdownTimeoutMillis;
        this.logLevel = b.logLevel;
    }

    public static Config fromEnv() {
        Builder b = builder().defaults();
        try {
            b.flinkRestUrl = env("FLINK_REST_URL", b.flinkRestUrl);
            b.gatewayUrl = env("MCP_FLINK_GATEWAY_URL", b.gatewayUrl);
            b.writeEnabled = Boolean.parseBoolean(env("MCP_FLINK_WRITE_ENABLED", "false"));
            b.approvalSecret = System.getenv("MCP_FLINK_APPROVAL_SECRET");
            b.approvalTtlMillis = parseLong("MCP_FLINK_APPROVAL_TTL_MS", b.approvalTtlMillis);
            b.readonlyCaller = Boolean.parseBoolean(env("MCP_FLINK_READONLY_CALLER", "false"));
            b.rps = parseInt("MCP_FLINK_RPS", b.rps);
            b.breakerFailures = parseInt("MCP_FLINK_BREAKER_FAILURES", b.breakerFailures);
            b.breakerResetMillis = parseLong("MCP_FLINK_BREAKER_RESET_MS", b.breakerResetMillis);
            b.maxBytes = parseInt("MCP_FLINK_MAX_BYTES", b.maxBytes);
            b.dlpEnabled = Boolean.parseBoolean(env("MCP_FLINK_DLP_ENABLED", "true"));
            b.protocolVersion = env("MCP_FLINK_PROTOCOL_VERSION", b.protocolVersion);
            b.transport = env("MCP_FLINK_TRANSPORT", b.transport).toLowerCase(Locale.ROOT);
            b.httpPort = parseInt("MCP_FLINK_HTTP_PORT", b.httpPort);
            b.httpHost = env("MCP_FLINK_HTTP_HOST", b.httpHost);
            b.httpBearerToken = System.getenv("MCP_FLINK_HTTP_BEARER_TOKEN");
            b.toolTimeoutMillis = parseLong("MCP_FLINK_TOOL_TIMEOUT_MS", b.toolTimeoutMillis);
            b.policyFile = System.getenv("MCP_FLINK_POLICY_FILE");
            String tools = System.getenv("MCP_FLINK_TOOLS_ALLOWED");
            if (tools != null && !tools.isBlank()) {
                b.toolsAllowed = parseCsv(tools);
            } else if (b.writeEnabled) {
                // Default write profile: expose reads + all write tools when writes unlocked.
                Set<String> expanded = new LinkedHashSet<>(DEFAULT_READ_TOOLS);
                expanded.addAll(WRITE_TOOLS);
                b.toolsAllowed = expanded;
            }
            b.allowedJobs = parseCsv(env("MCP_FLINK_SCOPE_JOBS_ALLOW", "*"));
            b.allowedJars = parseCsv(env("MCP_FLINK_SCOPE_JARS_ALLOW", "*"));
            b.flinkAuthHeader = blankToNull(System.getenv("MCP_FLINK_REST_AUTH_HEADER"));
            b.gatewayAuthHeader = blankToNull(System.getenv("MCP_FLINK_GATEWAY_AUTH_HEADER"));
            String dirs = System.getenv("MCP_FLINK_JAR_UPLOAD_ALLOW_DIRS");
            if (dirs != null && !dirs.isBlank()) {
                b.jarUploadAllowDirs = parseCsv(dirs);
            }
            b.maxSqlChars = parseInt("MCP_FLINK_MAX_SQL_CHARS", b.maxSqlChars);
            b.shutdownTimeoutMillis = parseLong("MCP_FLINK_SHUTDOWN_TIMEOUT_MS", b.shutdownTimeoutMillis);
            b.logLevel = env("MCP_FLINK_LOG_LEVEL", b.logLevel).toUpperCase(Locale.ROOT);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric configuration: " + e.getMessage(), e);
        }
        Config c = b.build();
        c.validate();
        return c;
    }

    public void validate() {
        requireHttpUrl("FLINK_REST_URL", flinkRestUrl);
        requireHttpUrl("MCP_FLINK_GATEWAY_URL", gatewayUrl);
        if (!"stdio".equals(transport) && !"http".equals(transport)) {
            throw new IllegalArgumentException("MCP_FLINK_TRANSPORT must be stdio or http, got: " + transport);
        }
        if (httpPort < 1 || httpPort > 65535) {
            throw new IllegalArgumentException("MCP_FLINK_HTTP_PORT out of range: " + httpPort);
        }
        if (rps < 1 || rps > 100_000) {
            throw new IllegalArgumentException("MCP_FLINK_RPS out of range: " + rps);
        }
        if (breakerFailures < 1) {
            throw new IllegalArgumentException("MCP_FLINK_BREAKER_FAILURES must be >= 1");
        }
        if (maxBytes < 256) {
            throw new IllegalArgumentException("MCP_FLINK_MAX_BYTES must be >= 256");
        }
        if (toolTimeoutMillis < 50) {
            throw new IllegalArgumentException("MCP_FLINK_TOOL_TIMEOUT_MS must be >= 50");
        }
        if (maxSqlChars < 1) {
            throw new IllegalArgumentException("MCP_FLINK_MAX_SQL_CHARS must be >= 1");
        }
        if (writeEnabled && (approvalSecret == null || approvalSecret.isBlank())) {
            throw new IllegalArgumentException(
                    "MCP_FLINK_WRITE_ENABLED=true requires MCP_FLINK_APPROVAL_SECRET (fail-closed)");
        }
        if ("http".equals(transport) && !httpAuthConfigured()) {
            throw new IllegalArgumentException(
                    "MCP_FLINK_TRANSPORT=http requires MCP_FLINK_HTTP_BEARER_TOKEN (fail-closed)");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private static void requireHttpUrl(String name, String url) {
        try {
            URI u = URI.create(url);
            String scheme = u.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException(name + " must be http(s) URL: " + url);
            }
            if (u.getHost() == null || u.getHost().isBlank()) {
                throw new IllegalArgumentException(name + " missing host: " + url);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(name + " invalid URL: " + url, e);
        }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    private static int parseInt(String key, int def) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            return def;
        }
        return Integer.parseInt(v.trim());
    }

    private static long parseLong(String key, long def) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            return def;
        }
        return Long.parseLong(v.trim());
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
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
    public String httpHost() { return httpHost; }
    public String httpBearerToken() { return httpBearerToken; }
    public long toolTimeoutMillis() { return toolTimeoutMillis; }
    public String policyFile() { return policyFile; }
    public Set<String> toolsAllowed() { return toolsAllowed; }
    public Set<String> allowedJobs() { return allowedJobs; }
    public Set<String> allowedJars() { return allowedJars; }
    public String flinkAuthHeader() { return flinkAuthHeader; }
    public String gatewayAuthHeader() { return gatewayAuthHeader; }
    public Set<String> jarUploadAllowDirs() { return jarUploadAllowDirs; }
    public int maxSqlChars() { return maxSqlChars; }
    public long shutdownTimeoutMillis() { return shutdownTimeoutMillis; }
    public String logLevel() { return logLevel; }

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
        private String httpHost = "127.0.0.1";
        private String httpBearerToken;
        private long toolTimeoutMillis = 30_000L;
        private String policyFile;
        private Set<String> toolsAllowed = new LinkedHashSet<>(DEFAULT_READ_TOOLS);
        private Set<String> allowedJobs = new LinkedHashSet<>(Set.of("*"));
        private Set<String> allowedJars = new LinkedHashSet<>(Set.of("*"));
        private String flinkAuthHeader;
        private String gatewayAuthHeader;
        private Set<String> jarUploadAllowDirs = new LinkedHashSet<>();
        private int maxSqlChars = 32_768;
        private long shutdownTimeoutMillis = 15_000L;
        private String logLevel = "INFO";

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

        public Builder writeEnabled(boolean writeEnabled) {
            this.writeEnabled = writeEnabled;
            return this;
        }

        public Builder approvalSecret(String approvalSecret) {
            this.approvalSecret = approvalSecret;
            return this;
        }

        public Builder readonlyCaller(boolean readonlyCaller) {
            this.readonlyCaller = readonlyCaller;
            return this;
        }

        public Builder rps(int rps) {
            this.rps = rps;
            return this;
        }

        public Builder transport(String transport) {
            this.transport = transport;
            return this;
        }

        public Builder httpBearerToken(String httpBearerToken) {
            this.httpBearerToken = httpBearerToken;
            return this;
        }

        public Builder jarUploadAllowDirs(Set<String> dirs) {
            this.jarUploadAllowDirs = new LinkedHashSet<>(dirs);
            return this;
        }

        public Builder maxSqlChars(int maxSqlChars) {
            this.maxSqlChars = maxSqlChars;
            return this;
        }

        public Config build() {
            return new Config(this);
        }
    }
}
