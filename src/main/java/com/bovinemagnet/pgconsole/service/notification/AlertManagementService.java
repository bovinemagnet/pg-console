package com.bovinemagnet.pgconsole.service.notification;

import com.bovinemagnet.pgconsole.model.ActiveAlert;
import com.bovinemagnet.pgconsole.model.AlertSilence;
import com.bovinemagnet.pgconsole.model.MaintenanceWindow;
import com.bovinemagnet.pgconsole.model.NotificationResult;
import com.bovinemagnet.pgconsole.repository.ActiveAlertRepository;
import com.bovinemagnet.pgconsole.repository.AlertSilenceRepository;
import com.bovinemagnet.pgconsole.repository.MaintenanceWindowRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Central service for managing alerts, silences, and maintenance windows.
 * <p>
 * Provides a unified API for:
 * <ul>
 *   <li>Creating and managing active alerts</li>
 *   <li>Acknowledging and resolving alerts</li>
 *   <li>Managing alert silences</li>
 *   <li>Managing maintenance windows</li>
 *   <li>Triggering notifications through the dispatcher</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class AlertManagementService {

	private static final Logger LOG = Logger.getLogger(AlertManagementService.class);

	@Inject
	ActiveAlertRepository alertRepository;

	@Inject
	AlertSilenceRepository silenceRepository;

	@Inject
	MaintenanceWindowRepository maintenanceRepository;

	@Inject
	NotificationDispatcher dispatcher;

	@Inject
	EscalationService escalationService;

	// ===== Alert Management =====

	/**
	 * Creates a new alert and dispatches notifications.
	 *
	 * @param alertType type of alert (e.g., "HIGH_CPU", "SLOW_QUERY")
	 * @param severity severity level (CRITICAL, HIGH, MEDIUM, LOW)
	 * @param message alert message
	 * @param instanceName optional instance name
	 * @return the created alert
	 */
	public ActiveAlert fireAlert(String alertType, String severity, String message, String instanceName) {
		return fireAlert(alertType, severity, message, instanceName, null);
	}

	/**
	 * Creates a new alert with escalation policy and dispatches notifications.
	 *
	 * @param alertType type of alert
	 * @param severity severity level
	 * @param message alert message
	 * @param instanceName optional instance name
	 * @param escalationPolicyId optional escalation policy ID
	 * @return the created alert
	 */
	public ActiveAlert fireAlert(String alertType, String severity, String message, String instanceName, Long escalationPolicyId) {
		// Generate unique alert ID
		String alertId = generateAlertId(alertType, instanceName);

		// Check for existing active alert with same ID (deduplication)
		Optional<ActiveAlert> existing = alertRepository.findByAlertId(alertId);
		if (existing.isPresent()) {
			LOG.debugf("Alert %s already active, skipping duplicate", alertId);
			return existing.get();
		}

		// Create new alert
		ActiveAlert alert = new ActiveAlert(alertId, alertType, severity, message);
		alert.setInstanceName(instanceName);
		alert.setEscalationPolicyId(escalationPolicyId);
		alert.setFiredAt(Instant.now());

		// Save alert
		alertRepository.save(alert);
		LOG.infof("Fired new alert: %s [%s] - %s", alertId, severity, alertType);

		// Dispatch notifications
		if (escalationPolicyId != null) {
			escalationService.assignPolicyAndNotify(alert, escalationPolicyId);
		} else {
			dispatcher.dispatch(alert);
		}

		return alert;
	}

	/**
	 * Acknowledges an alert, stopping escalation.
	 *
	 * @param alertId the database ID of the alert
	 * @param acknowledgedBy optional user who acknowledged
	 */
	public void acknowledgeAlert(Long alertId, String acknowledgedBy) {
		alertRepository
			.findById(alertId)
			.ifPresent(alert -> {
				if (!alert.isAcknowledged()) {
					alertRepository.acknowledge(alertId);
					LOG.infof("Alert %s acknowledged by %s", alert.getAlertId(), acknowledgedBy != null ? acknowledgedBy : "system");
				}
			});
	}

	/**
	 * Resolves an alert and optionally sends resolution notifications.
	 *
	 * @param alertId the database ID of the alert
	 * @param resolvedBy optional user who resolved
	 * @param sendResolution whether to send resolution notifications
	 * @return list of notification results if sendResolution is true
	 */
	public List<NotificationResult> resolveAlert(Long alertId, String resolvedBy, boolean sendResolution) {
		Optional<ActiveAlert> alertOpt = alertRepository.findById(alertId);
		if (alertOpt.isEmpty()) {
			return List.of();
		}

		ActiveAlert alert = alertOpt.get();
		if (!alert.isResolved()) {
			alert.setResolved(true);
			alert.setResolvedAt(Instant.now());
			alertRepository.update(alert);
			LOG.infof("Alert %s resolved by %s", alert.getAlertId(), resolvedBy != null ? resolvedBy : "system");

			if (sendResolution) {
				return dispatcher.dispatchResolution(alert, null);
			}
		}

		return List.of();
	}

	/**
	 * Gets all active (unresolved) alerts.
	 *
	 * @return list of active alerts
	 */
	public List<ActiveAlert> getActiveAlerts() {
		return alertRepository.findActive();
	}

	/**
	 * Gets recent alerts including resolved.
	 *
	 * @param limit maximum number to return
	 * @return list of recent alerts
	 */
	public List<ActiveAlert> getRecentAlerts(int limit) {
		return alertRepository.findAll(limit);
	}

	/**
	 * Gets an alert by database ID.
	 *
	 * @param id the database ID
	 * @return optional containing the alert if found
	 */
	public Optional<ActiveAlert> getAlert(Long id) {
		return alertRepository.findById(id);
	}

	/**
	 * Gets an alert by alert ID string.
	 *
	 * @param alertId the alert ID string
	 * @return optional containing the alert if found
	 */
	public Optional<ActiveAlert> getAlertByAlertId(String alertId) {
		return alertRepository.findByAlertId(alertId);
	}

	// ===== Silence Management =====

	/**
	 * Creates a new alert silence.
	 *
	 * @param silence the silence to create
	 * @return the created silence with ID
	 */
	public AlertSilence createSilence(AlertSilence silence) {
		AlertSilence saved = silenceRepository.save(silence);
		LOG.infof("Created alert silence: %s until %s", silence.getName(), silence.getEndTimeFormatted());
		return saved;
	}

	/**
	 * Creates a quick silence for a specific alert.
	 *
	 * @param alertType type of alert to silence
	 * @param instanceName optional instance to silence
	 * @param durationMinutes duration in minutes
	 * @param createdBy optional user who created
	 * @return the created silence
	 */
	public AlertSilence createQuickSilence(String alertType, String instanceName, int durationMinutes, String createdBy) {
		AlertSilence silence = new AlertSilence("Quick silence: " + alertType, Instant.now().plus(Duration.ofMinutes(durationMinutes)));
		silence.setCreatedBy(createdBy);

		if (alertType != null) {
			silence.addMatcher(new AlertSilence.Matcher("alertType", AlertSilence.Matcher.Operator.EQUALS, alertType));
		}

		if (instanceName != null) {
			silence.addMatcher(new AlertSilence.Matcher("instanceName", AlertSilence.Matcher.Operator.EQUALS, instanceName));
		}

		return silenceRepository.save(silence);
	}

	/**
	 * Gets all active silences.
	 *
	 * @return list of active silences
	 */
	public List<AlertSilence> getActiveSilences() {
		return silenceRepository.findActive();
	}

	/**
	 * Gets all silences.
	 *
	 * @return list of all silences
	 */
	public List<AlertSilence> getAllSilences() {
		return silenceRepository.findAll();
	}

	/**
	 * Gets a silence by ID.
	 *
	 * @param id the silence ID
	 * @return optional containing the silence if found
	 */
	public Optional<AlertSilence> getSilence(Long id) {
		return silenceRepository.findById(id);
	}

	/**
	 * Expires a silence immediately.
	 *
	 * @param id the silence ID to expire
	 */
	public void expireSilence(Long id) {
		silenceRepository.expire(id);
		LOG.infof("Expired silence %d", id);
	}

	/**
	 * Deletes a silence.
	 *
	 * @param id the silence ID to delete
	 */
	public void deleteSilence(Long id) {
		silenceRepository.delete(id);
	}

	// ===== Maintenance Window Management =====

	/**
	 * Creates a new maintenance window.
	 *
	 * @param window the window to create
	 * @return the created window with ID
	 */
	public MaintenanceWindow createMaintenanceWindow(MaintenanceWindow window) {
		MaintenanceWindow saved = maintenanceRepository.save(window);
		LOG.infof("Created maintenance window: %s from %s to %s", window.getName(), window.getStartTimeFormatted(), window.getEndTimeFormatted());
		return saved;
	}

	/**
	 * Gets all maintenance windows.
	 *
	 * @return list of all maintenance windows
	 */
	public List<MaintenanceWindow> getAllMaintenanceWindows() {
		return maintenanceRepository.findAll();
	}

	/**
	 * Gets active maintenance windows.
	 *
	 * @return list of active maintenance windows
	 */
	public List<MaintenanceWindow> getActiveMaintenanceWindows() {
		return maintenanceRepository.findActive();
	}

	/**
	 * Gets upcoming maintenance windows.
	 *
	 * @return list of upcoming maintenance windows
	 */
	public List<MaintenanceWindow> getUpcomingMaintenanceWindows() {
		return maintenanceRepository.findUpcoming();
	}

	/**
	 * Gets a maintenance window by ID.
	 *
	 * @param id the window ID
	 * @return optional containing the window if found
	 */
	public Optional<MaintenanceWindow> getMaintenanceWindow(Long id) {
		return maintenanceRepository.findById(id);
	}

	/**
	 * Updates a maintenance window.
	 *
	 * @param window the window to update
	 * @return the updated window
	 */
	public MaintenanceWindow updateMaintenanceWindow(MaintenanceWindow window) {
		return maintenanceRepository.update(window);
	}

	/**
	 * Deletes a maintenance window.
	 *
	 * @param id the window ID to delete
	 */
	public void deleteMaintenanceWindow(Long id) {
		maintenanceRepository.delete(id);
	}

	// ===== Statistics and Cleanup =====

	/**
	 * Gets alert statistics.
	 *
	 * @return alert statistics
	 */
	public AlertStats getAlertStats() {
		List<ActiveAlert> active = alertRepository.findActive();
		List<ActiveAlert> recent = alertRepository.findAll(1000);

		int criticalCount = (int) active
			.stream()
			.filter(a -> "CRITICAL".equalsIgnoreCase(a.getAlertSeverity()))
			.count();

		int unacknowledgedCount = (int) active
			.stream()
			.filter(a -> !a.isAcknowledged())
			.count();

		int resolvedLast24h = (int) recent
			.stream()
			.filter(a -> a.isResolved() && a.getResolvedAt() != null && a.getResolvedAt().isAfter(Instant.now().minus(Duration.ofHours(24))))
			.count();

		int activeSilences = silenceRepository.findActive().size();
		int activeMaintenanceWindows = maintenanceRepository.findActive().size();

		return new AlertStats(active.size(), criticalCount, unacknowledgedCount, resolvedLast24h, activeSilences, activeMaintenanceWindows);
	}

	/**
	 * Cleans up old resolved alerts and expired silences.
	 * Runs daily.
	 */
	@Scheduled(cron = "0 0 2 * * ?")
	public void cleanupOldData() {
		int deletedAlerts = alertRepository.deleteOlderThan(30);
		int deletedSilences = silenceRepository.deleteExpiredOlderThan(7);

		LOG.infof("Cleanup completed: %d old alerts, %d expired silences deleted", deletedAlerts, deletedSilences);
	}

	private String generateAlertId(String alertType, String instanceName) {
		String base = alertType + "-" + (instanceName != null ? instanceName : "global");
		return base + "-" + UUID.randomUUID().toString().substring(0, 8);
	}

	/**
	 * Alert statistics record.
	 */
	public record AlertStats(int activeCount, int criticalCount, int unacknowledgedCount, int resolvedLast24h, int activeSilences, int activeMaintenanceWindows) {
		/**
		 * Returns true if there are critical unacknowledged alerts.
		 */
		public boolean hasCriticalUnacknowledged() {
			return criticalCount > 0 && unacknowledgedCount > 0;
		}
	}
}
