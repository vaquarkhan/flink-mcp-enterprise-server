package io.github.vaquarkhan.flinkmcp.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-caller outbound Flink / Gateway credentials (O2 phase B).
 * <p>
 * File format ({@code MCP_FLINK_CALLER_CREDENTIALS_FILE}), colon-separated:
 * {@code callerId : flinkAuthHeader : gatewayAuthHeader}
 * <p>
 * Use {@code -} for a field to fall back to the static server env header.
 * Auth values are typically {@code Bearer <token>} or {@code Basic <base64>}.
 *
 * @author Viquar Khan
 */
public final class CallerCredentials {

    public record Outbound(String flinkAuthHeader, String gatewayAuthHeader) {}

    private final Map<String, Outbound> byCallerId;

    private CallerCredentials(Map<String, Outbound> byCallerId) {
        this.byCallerId = Collections.unmodifiableMap(byCallerId);
    }

    public static CallerCredentials empty() {
        return new CallerCredentials(Map.of());
    }

    public static CallerCredentials load(Path file) throws IOException {
        if (file == null || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("caller credentials file missing or not a file: " + file);
        }
        Map<String, Outbound> map = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            // Split into at most 3 parts so "Bearer xxx:yyy" is not expected; scheme+token uses a space.
            String[] parts = line.split(":", 3);
            if (parts.length < 2) {
                throw new IllegalArgumentException(
                        "caller credentials line " + lineNo + ": expected callerId : flinkAuth [: gatewayAuth]");
            }
            String callerId = parts[0].trim();
            String flink = parts[1].trim();
            String gateway = parts.length >= 3 ? parts[2].trim() : "-";
            if (callerId.isBlank()) {
                throw new IllegalArgumentException("caller credentials line " + lineNo + ": blank callerId");
            }
            if (map.put(callerId, new Outbound(nullIfDash(flink), nullIfDash(gateway))) != null) {
                throw new IllegalArgumentException(
                        "caller credentials line " + lineNo + ": duplicate callerId " + callerId);
            }
        }
        return new CallerCredentials(map);
    }

    public CallerIdentity enrich(CallerIdentity identity) {
        Outbound o = byCallerId.get(identity.callerId());
        if (o == null) {
            return identity;
        }
        String flink = o.flinkAuthHeader() != null ? o.flinkAuthHeader() : identity.flinkAuthHeader();
        String gateway = o.gatewayAuthHeader() != null ? o.gatewayAuthHeader() : identity.gatewayAuthHeader();
        return identity.withOutboundAuth(flink, gateway);
    }

    public int size() {
        return byCallerId.size();
    }

    private static String nullIfDash(String v) {
        if (v == null || v.isBlank() || "-".equals(v.trim())) {
            return null;
        }
        return v.trim();
    }
}
