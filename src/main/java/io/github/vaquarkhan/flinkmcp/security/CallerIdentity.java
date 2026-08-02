package io.github.vaquarkhan.flinkmcp.security;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Resolved HTTP (or default stdio) caller identity, scope, and optional outbound Flink credentials (O2B).
 *
 * @author Viquar Khan
 */
public final class CallerIdentity {

    private final String callerId;
    private final Set<String> jobsAllow;
    private final Set<String> jarsAllow;
    private final boolean readonly;
    private final String flinkAuthHeader;
    private final String gatewayAuthHeader;

    public CallerIdentity(String callerId, Set<String> jobsAllow, Set<String> jarsAllow, boolean readonly) {
        this(callerId, jobsAllow, jarsAllow, readonly, null, null);
    }

    public CallerIdentity(
            String callerId,
            Set<String> jobsAllow,
            Set<String> jarsAllow,
            boolean readonly,
            String flinkAuthHeader,
            String gatewayAuthHeader) {
        this.callerId = Objects.requireNonNull(callerId, "callerId");
        this.jobsAllow = Collections.unmodifiableSet(new LinkedHashSet<>(jobsAllow));
        this.jarsAllow = Collections.unmodifiableSet(new LinkedHashSet<>(jarsAllow));
        this.readonly = readonly;
        this.flinkAuthHeader = blankToNull(flinkAuthHeader);
        this.gatewayAuthHeader = blankToNull(gatewayAuthHeader);
    }

    public CallerIdentity withOutboundAuth(String flinkAuthHeader, String gatewayAuthHeader) {
        return new CallerIdentity(
                callerId, jobsAllow, jarsAllow, readonly, flinkAuthHeader, gatewayAuthHeader);
    }

    public String callerId() {
        return callerId;
    }

    public Set<String> jobsAllow() {
        return jobsAllow;
    }

    public Set<String> jarsAllow() {
        return jarsAllow;
    }

    public boolean readonly() {
        return readonly;
    }

    /** Optional outbound Authorization for Flink REST (e.g. {@code Bearer …} / {@code Basic …}). */
    public String flinkAuthHeader() {
        return flinkAuthHeader;
    }

    /** Optional outbound Authorization for SQL Gateway. */
    public String gatewayAuthHeader() {
        return gatewayAuthHeader;
    }

    public boolean jobAllowed(String jobId) {
        return jobsAllow.contains("*") || jobsAllow.contains(jobId);
    }

    public boolean jarAllowed(String jarId) {
        return jarsAllow.contains("*") || jarsAllow.contains(jarId);
    }

    private static String blankToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        if (t.isEmpty() || "-".equals(t)) {
            return null;
        }
        return t;
    }
}
