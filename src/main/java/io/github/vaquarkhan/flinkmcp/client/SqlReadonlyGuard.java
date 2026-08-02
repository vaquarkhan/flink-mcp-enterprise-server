package io.github.vaquarkhan.flinkmcp.client;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * @author Viquar Khan
 */
public final class SqlReadonlyGuard {

    private static final String[] ALLOWED = {"SELECT", "WITH", "SHOW", "DESCRIBE", "DESC", "EXPLAIN"};
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("--[^\\n]*");

    public boolean isReadOnly(String sql) {
        if (sql == null) {
            return false;
        }
        String cleaned = BLOCK_COMMENT.matcher(sql).replaceAll(" ");
        cleaned = LINE_COMMENT.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.trim();
        if (cleaned.isEmpty()) {
            return false;
        }
        // reject stacked statements: semicolon appears before the final one
        int lastSemi = cleaned.lastIndexOf(';');
        if (lastSemi >= 0) {
            String beforeLast = cleaned.substring(0, lastSemi);
            if (beforeLast.indexOf(';') >= 0) {
                return false;
            }
            if (lastSemi == cleaned.length() - 1) {
                cleaned = cleaned.substring(0, lastSemi).trim();
            } else {
                // semicolon mid-statement without being terminal stacked form
                return false;
            }
        }
        String upper = cleaned.toUpperCase(Locale.ROOT);
        for (String prefix : ALLOWED) {
            if (upper.equals(prefix)) {
                return true;
            }
            if (upper.startsWith(prefix + " ") || upper.startsWith(prefix + "\n") || upper.startsWith(prefix + "\t")) {
                return true;
            }
        }
        return false;
    }
}
