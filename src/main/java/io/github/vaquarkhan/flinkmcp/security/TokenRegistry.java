package io.github.vaquarkhan.flinkmcp.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Named HTTP callers loaded from {@code MCP_FLINK_AUTH_TOKENS_FILE}.
 * <p>
 * Line format (colon-separated):
 * {@code callerId : sha256Hex(token) : jobsAllowCsv : jarsAllowCsv : readonly
 *   [: flinkAuthHeader [: gatewayAuthHeader]]}
 * <p>
 * Inbound MCP tokens are stored as hashes only. Optional fields 6–7 are outbound
 * Flink/Gateway Authorization values (O2B); prefer {@link CallerCredentials} file
 * when secrets should stay out of the tokens file.
 *
 * @author Viquar Khan
 */
public final class TokenRegistry {

    private final Map<String, CallerIdentity> byTokenHash;

    private TokenRegistry(Map<String, CallerIdentity> byTokenHash) {
        this.byTokenHash = Collections.unmodifiableMap(byTokenHash);
    }

    public static TokenRegistry load(Path file) throws IOException {
        if (file == null || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("auth tokens file missing or not a file: " + file);
        }
        Map<String, CallerIdentity> map = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = Arrays.stream(line.split(":", -1)).map(String::trim).toArray(String[]::new);
            if (parts.length < 5 || parts.length > 7) {
                throw new IllegalArgumentException(
                        "auth tokens file line " + lineNo + ": expected 5..7 colon fields, got " + parts.length);
            }
            String callerId = parts[0];
            String hash = parts[1].toLowerCase(Locale.ROOT);
            if (callerId.isBlank() || !hash.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException(
                        "auth tokens file line " + lineNo + ": invalid callerId or sha256 hex");
            }
            Set<String> jobs = parseCsv(parts[2]);
            Set<String> jars = parseCsv(parts[3]);
            boolean readonly = Boolean.parseBoolean(parts[4]);
            String flinkAuth = parts.length >= 6 ? parts[5] : null;
            String gatewayAuth = parts.length >= 7 ? parts[6] : null;
            if (jobs.isEmpty() || jars.isEmpty()) {
                throw new IllegalArgumentException(
                        "auth tokens file line " + lineNo + ": jobs/jars allow lists must not be empty");
            }
            CallerIdentity identity = new CallerIdentity(callerId, jobs, jars, readonly, flinkAuth, gatewayAuth);
            if (map.put(hash, identity) != null) {
                throw new IllegalArgumentException(
                        "auth tokens file line " + lineNo + ": duplicate token hash");
            }
        }
        if (map.isEmpty()) {
            throw new IllegalArgumentException("auth tokens file has no entries: " + file);
        }
        return new TokenRegistry(map);
    }

    /** Overlay per-caller outbound credentials (credentials file wins when set). */
    public TokenRegistry withCredentials(CallerCredentials credentials) {
        if (credentials == null || credentials.size() == 0) {
            return this;
        }
        Map<String, CallerIdentity> enriched = new LinkedHashMap<>();
        for (Map.Entry<String, CallerIdentity> e : byTokenHash.entrySet()) {
            enriched.put(e.getKey(), credentials.enrich(e.getValue()));
        }
        return new TokenRegistry(enriched);
    }

    public Optional<CallerIdentity> authenticateBearerToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String hash = sha256Hex(rawToken);
        for (Map.Entry<String, CallerIdentity> e : byTokenHash.entrySet()) {
            if (constantTimeEquals(e.getKey(), hash)) {
                return Optional.of(e.getValue());
            }
        }
        return Optional.empty();
    }

    public int size() {
        return byTokenHash.size();
    }

    public static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static Set<String> parseCsv(String csv) {
        Set<String> set = new LinkedHashSet<>();
        if (csv == null || csv.isBlank()) {
            return set;
        }
        for (String part : csv.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                set.add(t);
            }
        }
        return set;
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] aa = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        int len = Math.max(aa.length, bb.length);
        byte[] left = new byte[len];
        byte[] right = new byte[len];
        System.arraycopy(aa, 0, left, 0, aa.length);
        System.arraycopy(bb, 0, right, 0, bb.length);
        int diff = aa.length ^ bb.length;
        if (!MessageDigest.isEqual(left, right)) {
            diff |= 1;
        }
        return diff == 0;
    }
}
