package io.github.vaquarkhan.flinkmcp.governance;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-tool circuit breaker. HALF_OPEN allows a single probe; success closes, failure re-opens.
 */
public final class CircuitBreaker {

    private enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final long resetMillis;
    private final ConcurrentHashMap<String, ToolState> states = new ConcurrentHashMap<>();

    public CircuitBreaker(int failureThreshold, long resetMillis) {
        this.failureThreshold = failureThreshold;
        this.resetMillis = resetMillis;
    }

    public boolean isOpen(String tool) {
        ToolState s = states.computeIfAbsent(tool, k -> new ToolState());
        synchronized (s) {
            if (s.state == State.OPEN) {
                if (System.currentTimeMillis() - s.openedAt >= resetMillis) {
                    s.state = State.HALF_OPEN;
                    s.halfOpenInFlight.set(false);
                    return false;
                }
                return true;
            }
            if (s.state == State.HALF_OPEN) {
                // only one concurrent probe
                return !s.halfOpenInFlight.compareAndSet(false, true);
            }
            return false;
        }
    }

    public void recordSuccess(String tool) {
        ToolState s = states.computeIfAbsent(tool, k -> new ToolState());
        synchronized (s) {
            s.failures = 0;
            s.state = State.CLOSED;
            s.halfOpenInFlight.set(false);
        }
    }

    public void recordFailure(String tool) {
        ToolState s = states.computeIfAbsent(tool, k -> new ToolState());
        synchronized (s) {
            s.failures++;
            if (s.state == State.HALF_OPEN || s.failures >= failureThreshold) {
                s.state = State.OPEN;
                s.openedAt = System.currentTimeMillis();
            }
            s.halfOpenInFlight.set(false);
        }
    }

    private static final class ToolState {
        State state = State.CLOSED;
        int failures;
        long openedAt;
        final AtomicBoolean halfOpenInFlight = new AtomicBoolean(false);
    }
}
