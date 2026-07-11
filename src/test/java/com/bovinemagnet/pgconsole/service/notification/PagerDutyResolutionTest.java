package com.bovinemagnet.pgconsole.service.notification;

import com.bovinemagnet.pgconsole.model.ActiveAlert;
import com.bovinemagnet.pgconsole.model.NotificationChannel;
import com.bovinemagnet.pgconsole.model.NotificationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies that resolving an alert on a PagerDuty channel with auto-resolve
 * disabled returns a non-null success result instead of NPE-ing on a null
 * HTTP response (M13).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("PagerDuty resolution no-op")
class PagerDutyResolutionTest {

    private NotificationChannel pagerDutyChannel(boolean autoResolve) {
        NotificationChannel channel = new NotificationChannel("pd", NotificationChannel.ChannelType.PAGERDUTY);
        channel.setId(1L);
        NotificationChannel.PagerDutyConfig config = new NotificationChannel.PagerDutyConfig();
        config.setRoutingKey("0123456789abcdef0123456789abcdef");
        config.setAutoResolve(autoResolve);
        channel.setPagerDutyConfig(config);
        return channel;
    }

    @Test
    @DisplayName("Resolution with auto-resolve off does not throw and returns success")
    void noOpResolutionReturnsSuccess() {
        PagerDutyNotificationSender sender = new PagerDutyNotificationSender();
        ActiveAlert alert = new ActiveAlert("cpu-prod", "CPU", "HIGH", "cpu high");

        NotificationResult[] result = new NotificationResult[1];
        assertThatCode(() -> result[0] = sender.sendResolution(pagerDutyChannel(false), alert))
            .doesNotThrowAnyException();

        assertThat(result[0]).isNotNull();
        assertThat(result[0].isSuccess()).isTrue();
    }
}
