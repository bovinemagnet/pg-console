package com.bovinemagnet.pgconsole.util;

/**
 * Escapes a value for safe inclusion in a CSV cell.
 * <p>
 * Applies two protections:
 * <ul>
 *   <li><b>Formula injection</b> — a leading {@code = + - @}, tab or carriage
 *       return is neutralised with a leading apostrophe so a spreadsheet does
 *       not evaluate the cell as a formula (e.g.
 *       {@code =HYPERLINK("http://evil","x")}).</li>
 *   <li><b>RFC 4180 quoting</b> — a value containing a comma, quote or newline
 *       is wrapped in double quotes with embedded quotes doubled.</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public final class CsvCell {

    private CsvCell() {
    }

    /**
     * Escapes a value for a CSV cell, neutralising spreadsheet formula triggers.
     *
     * @param value the value to escape (may be null)
     * @return the safe cell text, empty string for null
     */
    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        String result = value;
        char first = result.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            result = "'" + result;
        }

        if (result.indexOf(',') >= 0 || result.indexOf('"') >= 0
                || result.indexOf('\n') >= 0 || result.indexOf('\r') >= 0) {
            result = "\"" + result.replace("\"", "\"\"") + "\"";
        }

        return result;
    }
}
