package io.github.vaquarkhan.flinkmcp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vaquarkhan.flinkmcp.observability.AuditLog;
import io.github.vaquarkhan.flinkmcp.observability.Metrics;
import io.github.vaquarkhan.flinkmcp.security.PolicyEngine;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ObservabilityTest {

    @Test
    void metrics_jsonHasCoreFields() {
        Metrics m = new Metrics();
        m.recordAllowed("list_jobs", 10);
        m.recordDenied("stop_job", "APPROVAL_REQUIRED");
        m.addBytesIn(100);
        m.addBytesOut(40);
        String json = m.toJson();
        assertTrue(json.contains("\"total_calls\":2"));
        assertTrue(json.contains("\"bytes_in\":100"));
        assertTrue(json.contains("\"bytes_out\":40"));
        assertTrue(json.contains("heap_used_bytes"));
        assertTrue(json.contains("list_jobs"));
        assertTrue(json.contains("APPROVAL_REQUIRED"));
    }

    @Test
    void metrics_prometheusExposition() {
        Metrics m = new Metrics();
        m.recordAllowed("list_jobs", 12);
        String prom = m.toPrometheus();
        assertTrue(prom.contains("flink_mcp_calls_total"));
        assertTrue(prom.contains("flink_mcp_heap_used_bytes"));
        assertTrue(prom.contains("flink_mcp_tool_allowed_total{tool=\"list_jobs\"}"));
    }

    @Test
    void audit_recordsAndReportsChain() {
        AuditLog audit = new AuditLog();
        audit.append("test", "list_jobs", "ALLOWED");
        audit.append("test", "stop_job", "DENIED:APPROVAL_REQUIRED:step4");
        assertTrue(audit.recent().size() >= 2);
        assertTrue(audit.verifyChain());
        assertTrue(audit.recent().stream().anyMatch(s -> s.contains("DENIED")));
    }

    @Test
    void policyEngine_nullAllowsAll() {
        PolicyEngine p = PolicyEngine.load(null);
        assertTrue(p.allows("stop_job", "any"));
        assertTrue(p.allows("list_jobs", null));
    }

    @Test
    void policyEngine_denyRuleBlocks() throws Exception {
        Path f = Files.createTempFile("policy", ".txt");
        Files.writeString(f, "deny tool stop_job\ndeny job prod-*\n");
        PolicyEngine p = PolicyEngine.load(f.toString());
        assertFalse(p.allows("stop_job", "dev-1"));
        assertTrue(p.allows("list_jobs", "dev-1"));
        assertFalse(p.allows("list_jobs", "prod-123"));
        assertTrue(p.allows("list_jobs", "dev-1"));
        Files.deleteIfExists(f);
    }

    @Test
    void policyEngine_missingFileFailsClosed() {
        PolicyEngine p = PolicyEngine.load("/nonexistent/policy/file-" + System.nanoTime() + ".txt");
        assertFalse(p.allows("list_jobs", "j1"));
    }
}
