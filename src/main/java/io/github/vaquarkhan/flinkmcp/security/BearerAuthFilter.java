package io.github.vaquarkhan.flinkmcp.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.Set;

/**
 * Fail-closed bearer auth. Supports a single shared token or a hashed multi-caller registry.
 *
 * @author Viquar Khan
 */
public final class BearerAuthFilter implements Filter {

    public static final String ATTR_CALLER = "flinkmcp.caller";

    private final byte[] expectedBearerHeader;
    private final CallerIdentity singleCaller;
    private final TokenRegistry registry;

    /** Single shared bearer token (legacy mode). */
    public BearerAuthFilter(String token) {
        this(token, "http", Set.of("*"), Set.of("*"), false);
    }

    public BearerAuthFilter(
            String token, String callerId, Set<String> jobs, Set<String> jars, boolean readonly) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("bearer token required");
        }
        this.expectedBearerHeader = ("Bearer " + token).getBytes(StandardCharsets.UTF_8);
        this.singleCaller = new CallerIdentity(callerId, jobs, jars, readonly);
        this.registry = null;
    }

    /** Multi-caller registry (O2 phase A). */
    public BearerAuthFilter(TokenRegistry registry) {
        if (registry == null || registry.size() == 0) {
            throw new IllegalArgumentException("token registry required");
        }
        this.expectedBearerHeader = null;
        this.singleCaller = null;
        this.registry = registry;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String auth = req.getHeader("Authorization");
        Optional<CallerIdentity> identity = resolve(auth);
        if (identity.isEmpty()) {
            resp.setStatus(401);
            resp.setContentType("application/json");
            resp.getOutputStream().write("{\"error\":\"unauthorized\"}".getBytes(StandardCharsets.UTF_8));
            return;
        }
        CallerIdentity caller = identity.get();
        req.setAttribute(ATTR_CALLER, caller);
        CallerContext.set(caller);
        try {
            chain.doFilter(request, response);
        } finally {
            CallerContext.clear();
        }
    }

    private Optional<CallerIdentity> resolve(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return Optional.empty();
        }
        if (registry != null) {
            if (!authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return Optional.empty();
            }
            String token = authHeader.substring(7).trim();
            return registry.authenticateBearerToken(token);
        }
        byte[] provided = authHeader.getBytes(StandardCharsets.UTF_8);
        if (!constantTimeEquals(expectedBearerHeader, provided)) {
            return Optional.empty();
        }
        return Optional.of(singleCaller);
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        int len = Math.max(a.length, b.length);
        byte[] aa = new byte[len];
        byte[] bb = new byte[len];
        System.arraycopy(a, 0, aa, 0, a.length);
        System.arraycopy(b, 0, bb, 0, b.length);
        int diff = a.length ^ b.length;
        if (!MessageDigest.isEqual(aa, bb)) {
            diff |= 1;
        }
        return diff == 0;
    }
}
