package io.github.vaquarkhan.flinkmcp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vaquarkhan.flinkmcp.config.Config;
import io.github.vaquarkhan.flinkmcp.governance.CircuitBreaker;
import io.github.vaquarkhan.flinkmcp.governance.Governance;
import io.github.vaquarkhan.flinkmcp.governance.OutputControls;
import io.github.vaquarkhan.flinkmcp.governance.RateLimiter;
import io.github.vaquarkhan.flinkmcp.governance.ToolClass;
import io.github.vaquarkhan.flinkmcp.observability.AuditLog;
import io.github.vaquarkhan.flinkmcp.observability.Metrics;
import io.github.vaquarkhan.flinkmcp.security.ApprovalTokens;
import io.github.vaquarkhan.flinkmcp.security.NonceStore;
import io.github.vaquarkhan.flinkmcp.security.PolicyEngine;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GovernanceTest {

    private static Governance gov(Config config) {
        return new Governance(
                config,
                new RateLimiter(1000),
                new CircuitBreaker(5, 30000),
                new OutputControls(1_000_000, false),
                new AuditLog(),
                new ApprovalTokens("secret", new NonceStore()),
                new Metrics(),
                PolicyEngine.load(null),
                "test");
    }

    private static String text(McpSchema.CallToolResult r) {
        return ((McpSchema.TextContent) r.content().get(0)).text();
    }

    @Test
    void governedTimeout_deniesSlowBackend() {
        Config config = Config.builder().defaults().toolTimeoutMillis(50).build();
        Governance g = gov(config);
        McpSchema.CallToolRequest req = new McpSchema.CallToolRequest("list_jobs", Map.of());
        McpSchema.CallToolResult r = g.run("list_jobs", ToolClass.READ, req, () -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "ok";
        });
        assertTrue(Boolean.TRUE.equals(r.isError()));
        assertTrue(text(r).contains("TIMEOUT"));
    }

    @Test
    void fastBackend_allowed() {
        Config config = Config.builder().defaults().build();
        Governance g = gov(config);
        McpSchema.CallToolRequest req = new McpSchema.CallToolRequest("list_jobs", Map.of());
        McpSchema.CallToolResult r = g.run("list_jobs", ToolClass.READ, req, () -> "ok");
        assertFalse(Boolean.TRUE.equals(r.isError()));
        assertTrue(text(r).contains("ok"));
    }

    @Test
    void unexposedTool_denied() {
        Config config = Config.builder().defaults().build();
        Governance g = gov(config);
        McpSchema.CallToolRequest req = new McpSchema.CallToolRequest("stop_job", Map.of("jobId", "j1"));
        McpSchema.CallToolResult r = g.run("stop_job", ToolClass.DESTRUCTIVE, req, () -> "should-not-run");
        assertTrue(Boolean.TRUE.equals(r.isError()));
        assertTrue(text(r).contains("NOT_EXPOSED"));
    }
}
