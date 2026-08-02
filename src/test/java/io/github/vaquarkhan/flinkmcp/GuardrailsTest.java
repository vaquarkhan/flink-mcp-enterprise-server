package io.github.vaquarkhan.flinkmcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vaquarkhan.flinkmcp.client.SqlReadonlyGuard;
import io.github.vaquarkhan.flinkmcp.config.Config;
import io.github.vaquarkhan.flinkmcp.governance.CircuitBreaker;
import io.github.vaquarkhan.flinkmcp.governance.OutputControls;
import io.github.vaquarkhan.flinkmcp.governance.RateLimiter;
import io.github.vaquarkhan.flinkmcp.security.ApprovalTokens;
import io.github.vaquarkhan.flinkmcp.security.NonceStore;
import org.junit.jupiter.api.Test;

/**
 * @author Viquar Khan
 */
class GuardrailsTest {

    @Test
    void approvalToken_validRoundTrip() {
        ApprovalTokens tokens = new ApprovalTokens("secret", new NonceStore());
        String t = tokens.mint("stop_job", "job-1", 60_000);
        assertTrue(tokens.verify(t, "stop_job", "job-1"));
    }

    @Test
    void approvalToken_rejectedForWrongTool() {
        ApprovalTokens tokens = new ApprovalTokens("secret", new NonceStore());
        String t = tokens.mint("stop_job", "job-1", 60_000);
        assertFalse(tokens.verify(t, "run_jar", "job-1"));
    }

    @Test
    void approvalToken_rejectsReplay() {
        ApprovalTokens tokens = new ApprovalTokens("secret", new NonceStore());
        String t = tokens.mint("stop_job", "job-1", 60_000);
        assertTrue(tokens.verify(t, "stop_job", "job-1"));
        assertFalse(tokens.verify(t, "stop_job", "job-1"));
    }

    @Test
    void approvalToken_rejectsExpired() throws Exception {
        ApprovalTokens tokens = new ApprovalTokens("secret", new NonceStore());
        String t = tokens.mint("stop_job", "job-1", 1);
        Thread.sleep(5);
        assertFalse(tokens.verify(t, "stop_job", "job-1"));
    }

    @Test
    void approvalToken_rejectsTampered() {
        ApprovalTokens tokens = new ApprovalTokens("secret", new NonceStore());
        String t = tokens.mint("stop_job", "job-1", 60_000);
        char flip = t.charAt(0) == 'A' ? 'B' : 'A';
        String tampered = flip + t.substring(1);
        assertFalse(tokens.verify(tampered, "stop_job", "job-1"));
    }

    @Test
    void approvalToken_rejectedWhenNoSecret() {
        ApprovalTokens tokens = new ApprovalTokens(null, new NonceStore());
        String t = tokens.mint("stop_job", "job-1", 60_000);
        assertFalse(tokens.verify(t, "stop_job", "job-1"));
    }

    @Test
    void approvalToken_differentSecretDoesNotVerify() {
        ApprovalTokens mint = new ApprovalTokens("secret-a", new NonceStore());
        ApprovalTokens verify = new ApprovalTokens("secret-b", new NonceStore());
        String t = mint.mint("stop_job", "job-1", 60_000);
        assertFalse(verify.verify(t, "stop_job", "job-1"));
    }

    @Test
    void approvalToken_scopeBinding_rejectsOtherResource() {
        ApprovalTokens tokens = new ApprovalTokens("secret", new NonceStore());
        String t = tokens.mint("stop_job", "jobA", 60_000);
        assertFalse(tokens.verify(t, "stop_job", "jobB"));
        // fresh token for positive case (nonce single-use)
        String t2 = tokens.mint("stop_job", "jobA", 60_000);
        assertTrue(tokens.verify(t2, "stop_job", "jobA"));
    }

    @Test
    void sqlGuard_allowsReads() {
        SqlReadonlyGuard g = new SqlReadonlyGuard();
        assertTrue(g.isReadOnly("SELECT 1"));
        assertTrue(g.isReadOnly("show tables"));
        assertTrue(g.isReadOnly("DESCRIBE t"));
        assertTrue(g.isReadOnly("WITH x AS (SELECT 1) SELECT * FROM x"));
    }

    @Test
    void sqlGuard_rejectsMutations() {
        SqlReadonlyGuard g = new SqlReadonlyGuard();
        assertFalse(g.isReadOnly("INSERT INTO t VALUES (1)"));
        assertFalse(g.isReadOnly("CREATE TABLE t (id INT)"));
        assertFalse(g.isReadOnly("DROP TABLE t"));
        assertFalse(g.isReadOnly("SELECT 1; SELECT 2"));
        assertFalse(g.isReadOnly(null));
    }

    @Test
    void outputControls_boundsAndRedacts() {
        OutputControls oc = new OutputControls(20, true);
        String out = oc.boundAndRedact("password=supersecret value that is long");
        assertTrue(out.contains("<redacted>") || out.contains("...<truncated>"));
        assertTrue(out.length() <= 20 + "...<truncated>".length());
    }

    @Test
    void outputControls_redactsEmailAndJwt() {
        OutputControls oc = new OutputControls(1_000_000, true);
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signaturepad";
        // need three JWT segments with enough length
        jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        String out = oc.boundAndRedact("user@example.com and " + jwt);
        assertTrue(out.contains("<redacted>"));
        assertFalse(out.contains("user@example.com"));
    }

    @Test
    void rateLimiter_blocksBurst() {
        RateLimiter rl = new RateLimiter(2);
        assertTrue(rl.allow());
        assertTrue(rl.allow());
        assertFalse(rl.allow());
    }

    @Test
    void circuitBreaker_opensAfterFailures() {
        CircuitBreaker cb = new CircuitBreaker(3, 30_000);
        assertFalse(cb.isOpen("t"));
        cb.recordFailure("t");
        cb.recordFailure("t");
        assertFalse(cb.isOpen("t"));
        cb.recordFailure("t");
        assertTrue(cb.isOpen("t"));
    }

    @Test
    void config_secureByDefault() {
        Config c = Config.builder().defaults().build();
        assertFalse(c.writesUnlocked());
        assertFalse(c.toolsAllowed().contains("stop_job"));
        assertTrue(c.toolsAllowed().contains("list_jobs"));
    }
}
