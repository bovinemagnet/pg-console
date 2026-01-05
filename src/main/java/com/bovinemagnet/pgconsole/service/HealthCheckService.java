package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.ConfigSetting;
import com.bovinemagnet.pgconsole.model.HealthCheck;
import com.bovinemagnet.pgconsole.model.HealthCheck.Category;
import com.bovinemagnet.pgconsole.model.HealthCheck.Status;
import com.bovinemagnet.pgconsole.model.OverviewStats;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for performing consolidated health checks across PostgreSQL metrics.
 * <p>
 * Aggregates health status from various subsystems (connections, performance,
 * maintenance, replication, configuration) into a unified view with traffic
 * light indicators for quick health assessment.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class HealthCheckService {

	@Inject
	PostgresService postgresService;

	@Inject
	InfrastructureService infrastructureService;

	@Inject
	ReplicationService replicationService;

	/**
	 * Performs all health checks for the specified instance.
	 *
	 * @param instanceName the PostgreSQL instance to check
	 * @return list of health check results sorted by category and severity
	 */
	public List<HealthCheck> performHealthChecks(String instanceName) {
		List<HealthCheck> checks = new ArrayList<>();

		// Connection checks
		checks.addAll(checkConnections(instanceName));

		// Performance checks
		checks.addAll(checkPerformance(instanceName));

		// Maintenance checks
		checks.addAll(checkMaintenance(instanceName));

		// Replication checks
		checks.addAll(checkReplication(instanceName));

		// Configuration checks
		checks.addAll(checkConfiguration(instanceName));

		// Sort by category order, then by severity (critical first)
		checks.sort(Comparator.comparing((HealthCheck h) -> h.getCategory().getOrder()).thenComparing(h -> h.getStatus() == Status.CRITICAL ? 0 : h.getStatus() == Status.WARNING ? 1 : 2));

		return checks;
	}

	/**
	 * Groups health checks by category for display.
	 */
	public Map<Category, List<HealthCheck>> groupByCategory(List<HealthCheck> checks) {
		return checks.stream().collect(Collectors.groupingBy(HealthCheck::getCategory));
	}

	/**
	 * Calculates overall health status from check results.
	 */
	public Status getOverallStatus(List<HealthCheck> checks) {
		boolean hasCritical = checks.stream().anyMatch(c -> c.getStatus() == Status.CRITICAL);
		boolean hasWarning = checks.stream().anyMatch(c -> c.getStatus() == Status.WARNING);

		if (hasCritical) return Status.CRITICAL;
		if (hasWarning) return Status.WARNING;
		return Status.OK;
	}

	/**
	 * Counts checks by status.
	 */
	public Map<Status, Long> countByStatus(List<HealthCheck> checks) {
		return checks.stream().collect(Collectors.groupingBy(HealthCheck::getStatus, Collectors.counting()));
	}

	private List<HealthCheck> checkConnections(String instanceName) {
		List<HealthCheck> checks = new ArrayList<>();
		OverviewStats stats = postgresService.getOverviewStats(instanceName);

		// Connection saturation
		if (stats.getMaxConnections() > 0) {
			double saturation = (stats.getConnectionsUsed() * 100.0) / stats.getMaxConnections();
			Status status = saturation > 90 ? Status.CRITICAL : saturation > 70 ? Status.WARNING : Status.OK;

			checks.add(
				HealthCheck.builder()
					.name("Connection Saturation")
					.description("Percentage of max_connections in use")
					.currentValue(String.format("%.1f%% (%d/%d)", saturation, stats.getConnectionsUsed(), stats.getMaxConnections()))
					.status(status)
					.category(Category.CONNECTIONS)
					.warningThreshold("> 70%")
					.criticalThreshold("> 90%")
					.detailsLink("/activity")
					.recommendation(status != Status.OK ? "Consider using connection pooling (PgBouncer)" : null)
					.build()
			);
		}

		// Active queries
		Status activeStatus = stats.getActiveQueries() > 50 ? Status.CRITICAL : stats.getActiveQueries() > 20 ? Status.WARNING : Status.OK;

		checks.add(
			HealthCheck.builder()
				.name("Active Queries")
				.description("Number of currently running queries")
				.currentValue(String.valueOf(stats.getActiveQueries()))
				.status(activeStatus)
				.category(Category.CONNECTIONS)
				.warningThreshold("> 20")
				.criticalThreshold("> 50")
				.detailsLink("/activity")
				.build()
		);

		// Blocked queries
		Status blockedStatus = stats.getBlockedQueries() > 5 ? Status.CRITICAL : stats.getBlockedQueries() > 0 ? Status.WARNING : Status.OK;

		checks.add(
			HealthCheck.builder()
				.name("Blocked Queries")
				.description("Queries waiting on locks")
				.currentValue(String.valueOf(stats.getBlockedQueries()))
				.status(blockedStatus)
				.category(Category.CONNECTIONS)
				.warningThreshold("> 0")
				.criticalThreshold("> 5")
				.detailsLink("/locks")
				.recommendation(blockedStatus != Status.OK ? "Check lock contention in the Locks dashboard" : null)
				.build()
		);

		return checks;
	}

	private List<HealthCheck> checkPerformance(String instanceName) {
		List<HealthCheck> checks = new ArrayList<>();
		OverviewStats stats = postgresService.getOverviewStats(instanceName);

		// Cache hit ratio
		double cacheRatio = stats.getCacheHitRatio();
		Status cacheStatus = cacheRatio < 90 ? Status.CRITICAL : cacheRatio < 95 ? Status.WARNING : Status.OK;

		checks.add(
			HealthCheck.builder()
				.name("Cache Hit Ratio")
				.description("Percentage of data reads from buffer cache")
				.currentValue(String.format("%.1f%%", cacheRatio))
				.status(cacheStatus)
				.category(Category.PERFORMANCE)
				.warningThreshold("< 95%")
				.criticalThreshold("< 90%")
				.detailsLink("/")
				.recommendation(cacheStatus != Status.OK ? "Consider increasing shared_buffers" : null)
				.build()
		);

		// Longest running query
		String longestQuery = stats.getLongestQueryDuration();
		if (longestQuery != null && !longestQuery.isEmpty() && !longestQuery.equals("00:00:00")) {
			// Parse duration string (HH:MM:SS or similar format) for threshold checks
			Status longQueryStatus = Status.OK;
			try {
				// Try to extract minutes from the duration string
				String[] parts = longestQuery.split(":");
				if (parts.length >= 2) {
					int hours = Integer.parseInt(parts[0]);
					int minutes = Integer.parseInt(parts[1]);
					int totalMinutes = hours * 60 + minutes;
					longQueryStatus = totalMinutes > 30 ? Status.CRITICAL : totalMinutes > 5 ? Status.WARNING : Status.OK;
				}
			} catch (NumberFormatException e) {
				// If parsing fails, just display the value
			}

			checks.add(
				HealthCheck.builder()
					.name("Longest Query")
					.description("Duration of the longest currently running query")
					.currentValue(longestQuery)
					.status(longQueryStatus)
					.category(Category.PERFORMANCE)
					.warningThreshold("> 5 min")
					.criticalThreshold("> 30 min")
					.detailsLink("/activity")
					.recommendation(longQueryStatus != Status.OK ? "Consider reviewing or cancelling long-running queries" : null)
					.build()
			);
		}

		return checks;
	}

	private List<HealthCheck> checkMaintenance(String instanceName) {
		List<HealthCheck> checks = new ArrayList<>();

		// XID Wraparound
		try {
			var xidList = postgresService.getXidWraparound(instanceName);
			if (!xidList.isEmpty()) {
				var worstDb = xidList.get(0);
				double percentUsed = worstDb.calculatePercentToWraparound();
				Status xidStatus = percentUsed > 80 ? Status.CRITICAL : percentUsed > 50 ? Status.WARNING : Status.OK;

				checks.add(
					HealthCheck.builder()
						.name("XID Wraparound Risk")
						.description("Transaction ID age as percentage of wraparound limit")
						.currentValue(String.format("%.1f%% (%s)", percentUsed, worstDb.getDatabaseName()))
						.status(xidStatus)
						.category(Category.MAINTENANCE)
						.warningThreshold("> 50%")
						.criticalThreshold("> 80%")
						.detailsLink("/diagnostics/xid-wraparound")
						.recommendation(xidStatus != Status.OK ? "Run VACUUM FREEZE on affected tables" : null)
						.build()
				);
			}
		} catch (Exception e) {
			// XID check failed, skip
		}

		// Dead tuple percentage (table bloat)
		try {
			var tables = postgresService.getTableStats(instanceName);
			double worstBloat = tables
				.stream()
				.filter(t -> t.getnLiveTup() > 0)
				.mapToDouble(t -> (t.getnDeadTup() * 100.0) / (t.getnLiveTup() + t.getnDeadTup()))
				.max()
				.orElse(0);

			Status bloatStatus = worstBloat > 20 ? Status.CRITICAL : worstBloat > 10 ? Status.WARNING : Status.OK;

			checks.add(
				HealthCheck.builder()
					.name("Table Bloat")
					.description("Highest dead tuple percentage across tables")
					.currentValue(String.format("%.1f%%", worstBloat))
					.status(bloatStatus)
					.category(Category.MAINTENANCE)
					.warningThreshold("> 10%")
					.criticalThreshold("> 20%")
					.detailsLink("/table-maintenance")
					.recommendation(bloatStatus != Status.OK ? "Run VACUUM on bloated tables" : null)
					.build()
			);
		} catch (Exception e) {
			// Table stats check failed, skip
		}

		return checks;
	}

	private List<HealthCheck> checkReplication(String instanceName) {
		List<HealthCheck> checks = new ArrayList<>();

		try {
			// Check for connected replicas (if this is a primary)
			var replicas = replicationService.getStreamingReplication(instanceName);
			if (!replicas.isEmpty()) {
				// Check for lag (getReplayLag() returns milliseconds)
				boolean hasLag = replicas.stream().anyMatch(r -> r.getReplayLag() > 60000); // > 1 minute
				boolean hasSevereLag = replicas.stream().anyMatch(r -> r.getReplayLag() > 300000); // > 5 minutes

				Status lagStatus = hasSevereLag ? Status.CRITICAL : hasLag ? Status.WARNING : Status.OK;

				// Convert max lag from milliseconds to seconds for display
				double maxLagSeconds = replicas
					.stream()
					.mapToDouble(r -> r.getReplayLag() / 1000.0)
					.max()
					.orElse(0);

				checks.add(
					HealthCheck.builder()
						.name("Replication Lag")
						.description("Maximum lag across connected replicas")
						.currentValue(maxLagSeconds > 60 ? String.format("%.1f min", maxLagSeconds / 60) : String.format("%.0f sec", maxLagSeconds))
						.status(lagStatus)
						.category(Category.REPLICATION)
						.warningThreshold("> 1 min")
						.criticalThreshold("> 5 min")
						.detailsLink("/replication")
						.recommendation(lagStatus != Status.OK ? "Check network and replica performance" : null)
						.build()
				);
			}
		} catch (Exception e) {
			// Replication check failed, skip
		}

		return checks;
	}

	private List<HealthCheck> checkConfiguration(String instanceName) {
		List<HealthCheck> checks = new ArrayList<>();

		try {
			var configSettings = postgresService.getConfigurationHealth(instanceName);

			long criticalCount = configSettings
				.stream()
				.filter(s -> s.getStatus() == ConfigSetting.Status.CRITICAL)
				.count();
			long warningCount = configSettings
				.stream()
				.filter(s -> s.getStatus() == ConfigSetting.Status.WARNING)
				.count();

			Status configStatus = criticalCount > 0 ? Status.CRITICAL : warningCount > 0 ? Status.WARNING : Status.OK;

			String valueStr = criticalCount > 0 ? String.format("%d critical, %d warnings", criticalCount, warningCount) : warningCount > 0 ? String.format("%d warnings", warningCount) : "All OK";

			checks.add(
				HealthCheck.builder()
					.name("Configuration Issues")
					.description("PostgreSQL configuration assessment")
					.currentValue(valueStr)
					.status(configStatus)
					.category(Category.CONFIGURATION)
					.detailsLink("/config-health")
					.recommendation(configStatus != Status.OK ? "Review configuration recommendations" : null)
					.build()
			);
		} catch (Exception e) {
			// Config check failed, skip
		}

		return checks;
	}
}
