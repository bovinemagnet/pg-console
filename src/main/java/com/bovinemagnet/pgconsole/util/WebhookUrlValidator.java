package com.bovinemagnet.pgconsole.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Validates outbound webhook URLs to prevent Server-Side Request Forgery (SSRF).
 * <p>
 * A URL is considered safe only when it uses {@code https} and its host resolves
 * exclusively to public, routable addresses. Loopback, link-local (including the
 * cloud metadata endpoint 169.254.169.254), site-local/private (RFC 1918) and
 * any-local addresses are rejected. Validation resolves the host, so substring
 * tricks such as {@code https://169.254.169.254/?x=discord.com/api/webhooks/}
 * cannot bypass it.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public final class WebhookUrlValidator {

    private WebhookUrlValidator() {
    }

    /**
     * Determines whether a webhook URL is safe to call from the server.
     *
     * @param url the candidate URL (may be null)
     * @return true only for https URLs whose host resolves to public addresses
     */
    public static boolean isSafe(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        final URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }

        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("https")) {
            return false;
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                return false;
            }
            for (InetAddress addr : addresses) {
                if (isBlocked(addr)) {
                    return false;
                }
            }
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private static boolean isBlocked(InetAddress addr) {
        return addr.isLoopbackAddress()
            || addr.isLinkLocalAddress()
            || addr.isSiteLocalAddress()
            || addr.isAnyLocalAddress()
            || addr.isMulticastAddress();
    }
}
