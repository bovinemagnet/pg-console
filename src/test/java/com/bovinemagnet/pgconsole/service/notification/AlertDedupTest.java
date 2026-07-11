package com.bovinemagnet.pgconsole.service.notification;

import com.bovinemagnet.pgconsole.model.ActiveAlert;
import com.bovinemagnet.pgconsole.repository.ActiveAlertRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that re-firing the same alert condition deduplicates against the
 * existing active alert instead of creating a new one each time (M09).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("Alert deduplication")
class AlertDedupTest {

    @Test
    @DisplayName("Re-firing the same condition returns the existing alert, no second save")
    void reFireDeduplicates() {
        AlertManagementService service = new AlertManagementService();
        service.alertRepository = mock(ActiveAlertRepository.class);
        service.dispatcher = mock(NotificationDispatcher.class);

        // First fire: no existing alert; capture what gets saved.
        when(service.alertRepository.findByAlertId(anyString())).thenReturn(Optional.empty());
        when(service.alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ActiveAlert first = service.fireAlert("CPU", "HIGH", "cpu high", "prod", null);
        assertThat(first.getAlertId()).isEqualTo("CPU-prod");

        // Second fire of the same condition: the deterministic id now matches an
        // existing active alert, so no new alert is created.
        when(service.alertRepository.findByAlertId("CPU-prod")).thenReturn(Optional.of(first));

        ActiveAlert second = service.fireAlert("CPU", "HIGH", "cpu high", "prod", null);

        assertThat(second).isSameAs(first);
        // save() and dispatch() happened exactly once — only for the first fire.
        verify(service.alertRepository, times(1)).save(any());
        verify(service.dispatcher, times(1)).dispatch(any());
    }
}
