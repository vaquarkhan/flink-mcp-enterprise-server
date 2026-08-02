package io.github.vaquarkhan.flinkmcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vaquarkhan.flinkmcp.config.Config;
import io.github.vaquarkhan.flinkmcp.security.BearerAuthFilter;
import io.github.vaquarkhan.flinkmcp.util.Inputs;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Viquar Khan
 */
class ProductionHardeningTest {

    @Test
    void config_validateRejectsBadTransport() {
        Config c = Config.builder().defaults().transport("udp").build();
        assertThrows(IllegalArgumentException.class, c::validate);
    }

    @Test
    void config_writeEnabledRequiresSecret() {
        Config c = Config.builder().defaults().writeEnabled(true).approvalSecret(null).build();
        assertThrows(IllegalArgumentException.class, c::validate);
    }

    @Test
    void bearerAuth_acceptsValidToken() throws Exception {
        BearerAuthFilter filter = new BearerAuthFilter("secret");
        AtomicBoolean passed = new AtomicBoolean(false);
        StubReq req = new StubReq("Bearer secret");
        StubResp resp = new StubResp();
        filter.doFilter(req, resp, (r, s) -> passed.set(true));
        assertTrue(passed.get());
    }

    @Test
    void bearerAuth_rejectsWrongToken() throws Exception {
        BearerAuthFilter filter = new BearerAuthFilter("secret");
        AtomicBoolean passed = new AtomicBoolean(false);
        StubReq req = new StubReq("Bearer wrong");
        StubResp resp = new StubResp();
        filter.doFilter(req, resp, (r, s) -> passed.set(true));
        assertFalse(passed.get());
        assertEquals(401, resp.status);
        assertTrue(new String(resp.body.toByteArray(), StandardCharsets.UTF_8).contains("unauthorized"));
    }

    @Test
    void bearerAuth_rejectsMissingHeader() throws Exception {
        BearerAuthFilter filter = new BearerAuthFilter("secret");
        AtomicBoolean passed = new AtomicBoolean(false);
        StubReq req = new StubReq(null);
        StubResp resp = new StubResp();
        filter.doFilter(req, resp, (r, s) -> passed.set(true));
        assertFalse(passed.get());
        assertEquals(401, resp.status);
    }

    @Test
    void requireJarPath_enforcesAllowList(@TempDir Path dir) throws Exception {
        Path jar = dir.resolve("app.jar");
        Files.writeString(jar, "x");
        Path ok = Inputs.requireJarPath(jar.toString(), Set.of(dir.toString()));
        assertTrue(Files.isSameFile(ok, jar));
        assertThrows(Inputs.InvalidInput.class,
                () -> Inputs.requireJarPath(jar.toString(), Set.of(dir.resolve("other").toString())));
        assertThrows(Inputs.InvalidInput.class,
                () -> Inputs.requireJarPath(jar.toString(), Set.of()));
    }

    @Test
    void requireSql_enforcesMaxLength() {
        assertThrows(Inputs.InvalidInput.class, () -> Inputs.requireSql("SELECT 1", 3));
        assertEquals("SELECT 1", Inputs.requireSql("SELECT 1", 100));
    }

    private static final class StubReq implements HttpServletRequest {
        private final String auth;

        StubReq(String auth) {
            this.auth = auth;
        }

        @Override
        public String getHeader(String name) {
            return "Authorization".equalsIgnoreCase(name) ? auth : null;
        }

        @Override public Object getAttribute(String name) { return null; }
        @Override public java.util.Enumeration<String> getAttributeNames() { return java.util.Collections.emptyEnumeration(); }
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
        @Override public void setAttribute(String name, Object o) {}
        @Override public void removeAttribute(String name) {}
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
