package com.bovinemagnet.pgconsole.util;

/**
 * Minimal HTML escape helper for contexts where Qute auto-escaping is not in play
 * (e.g. Java code that assembles HTML fragments via {@code StringBuilder}).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public final class HtmlText {

    private HtmlText() {
    }

    /**
     * Escapes HTML-significant characters in {@code value.toString()}.
     * Returns an empty string when {@code value} is {@code null}.
     *
     * @param value value to escape (may be {@code null})
     * @return an HTML-safe representation of the value
     */
    public static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
