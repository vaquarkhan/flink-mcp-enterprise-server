package io.github.vaquarkhan.flinkmcp.governance;

import io.github.vaquarkhan.flinkmcp.config.Config;
import io.github.vaquarkhan.flinkmcp.observability.AuditLog;
import io.github.vaquarkhan.flinkmcp.observability.Metrics;
import io.github.vaquarkhan.flinkmcp.observability.Trace;
import io.github.vaquarkhan.flinkmcp.security.ApprovalTokens;
import io.github.vaquarkhan.flinkmcp.security.PolicyEngine;
import io.github.vaquarkhan.flinkmcp.util.Inputs;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Governance {

    private static final Logger LOG = LoggerFactory.getLogger(Governance.class);

    private final Config config;
    private final RateLimiter rateLimiter;
    private final CircuitBreaker breaker;
    private final OutputControls output;
    private final AuditLog audit;
    private final ApprovalTokens approvals;
    private final Metrics metrics;
    private final PolicyEngine policy;
    private final String callerLabel;
    private final ThreadPoolExecutor backendPool;

    public Governance(
            Config config,
            RateLimiter rateLimiter,
            CircuitBreaker breaker,
            OutputControls output,
            AuditLog audit,
            ApprovalTokens approvals,
            Metrics metrics,
            PolicyEngine policy,
            String callerLabel) {
        this.config = config;
        this.rateLimiter = rateLimiter;
        this.breaker = breaker;
        this.output = output;
        this.audit = audit;
        this.approvals = approvals;
        this.metrics = metrics;
        this.policy = policy;
        this.callerLabel = callerLabel;
        AtomicInteger n = new AtomicInteger();
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "flink-mcp-backend-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        this.backendPool = new ThreadPoolExecutor(
                4, 32, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(128), tf, new ThreadPoolExecutor.AbortPolicy());
    }

    public McpSchema.CallToolResult run(
            String toolName, ToolClass cls, McpSchema.CallToolRequest request, Supplier<String> backendCall) {
        String trace = Trace.newId();
        long started = System.nanoTime();
        try {
            String jobId = arg(request, "jobId");
            String jarId = arg(request, "jarId");
            LOG.info("call tool={} class={} job={} jar={}", toolName, cls,
                    jobId == null ? "-" : jobId, jarId == null ? "-" : jarId);

            if (!config.toolsAllowed().contains(toolName)) {
                return deny(toolName, "NOT_EXPOSED", 1);
            }
            if (cls != ToolClass.READ && config.readonlyCaller()) {
                return deny(toolName, "READONLY_CALLER", 2);
            }
            if (jobId != null && !jobId.isBlank() && !config.jobInScope(jobId)) {
                return deny(toolName, "SCOPE_DENIED", 2);
            }
            if (jarId != null && !jarId.isBlank() && !config.jarInScope(jarId)) {
                return deny(toolName, "SCOPE_DENIED", 2);
            }
            if (!policy.allows(toolName, jobId)) {
                return deny(toolName, "POLICY_DENIED", 3);
            }
            if (cls != ToolClass.READ) {
                String scope;
                if (jobId != null && !jobId.isBlank()) {
                    scope = jobId;
                } else if (jarId != null && !jarId.isBlank()) {
                    scope = jarId;
                } else {
                    scope = "*";
                }
                String token = arg(request, "approvalToken");
                if (!approvals.verify(token, toolName, scope)) {
                    return deny(toolName, "APPROVAL_REQUIRED", 4);
                }
            }
            if (!rateLimiter.allow()) {
                return deny(toolName, "RATE_LIMITED", 5);
            }
            if (breaker.isOpen(toolName)) {
                return deny(toolName, "BREAKER_OPEN", 6);
            }

            Future<String> future = backendPool.submit(backendCall::get);
            String body;
            try {
                body = future.get(config.toolTimeoutMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                future.cancel(true);
                breaker.recordFailure(toolName);
                metrics.recordDenied(toolName, "TIMEOUT");
                audit.append(callerLabel, toolName, "DENIED:TIMEOUT:step7");
                LOG.warn("timeout tool={} after={}ms", toolName, config.toolTimeoutMillis());
                return McpSchema.CallToolResult.builder()
                        .isError(true)
                        .addTextContent("denied: TIMEOUT after " + config.toolTimeoutMillis() + "ms")
                        .build();
            } catch (ExecutionException ee) {
                Throwable cause = ee.getCause() == null ? ee : ee.getCause();
                if (cause instanceof Inputs.InvalidInput) {
                    metrics.recordDenied(toolName, "INVALID_INPUT");
                    audit.append(callerLabel, toolName, "DENIED:INVALID_INPUT:step7");
                    LOG.warn("invalid input tool={} msg={}", toolName, cause.getMessage());
                    return McpSchema.CallToolResult.builder()
                            .isError(true)
                            .addTextContent("denied: INVALID_INPUT")
                            .build();
                }
                breaker.recordFailure(toolName);
                metrics.recordDenied(toolName, "BACKEND_ERROR");
                audit.append(callerLabel, toolName, "DENIED:BACKEND_ERROR:step7");
                String msg = output.boundAndRedact("backend error: " + safeMsg(cause));
                LOG.error("backend error tool={} msg={}", toolName, msg);
                return McpSchema.CallToolResult.builder().isError(true).addTextContent(msg).build();
            } catch (Exception e) {
                breaker.recordFailure(toolName);
                metrics.recordDenied(toolName, "BACKEND_ERROR");
                audit.append(callerLabel, toolName, "DENIED:BACKEND_ERROR:step7");
                String msg = output.boundAndRedact("backend error: " + safeMsg(e));
                LOG.error("backend error tool={} msg={}", toolName, msg);
                return McpSchema.CallToolResult.builder().isError(true).addTextContent(msg).build();
            }
            breaker.recordSuccess(toolName);

            String safe = output.boundAndRedact(body);
            long ms = (System.nanoTime() - started) / 1_000_000L;
            metrics.recordAllowed(toolName, ms);
            audit.append(callerLabel, toolName, "ALLOWED");
            LOG.info("allowed tool={} ms={}", toolName, ms);
            return McpSchema.CallToolResult.builder().addTextContent(safe).build();
        } finally {
            Trace.clear();
        }
    }

    public void shutdown(long timeoutMillis) {
        backendPool.shutdown();
        try {
            if (!backendPool.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) {
                backendPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            backendPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private McpSchema.CallToolResult deny(String toolName, String code, int step) {
        metrics.recordDenied(toolName, code);
        audit.append(callerLabel, toolName, "DENIED:" + code + ":step" + step);
        LOG.warn("denied tool={} code={} step={}", toolName, code, step);
        return McpSchema.CallToolResult.builder().isError(true).addTextContent("denied: " + code).build();
    }

    private static String arg(McpSchema.CallToolRequest request, String key) {
        Map<String, Object> args = request.arguments();
        if (args == null) {
            return null;
        }
        Object v = args.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static String safeMsg(Throwable e) {
        Throwable c = e.getCause();
        if (c != null && c != e) {
            if (c.getMessage() != null && !c.getMessage().isBlank()) {
                return c.getMessage();
            }
            return c.getClass().getName();
        }
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return e.getClass().getName();
    }
}
