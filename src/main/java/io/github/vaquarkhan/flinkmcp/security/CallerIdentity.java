package io.github.vaquarkhan.flinkmcp.security;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Resolved HTTP (or default stdio) caller identity and per-caller scope.
 *
 * @author Viquar Khan
 */
public final class CallerIdentity {

    private final String callerId;
    private final Set<String> jobsAllow;
    private final Set<String> jarsAllow;
    private final boolean readonly;

    public CallerIdentity(String callerId, Set<String> jobsAllow, Set<String> jarsAllow, boolean readonly) {
        this.callerId = Objects.requireNonNull(callerId, "callerId");
        this.jobsAllow = Collections.unmodifiableSet(new LinkedHashSet<>(jobsAllow));
        this.jarsAllow = Collections.unmodifiableSet(new LinkedHashSet<>(jarsAllow));
        this.readonly = readonly;
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

    public boolean jobAllowed(String jobId) {
        return jobsAllow.contains("*") || jobsAllow.contains(jobId);
    }

    public boolean jarAllowed(String jarId) {
        return jarsAllow.contains("*") || jarsAllow.contains(jarId);
    }
}
