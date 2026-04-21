package com.bovinemagnet.pgconsole.util;

/**
 * Helpers for safely interpolating user-controllable values into filenames
 * and {@code Content-Disposition} headers.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public final class Filenames {

    private static final int MAX_LENGTH = 200;

    private Filenames() {
    }

    /**
     * Strips characters that could break a {@code Content-Disposition} header or
     * escape the filename context (CR, LF, quote, backslash, semicolon, slashes,
     * control chars), collapses whitespace, and bounds the length.
     * <p>
     * Safe to apply to an already-constructed filename like
     * {@code "slow-queries-<instance>-<timestamp>.csv"} — only dangerous
     * characters are replaced, so legitimate timestamps and extensions are
     * preserved.
     *
     * @param filename filename candidate (may contain untrusted segments); never {@code null}
     * @return a filename containing only safe characters, truncated to a sensible length
     */
    public static String sanitize(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "download";
        }

        StringBuilder out = new StringBuilder(filename.length());
        for (int i = 0; i < filename.length(); i++) {
            char c = filename.charAt(i);
            if (c < 0x20 || c == 0x7F || c == '"' || c == '\\' || c == ';'
                    || c == '/' || c == '\\' || c == '\r' || c == '\n') {
                out.append('_');
            } else {
                out.append(c);
            }
        }

        String cleaned = out.toString().trim();
        if (cleaned.isEmpty()) {
            return "download";
        }
        if (cleaned.length() > MAX_LENGTH) {
            cleaned = cleaned.substring(0, MAX_LENGTH);
        }
        return cleaned;
    }
}
