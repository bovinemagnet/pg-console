package com.bovinemagnet.pgconsole.service.notification;

import com.bovinemagnet.pgconsole.model.ActiveAlert;
import com.bovinemagnet.pgconsole.model.NotificationChannel;
import com.bovinemagnet.pgconsole.model.NotificationResult;
import com.bovinemagnet.pgconsole.repository.AlertSilenceRepository;
import com.bovinemagnet.pgconsole.repository.MaintenanceWindowRepository;
import com.bovinemagnet.pgconsole.repository.NotificationChannelRepository;
import com.bovinemagnet.pgconsole.repository.NotificationHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies escalation dispatch honours alert-level suppression (M10) and that
 * test-mode channels are never actually sent to (M11).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("NotificationDispatcher gating")
class NotificationDispatcherGatingTest {

    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new NotificationDispatcher();
        dispatcher.channelRepository = mock(NotificationChannelRepository.class);
        dispatcher.historyRepository = mock(NotificationHistoryRepository.class);
        dispatcher.maintenanceWindowRepository = mock(MaintenanceWindowRepository.class);
        dispatcher.alertSilenceRepository = mock(AlertSilenceRepository.class);
    }

    private ActiveAlert alert() {
        return new ActiveAlert("cpu-prod", "CPU", "HIGH", "cpu high");
    }

    @Test
    @DisplayName("A silenced alert produces no escalation notifications")
    void silencedAlertNotEscalated() {
        when(dispatcher.maintenanceWindowRepository.shouldSuppress(any(), any())).thenReturn(false);
        when(dispatcher.alertSilenceRepository.isSilenced(anyString(), anyString(), any(), any()))
            .thenReturn(true);

        List<NotificationResult> results = dispatcher.dispatchToChannels(alert(), List.of(1L));

        assertThat(results).isEmpty();
        verify(dispatcher.channelRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("An alert in a maintenance window produces no escalation notifications")
    void maintenanceAlertNotEscalated() {
        when(dispatcher.maintenanceWindowRepository.shouldSuppress(any(), any())).thenReturn(true);

        List<NotificationResult> results = dispatcher.dispatchToChannels(alert(), List.of(1L));

        assertThat(results).isEmpty();
        verify(dispatcher.channelRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("A test-mode channel logs a resolution but is never sent")
    void testModeChannelResolutionNotSent() {
        NotificationChannel channel = new NotificationChannel("pd", NotificationChannel.ChannelType.SLACK);
        channel.setId(1L);
        channel.setEnabled(true);
        channel.setTestMode(true);
        when(dispatcher.channelRepository.findEnabled()).thenReturn(List.of(channel));

        List<NotificationResult> results = dispatcher.dispatchResolution(alert(), null);

        assertThat(results).isEmpty();
        verify(dispatcher.historyRepository, never()).save(any());
    }
}
