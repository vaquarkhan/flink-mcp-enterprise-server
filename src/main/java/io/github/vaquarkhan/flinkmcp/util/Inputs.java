package io.github.vaquarkhan.flinkmcp.util;

import java.util.regex.Pattern;

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
