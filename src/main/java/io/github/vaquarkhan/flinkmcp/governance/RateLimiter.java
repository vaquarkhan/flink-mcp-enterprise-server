package io.github.vaquarkhan.flinkmcp.governance;

public final class RateLimiter {

    private final int maxPerSecond;
    private long windowStartMs;
    private int count;

    public RateLimiter(int maxPerSecond) {
        this.maxPerSecond = maxPerSecond;
        this.windowStartMs = System.currentTimeMillis();
        this.count = 0;
    }

    public synchronized boolean allow() {
        long now = System.currentTimeMillis();
        if (now - windowStartMs >= 1000L) {
            windowStartMs = now;
            count = 0;
        }
        if (count >= maxPerSecond) {
            return false;
        }
        count++;
        return true;
    }
}
