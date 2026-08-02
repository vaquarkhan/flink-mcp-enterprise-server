package io.github.vaquarkhan.flinkmcp.governance;

import java.util.regex.Pattern;

/**
 * @author Viquar Khan
 */
public final class OutputControls {

    private static final Pattern[] DLP = {
            Pattern.compile("(?i)(api[_-]?key|secret|password|passwd|token)\\s*[=:]\\s*\"?[^\"\\s,}]+"),
            Pattern.compile("eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}"),
            Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----"),
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"),
            Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._-]+")
    };

    private final int maxBytes;
    private final boolean dlpEnabled;

    public OutputControls(int maxBytes, boolean dlpEnabled) {
        this.maxBytes = maxBytes;
        this.dlpEnabled = dlpEnabled;
    }

    public String boundAndRedact(String input) {
        String s = input == null ? "" : input;
        if (dlpEnabled) {
            for (Pattern p : DLP) {
                s = p.matcher(s).replaceAll("<redacted>");
            }
        }
        if (s.length() > maxBytes) {
            s = s.substring(0, maxBytes) + "...<truncated>";
        }
        return s;
    }
}
