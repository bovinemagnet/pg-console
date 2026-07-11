package com.bovinemagnet.pgconsole.service.notification;

import com.bovinemagnet.pgconsole.model.ActiveAlert;
import com.bovinemagnet.pgconsole.model.NotificationChannel;
import com.bovinemagnet.pgconsole.model.NotificationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the JSON string escaper neutralises quotes and control characters so
 * an attacker-supplied field cannot break out of a hand-built webhook payload
 * (M07).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("Notification JSON escaping")
class JsonEscapeTest {

    /** Minimal concrete sender exposing the protected escapeJson helper. */
    private static class ExposingSender extends AbstractNotificationSender {
        String esc(String s) {
            return escapeJson(s);
        }

        @Override
        public NotificationChannel.ChannelType getChannelType() {
            return NotificationChannel.ChannelType.SLACK;
        }

        @Override
        public NotificationResult send(NotificationChannel channel, ActiveAlert alert) {
            return null;
        }

        @Override
        public NotificationResult sendTest(NotificationChannel channel) {
            return null;
        }

        @Override
        public boolean validateConfig(NotificationChannel channel) {
            return true;
        }
    }

    private final ExposingSender sender = new ExposingSender();

    @Test
    @DisplayName("A quote is escaped so it cannot close the string")
    void quoteEscaped() {
        assertThat(sender.esc("HIGH\",\"channel\":\"#general"))
            .isEqualTo("HIGH\\\",\\\"channel\\\":\\\"#general");
    }

    @Test
    @DisplayName("Control characters are escaped as uXXXX")
    void controlCharsEscaped() {
        // A bare NUL (0x00) and vertical tab (0x0B) must not appear literally.
        String input = "a" + ((char) 0x00) + "b" + ((char) 0x0B) + "c";
        assertThat(sender.esc(input)).isEqualTo("a\\u0000b\\u000bc");
    }

    @Test
    @DisplayName("Backslash is doubled")
    void backslashDoubled() {
        assertThat(sender.esc("a\\b")).isEqualTo("a\\\\b");
    }

    @Test
    @DisplayName("Null becomes empty string")
    void nullEmpty() {
        assertThat(sender.esc(null)).isEmpty();
    }
}
