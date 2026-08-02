package io.github.vaquarkhan.flinkmcp.observability;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public final class AuditLog {

    public record Entry(String record, String hash, String prevHash) {}

    private static final int MAX = 500;
    private final Object lock = new Object();
    private final List<Entry> entries = new ArrayList<>();
    private String prev = "";

    public void append(String caller, String tool, String outcome) {
        String trace = Trace.get();
        if (trace == null || trace.isBlank()) {
            trace = "-";
        }
        String record = Instant.now() + " | trace=" + trace + " | " + caller + " | " + tool + " | " + outcome;
        synchronized (lock) {
            String hash = sha256(prev + " | " + record);
            entries.add(new Entry(record, hash, prev));
            if (entries.size() > MAX) {
                entries.remove(0);
            }
            prev = hash;
        }
    }

    public List<String> recent() {
        synchronized (lock) {
            List<String> out = new ArrayList<>(entries.size());
            for (Entry e : entries) {
                out.add(e.hash().substring(0, 12) + "  " + e.record());
            }
            return out;
        }
    }

    public boolean verifyChain() {
        synchronized (lock) {
            String expectedPrev = "";
            for (Entry e : entries) {
                if (!expectedPrev.equals(e.prevHash())) {
                    return false;
                }
                String recomputed = sha256(e.prevHash() + " | " + e.record());
                if (!recomputed.equals(e.hash())) {
                    return false;
                }
                expectedPrev = e.hash();
            }
            return true;
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
