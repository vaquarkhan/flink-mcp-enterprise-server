package io.github.vaquarkhan.flinkmcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vaquarkhan.flinkmcp.config.Config;
import io.github.vaquarkhan.flinkmcp.governance.CircuitBreaker;
import io.github.vaquarkhan.flinkmcp.governance.Governance;
import io.github.vaquarkhan.flinkmcp.governance.OutputControls;
import io.github.vaquarkhan.flinkmcp.governance.RateLimiter;
import io.github.vaquarkhan.flinkmcp.governance.ToolClass;
import io.github.vaquarkhan.flinkmcp.observability.AuditLog;
import io.github.vaquarkhan.flinkmcp.observability.Metrics;
import io.github.vaquarkhan.flinkmcp.security.ApprovalTokens;
import io.github.vaquarkhan.flinkmcp.security.BearerAuthFilter;
import io.github.vaquarkhan.flinkmcp.security.CallerContext;
import io.github.vaquarkhan.flinkmcp.security.CallerIdentity;
import io.github.vaquarkhan.flinkmcp.security.NonceStore;
import io.github.vaquarkhan.flinkmcp.security.PolicyEngine;
import io.github.vaquarkhan.flinkmcp.security.TokenRegistry;
import io.github.vaquarkhan.flinkmcp.util.Inputs;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * O1 TLS config validation, O2 multi-token auth, and D2 breaker isolation for InvalidInput.
 *
 * @author Viquar Khan
 */
class OpenRemediationTest {

    @AfterEach
    void clearCaller() {
        CallerContext.clear();
    }

    @Test
    void o1_tlsEnabledRequiresKeystore() {
        Config c = Config.builder().defaults()
                .transport("http")
                .httpBearerToken("t")
                .httpTlsEnabled(true)
                .build();
        assertThrows(IllegalArgumentException.class, c::validate);
    }

    @Test
    void o1_tlsEnabledRequiresPassword() {
        Config c = Config.builder().defaults()
                .transport("http")
                .httpBearerToken("t")
                .httpTlsEnabled(true)
                .httpTlsKeystore("/tmp/keystore.p12")
                .httpTlsKeystorePassword(null)
                .build();
        assertThrows(IllegalArgumentException.class, c::validate);
    }

    @Test
    void o1_tlsConfiguredPassesValidation() {
        Config c = Config.builder().defaults()
                .transport("http")
                .httpBearerToken("t")
                .httpTlsEnabled(true)
                .httpTlsKeystore("/tmp/keystore.p12")
                .httpTlsKeystorePassword("changeit")
                .build();
        c.validate();
        assertTrue(c.httpTlsEnabled());
    }

    @Test
    void o2_tokenRegistry_authenticatesAndScopes(@TempDir Path dir) throws Exception {
        String tokenA = "alice-secret-token";
        String tokenB = "bob-secret-token";
        Path file = dir.resolve("tokens.txt");
        Files.writeString(file, String.join("\n",
                "# callerId : sha256 : jobs : jars : readonly",
                "alice : " + TokenRegistry.sha256Hex(tokenA) + " : job-a : * : false",
                "bob : " + TokenRegistry.sha256Hex(tokenB) + " : job-b : jar-b : true",
                ""));
        TokenRegistry registry = TokenRegistry.load(file);
        assertEquals(2, registry.size());
        assertEquals("alice", registry.authenticateBearerToken(tokenA).orElseThrow().callerId());
        assertTrue(registry.authenticateBearerToken(tokenA).orElseThrow().jobAllowed("job-a"));
        assertFalse(registry.authenticateBearerToken(tokenA).orElseThrow().jobAllowed("job-b"));
        assertTrue(registry.authenticateBearerToken(tokenB).orElseThrow().readonly());
        assertTrue(registry.authenticateBearerToken("nope").isEmpty());
    }

    @Test
    void o2_bearerFilter_setsCallerContext(@TempDir Path dir) throws Exception {
        String token = "scoped-token";
        Path file = dir.resolve("tokens.txt");
        Files.writeString(file, "ops : " + TokenRegistry.sha256Hex(token) + " : j1 : * : false\n");
        BearerAuthFilter filter = new BearerAuthFilter(TokenRegistry.load(file));
        AtomicBoolean passed = new AtomicBoolean(false);
        AtomicReference<CallerIdentity> seen = new AtomicReference<>();
        StubReq req = new StubReq("Bearer " + token);
        StubResp resp = new StubResp();
        filter.doFilter(req, resp, (r, s) -> {
            passed.set(true);
            seen.set(CallerContext.current().orElse(null));
        });
        assertTrue(passed.get());
        assertEquals("ops", seen.get().callerId());
        assertTrue(seen.get().jobAllowed("j1"));
        assertTrue(CallerContext.current().isEmpty());
    }

    @Test
    void o2_bearerFilter_rejectsUnknownToken(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("tokens.txt");
        Files.writeString(file, "ops : " + TokenRegistry.sha256Hex("good") + " : * : * : false\n");
        BearerAuthFilter filter = new BearerAuthFilter(TokenRegistry.load(file));
        AtomicBoolean passed = new AtomicBoolean(false);
        StubResp resp = new StubResp();
        filter.doFilter(new StubReq("Bearer bad"), resp, (r, s) -> passed.set(true));
        assertFalse(passed.get());
        assertEquals(401, resp.status);
    }

    @Test
    void o2_governance_usesCallerScope() {
        Config config = Config.builder().defaults()
                .allowedJobs(Set.of("*"))
                .build();
        Governance g = gov(config, new CircuitBreaker(5, 30_000));
        CallerContext.set(new CallerIdentity("alice", Set.of("job-a"), Set.of("*"), false));
        McpSchema.CallToolRequest bad = new McpSchema.CallToolRequest("get_job", Map.of("jobId", "job-b"));
        McpSchema.CallToolResult denied = g.run("get_job", ToolClass.READ, bad, () -> "should-not-run");
        assertTrue(Boolean.TRUE.equals(denied.isError()));
        assertTrue(text(denied).contains("SCOPE_DENIED"));

        McpSchema.CallToolRequest ok = new McpSchema.CallToolRequest("get_job", Map.of("jobId", "job-a"));
        McpSchema.CallToolResult allowed = g.run("get_job", ToolClass.READ, ok, () -> "ok");
        assertFalse(Boolean.TRUE.equals(allowed.isError()));
    }

    @Test
    void d2_invalidInput_doesNotOpenBreaker() {
        CircuitBreaker breaker = new CircuitBreaker(3, 60_000);
        Governance g = gov(Config.builder().defaults().build(), breaker);
        McpSchema.CallToolRequest req = new McpSchema.CallToolRequest("list_jobs", Map.of());
        for (int i = 0; i < 10; i++) {
            McpSchema.CallToolResult r = g.run("list_jobs", ToolClass.READ, req, () -> {
                throw new Inputs.InvalidInput("bad id");
            });
            assertTrue(Boolean.TRUE.equals(r.isError()));
            assertTrue(text(r).contains("INVALID_INPUT"));
        }
        assertFalse(breaker.isOpen("list_jobs"));

        for (int i = 0; i < 3; i++) {
            g.run("list_jobs", ToolClass.READ, req, () -> {
                throw new RuntimeException("backend down");
            });
        }
        assertTrue(breaker.isOpen("list_jobs"));
    }

    @Test
    void httpAuth_acceptsTokensFileWithoutBearerToken() {
        Config c = Config.builder().defaults()
                .transport("http")
                .authTokensFile("/tmp/tokens.txt")
                .build();
        assertTrue(c.httpAuthConfigured());
        c.validate();
    }

    private static Governance gov(Config config, CircuitBreaker breaker) {
        return new Governance(
                config,
                new RateLimiter(1000),
                breaker,
                new OutputControls(1_000_000, false),
                new AuditLog(),
                new ApprovalTokens("secret", new NonceStore()),
                new Metrics(),
                PolicyEngine.load(null),
                "test");
    }

    private static String text(McpSchema.CallToolResult r) {
        return ((McpSchema.TextContent) r.content().get(0)).text();
    }

    private static final class StubReq implements HttpServletRequest {
        private final String auth;
        private final Map<String, Object> attrs = new HashMap<>();

        StubReq(String auth) {
            this.auth = auth;
        }

        @Override
        public String getHeader(String name) {
            return "Authorization".equalsIgnoreCase(name) ? auth : null;
        }

        @Override public Object getAttribute(String name) { return attrs.get(name); }
        @Override public void setAttribute(String name, Object o) { attrs.put(name, o); }
        @Override public void removeAttribute(String name) { attrs.remove(name); }
        @Override public java.util.Enumeration<String> getAttributeNames() { return java.util.Collections.enumeration(attrs.keySet()); }
        @Override public String getCharacterEncoding() { return null; }
        @Override public void setCharacterEncoding(String env) {}
        @Override public int getContentLength() { return 0; }
        @Override public long getContentLengthLong() { return 0; }
        @Override public String getContentType() { return null; }
        @Override public jakarta.servlet.ServletInputStream getInputStream() { return null; }
        @Override public String getParameter(String name) { return null; }
        @Override public java.util.Enumeration<String> getParameterNames() { return null; }
        @Override public String[] getParameterValues(String name) { return null; }
        @Override public java.util.Map<String, String[]> getParameterMap() { return null; }
        @Override public String getProtocol() { return null; }
        @Override public String getScheme() { return null; }
        @Override public String getServerName() { return null; }
        @Override public int getServerPort() { return 0; }
        @Override public java.io.BufferedReader getReader() { return null; }
        @Override public String getRemoteAddr() { return null; }
        @Override public String getRemoteHost() { return null; }
        @Override public java.util.Locale getLocale() { return null; }
        @Override public java.util.Enumeration<java.util.Locale> getLocales() { return null; }
        @Override public boolean isSecure() { return false; }
        @Override public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }
        @Override public int getRemotePort() { return 0; }
        @Override public String getLocalName() { return null; }
        @Override public String getLocalAddr() { return null; }
        @Override public int getLocalPort() { return 0; }
        @Override public jakarta.servlet.ServletContext getServletContext() { return null; }
        @Override public jakarta.servlet.AsyncContext startAsync() { return null; }
        @Override public jakarta.servlet.AsyncContext startAsync(ServletRequest r, ServletResponse s) { return null; }
        @Override public boolean isAsyncStarted() { return false; }
        @Override public boolean isAsyncSupported() { return false; }
        @Override public jakarta.servlet.AsyncContext getAsyncContext() { return null; }
        @Override public jakarta.servlet.DispatcherType getDispatcherType() { return null; }
        @Override public String getRequestId() { return null; }
        @Override public String getProtocolRequestId() { return null; }
        @Override public jakarta.servlet.ServletConnection getServletConnection() { return null; }
        @Override public String getAuthType() { return null; }
        @Override public jakarta.servlet.http.Cookie[] getCookies() { return null; }
        @Override public long getDateHeader(String name) { return 0; }
        @Override public java.util.Enumeration<String> getHeaders(String name) { return null; }
        @Override public java.util.Enumeration<String> getHeaderNames() { return null; }
        @Override public int getIntHeader(String name) { return 0; }
        @Override public String getMethod() { return "GET"; }
        @Override public String getPathInfo() { return null; }
        @Override public String getPathTranslated() { return null; }
        @Override public String getContextPath() { return ""; }
        @Override public String getQueryString() { return null; }
        @Override public String getRemoteUser() { return null; }
        @Override public boolean isUserInRole(String role) { return false; }
        @Override public java.security.Principal getUserPrincipal() { return null; }
        @Override public String getRequestedSessionId() { return null; }
        @Override public String getRequestURI() { return "/"; }
        @Override public StringBuffer getRequestURL() { return new StringBuffer("http://localhost/"); }
        @Override public String getServletPath() { return "/"; }
        @Override public jakarta.servlet.http.HttpSession getSession(boolean create) { return null; }
        @Override public jakarta.servlet.http.HttpSession getSession() { return null; }
        @Override public String changeSessionId() { return null; }
        @Override public boolean isRequestedSessionIdValid() { return false; }
        @Override public boolean isRequestedSessionIdFromCookie() { return false; }
        @Override public boolean isRequestedSessionIdFromURL() { return false; }
        @Override public boolean authenticate(HttpServletResponse response) { return false; }
        @Override public void login(String username, String password) {}
        @Override public void logout() {}
        @Override public java.util.Collection<jakarta.servlet.http.Part> getParts() { return null; }
        @Override public jakarta.servlet.http.Part getPart(String name) { return null; }
        @Override public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
    }

    private static final class StubResp implements HttpServletResponse {
        int status = 200;
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        String contentType;

        @Override public void setStatus(int sc) { this.status = sc; }
        @Override public void setContentType(String type) { this.contentType = type; }
        @Override public jakarta.servlet.ServletOutputStream getOutputStream() {
            return new jakarta.servlet.ServletOutputStream() {
                @Override public boolean isReady() { return true; }
                @Override public void setWriteListener(jakarta.servlet.WriteListener writeListener) {}
                @Override public void write(int b) { body.write(b); }
            };
        }
        @Override public void addCookie(jakarta.servlet.http.Cookie cookie) {}
        @Override public boolean containsHeader(String name) { return false; }
        @Override public String encodeURL(String url) { return url; }
        @Override public String encodeRedirectURL(String url) { return url; }
        @Override public void sendError(int sc, String msg) { status = sc; }
        @Override public void sendError(int sc) { status = sc; }
        @Override public void sendRedirect(String location) {}
        @Override public void setDateHeader(String name, long date) {}
        @Override public void addDateHeader(String name, long date) {}
        @Override public void setHeader(String name, String value) {}
        @Override public void addHeader(String name, String value) {}
        @Override public void setIntHeader(String name, int value) {}
        @Override public void addIntHeader(String name, int value) {}
        @Override public int getStatus() { return status; }
        @Override public String getHeader(String name) { return null; }
        @Override public java.util.Collection<String> getHeaders(String name) { return java.util.List.of(); }
        @Override public java.util.Collection<String> getHeaderNames() { return java.util.List.of(); }
        @Override public String getCharacterEncoding() { return "UTF-8"; }
        @Override public String getContentType() { return contentType; }
        @Override public java.io.PrintWriter getWriter() { return new java.io.PrintWriter(body); }
        @Override public void setCharacterEncoding(String charset) {}
        @Override public void setContentLength(int len) {}
        @Override public void setContentLengthLong(long len) {}
        @Override public void setBufferSize(int size) {}
        @Override public int getBufferSize() { return 0; }
        @Override public void flushBuffer() {}
        @Override public void resetBuffer() {}
        @Override public boolean isCommitted() { return false; }
        @Override public void reset() {}
        @Override public void setLocale(java.util.Locale loc) {}
        @Override public java.util.Locale getLocale() { return java.util.Locale.ROOT; }
    }
}
