package io.github.vaquarkhan.flinkmcp.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Viquar Khan
 */
public final class Inputs {

    public static final class InvalidInput extends RuntimeException {
        public InvalidInput(String message) {
            super(message);
        }
    }

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9._-]{1,256}");
    private static final Pattern INT = Pattern.compile("[0-9]{1,9}");

    private Inputs() {}

    public static String requireId(String id) {
        if (id == null || !ID.matcher(id).matches() || id.contains("..")) {
            throw new InvalidInput("invalid id: " + id);
        }
        return id;
    }

    public static String requireInt(String value) {
        if (value == null || !INT.matcher(value).matches()) {
            throw new InvalidInput("invalid int: " + value);
        }
        return value;
    }

    /**
     * Resolves {@code path} and requires it to be a regular file under one of the allow-listed
     * directories. Empty allow-list rejects all uploads (fail-closed when configured empty at call site
     * after checking config — callers pass the configured set).
     */
    public static Path requireJarPath(String path, Set<String> allowDirs) {
        if (path == null || path.isBlank()) {
            throw new InvalidInput("jar path required");
        }
        if (allowDirs == null || allowDirs.isEmpty()) {
            throw new InvalidInput("jar upload directories not configured (MCP_FLINK_JAR_UPLOAD_ALLOW_DIRS)");
        }
        try {
            Path resolved = Path.of(path).toAbsolutePath().normalize();
            if (!Files.isRegularFile(resolved)) {
                throw new InvalidInput("jar path is not a regular file");
            }
            boolean ok = false;
            for (String dir : allowDirs) {
                Path root = Path.of(dir).toAbsolutePath().normalize();
                if (resolved.startsWith(root)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                throw new InvalidInput("jar path outside allow-listed directories");
            }
            return resolved;
        } catch (InvalidInput e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidInput("invalid jar path: " + e.getMessage());
        }
    }

    public static String requireSql(String sql, int maxChars) {
        if (sql == null || sql.isBlank()) {
            throw new InvalidInput("sql required");
        }
        if (sql.length() > maxChars) {
            throw new InvalidInput("sql exceeds max length " + maxChars);
        }
        return sql;
    }

    public static String jsonEscape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
