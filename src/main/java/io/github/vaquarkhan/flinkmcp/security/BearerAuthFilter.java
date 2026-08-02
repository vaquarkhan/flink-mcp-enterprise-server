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

public final class BearerAuthFilter implements Filter {

    private final byte[] expected;

    public BearerAuthFilter(String token) {
        this.expected = ("Bearer " + token).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String auth = req.getHeader("Authorization");
        byte[] provided = auth == null ? new byte[0] : auth.getBytes(StandardCharsets.UTF_8);
        if (!constantTimeEquals(expected, provided)) {
            resp.setStatus(401);
            resp.setContentType("application/json");
            resp.getOutputStream().write("{\"error\":\"unauthorized\"}".getBytes(StandardCharsets.UTF_8));
            return;
        }
        chain.doFilter(request, response);
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
