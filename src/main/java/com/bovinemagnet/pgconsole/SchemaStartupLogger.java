package com.bovinemagnet.pgconsole;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Logs schema mode information at application startup.
 * <p>
 * Provides clear visibility into whether the application is running in
 * full schema mode (with persistence) or read-only mode (in-memory only).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class SchemaStartupLogger {

	private static final Logger LOG = Logger.getLogger("pgconsole.STARTUP");

	@Inject
	InstanceConfig config;

	/**
	 * Logs schema mode information when the application starts.
	 *
	 * @param event the startup event
	 */
	void onStart(@Observes StartupEvent event) {
		try {
			logSchemaMode();
		} catch (Exception e) {
			LOG.warn("Could not log schema mode: " + e.getMessage());
		}
	}

	/**
	 * Logs the schema mode configuration.
	 */
	private void logSchemaMode() {
		boolean schemaEnabled = config.schema().enabled();

		LOG.info("===========================================");
		LOG.info("PG-CONSOLE SCHEMA MODE");
		LOG.info("===========================================");

		if (schemaEnabled) {
			LOG.info("Mode: ENABLED (Full Persistence)");
			LOG.info("  - pgconsole schema will be created/migrated");
			LOG.info("  - History sampling: " + (config.history().enabled() ? "enabled" : "disabled"));
			if (config.history().enabled()) {
				LOG.infof("  - Sampling interval: %d seconds", config.history().intervalSeconds());
				LOG.infof("  - Retention period: %d days", config.history().retentionDays());
			}
			LOG.info("  - Audit logging: enabled");
			LOG.info("  - Query bookmarks: enabled");
			LOG.info("  - Alert history: enabled");
		} else {
			LOG.info("Mode: DISABLED (Read-Only / In-Memory)");
			LOG.info("  - No pgconsole schema will be created");
			LOG.infof("  - In-memory trends: last %d minutes", config.schema().inMemoryMinutes());
			LOG.info("  - Data lost on restart: expected");
			LOG.info("");
			LOG.info("  Disabled features:");
			LOG.info("    - Persistent history (long-term trends)");
			LOG.info("    - Audit logging to database");
			LOG.info("    - Query bookmarks");
			LOG.info("    - Alert history");
			LOG.info("    - Anomaly detection baselines");
			LOG.info("");
			LOG.info("  Working features:");
			LOG.info("    - Real-time monitoring (activity, locks, slow queries)");
			LOG.info("    - Short-term sparklines (in-memory)");
			LOG.info("    - All dashboard pages");
			LOG.info("    - Alerting (webhook/email, no history)");
		}

		LOG.info("===========================================");
	}
}
