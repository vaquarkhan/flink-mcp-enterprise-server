package io.github.vaquarkhan.flinkmcp.observability;

import java.security.SecureRandom;
import org.slf4j.MDC;

/**
 * Per-request correlation id. Stored in a ThreadLocal and mirrored into SLF4J MDC
 * key {@code trace} so every log line is correlatable (stderr-only logging).
 */
public final class Trace {

    public static final String MDC_KEY = "trace";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();
    private static final SecureRandom RNG = new SecureRandom();

    private Trace() {}

    public static String newId() {
        byte[] bytes = new byte[8];
        RNG.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(16);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        String id = sb.toString();
        set(id);
        return id;
    }

    public static void set(String id) {
        CURRENT.set(id);
        if (id == null || id.isBlank()) {
            MDC.remove(MDC_KEY);
        } else {
            MDC.put(MDC_KEY, id);
        }
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
        MDC.remove(MDC_KEY);
    }
}
