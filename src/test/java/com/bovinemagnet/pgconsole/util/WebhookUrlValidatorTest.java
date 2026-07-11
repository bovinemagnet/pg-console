package com.bovinemagnet.pgconsole.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests SSRF protection for outbound webhook URLs: only https URLs whose
 * host resolves to a public address are accepted; loopback, link-local and
 * private ranges are rejected.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("WebhookUrlValidator SSRF guard")
class WebhookUrlValidatorTest {

    @Test
    @DisplayName("Plain-http URL is rejected")
    void httpIsRejected() {
        assertThat(WebhookUrlValidator.isSafe("http://hooks.slack.com/services/x")).isFalse();
    }

    @Test
    @DisplayName("Loopback address is rejected")
    void loopbackIsRejected() {
        assertThat(WebhookUrlValidator.isSafe("https://127.0.0.1/x")).isFalse();
        assertThat(WebhookUrlValidator.isSafe("https://localhost/x")).isFalse();
    }

    @Test
    @DisplayName("Cloud metadata link-local address is rejected")
    void linkLocalMetadataIsRejected() {
        assertThat(WebhookUrlValidator.isSafe("https://169.254.169.254/latest/meta-data")).isFalse();
    }

    @Test
    @DisplayName("Private RFC1918 ranges are rejected")
    void privateRangesAreRejected() {
        assertThat(WebhookUrlValidator.isSafe("https://10.0.0.5/x")).isFalse();
        assertThat(WebhookUrlValidator.isSafe("https://192.168.1.1/x")).isFalse();
        assertThat(WebhookUrlValidator.isSafe("https://172.16.3.4/x")).isFalse();
    }

    @Test
    @DisplayName("Substring-laundered metadata host is still rejected")
    void launderedHostIsRejected() {
        // Host is the metadata IP; the discord.com string is only in the query.
        assertThat(WebhookUrlValidator.isSafe(
            "https://169.254.169.254/latest/?x=discord.com/api/webhooks/")).isFalse();
    }

    @Test
    @DisplayName("Malformed URL is rejected")
    void malformedIsRejected() {
        assertThat(WebhookUrlValidator.isSafe("not a url")).isFalse();
        assertThat(WebhookUrlValidator.isSafe(null)).isFalse();
        assertThat(WebhookUrlValidator.isSafe("")).isFalse();
    }

    @Test
    @DisplayName("A public https host is accepted")
    void publicHttpsIsAccepted() {
        // 1.1.1.1 is a public resolver address; no DNS lookup needed.
        assertThat(WebhookUrlValidator.isSafe("https://1.1.1.1/services/x")).isTrue();
    }
}
