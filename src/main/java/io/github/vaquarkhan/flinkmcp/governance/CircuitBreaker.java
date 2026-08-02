package io.github.vaquarkhan.flinkmcp.governance;

import java.util.concurrent.ConcurrentHashMap;

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
                    return false;
                }
                return true;
            }
            return false;
        }
    }

    public void recordSuccess(String tool) {
        ToolState s = states.computeIfAbsent(tool, k -> new ToolState());
        synchronized (s) {
            s.failures = 0;
            s.state = State.CLOSED;
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
        }
    }

    private static final class ToolState {
        State state = State.CLOSED;
        int failures;
        long openedAt;
    }
}
