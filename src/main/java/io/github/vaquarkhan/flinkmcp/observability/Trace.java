package io.github.vaquarkhan.flinkmcp.observability;

import java.security.SecureRandom;

public final class Trace {

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
        CURRENT.set(id);
        return id;
    }

    public static void set(String id) {
        CURRENT.set(id);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
