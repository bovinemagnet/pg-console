package com.bovinemagnet.pgconsole.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration mapping for dashboard feature toggles.
 * <p>
 * Allows enabling/disabling dashboard sections and individual pages via configuration.
 * All features are enabled by default (opt-out model).
 * <p>
 * Configuration example:
 * <pre>
 * # Disable entire section
 * pg-console.dashboards.monitoring.enabled=false
 *
 * # Disable individual pages
 * pg-console.dashboards.analysis.index-advisor.enabled=false
 * </pre>
 * <p>
 * Environment variables:
 * <pre>
 * PG_CONSOLE_DASH_MONITORING=false
 * PG_CONSOLE_DASH_ANALYSIS_INDEX_ADVISOR=false
 * </pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ConfigMapping(prefix = "pg-console.dashboards")
public interface DashboardConfig {
	/**
	 * Monitoring section configuration.
	 *
	 * @return monitoring section config
	 */
	MonitoringConfig monitoring();

	/**
	 * Analysis section configuration.
	 *
	 * @return analysis section config
	 */
	AnalysisConfig analysis();

	/**
	 * Infrastructure section configuration.
	 *
	 * @return infrastructure section config
	 */
	InfrastructureConfig infrastructure();

	/**
	 * Data Control section configuration.
	 *
	 * @return data control section config
	 */
	@WithName("data-control")
	DataControlConfig dataControl();

	/**
	 * Enterprise section configuration.
	 *
	 * @return enterprise section config
	 */
	EnterpriseConfig enterprise();

	/**
	 * Security section configuration.
	 * Note: Uses pg-console.security.enabled for main toggle.
	 *
	 * @return security section config
	 */
	SecurityDashboardConfig security();

	/**
	 * Insights section configuration.
	 *
	 * @return insights section config
	 */
	InsightsConfig insights();

	/**
	 * Diagnostics section configuration (Phase 21).
	 *
	 * @return diagnostics section config
	 */
	DiagnosticsConfig diagnostics();

	// ========================================
	// Monitoring Section
	// ========================================

	/**
	 * Configuration for the Monitoring section.
	 * Pages: Dashboard, Activity, Slow Queries, Locks, Wait Events, Tables, Databases.
	 */
	interface MonitoringConfig {
		/**
		 * Enable or disable the entire Monitoring section.
		 *
		 * @return true if section is enabled
		 */
		@WithDefault("true")
		boolean enabled();

		/**
		 * Enable or disable the Dashboard page (/).
		 *
		 * @return true if page is enabled
		 */
		@WithName("dashboard")
		@WithDefault("true")
		boolean dashboardEnabled();

		/**
		 * Enable or disable the Activity page (/activity).
		 *
		 * @return true if page is enabled
		 */
		@WithName("activity")
		@WithDefault("true")
		boolean activityEnabled();

		/**
		 * Enable or disable the Slow Queries page (/slow-queries).
		 *
		 * @return true if page is enabled
		 */
		@WithName("slow-queries")
		@WithDefault("true")
		boolean slowQueriesEnabled();

		/**
		 * Enable or disable the Locks page (/locks).
		 *
		 * @return true if page is enabled
		 */
		@WithName("locks")
		@WithDefault("true")
		boolean locksEnabled();

		/**
		 * Enable or disable the Wait Events page (/wait-events).
		 *
		 * @return true if page is enabled
		 */
		@WithName("wait-events")
		@WithDefault("true")
		boolean waitEventsEnabled();

		/**
		 * Enable or disable the Tables page (/tables).
		 *
		 * @return true if page is enabled
		 */
		@WithName("tables")
		@WithDefault("true")
		boolean tablesEnabled();

		/**
		 * Enable or disable the Databases page (/databases).
		 *
		 * @return true if page is enabled
		 */
		@WithName("databases")
		@WithDefault("true")
		boolean databasesEnabled();

		/**
		 * Enable or disable the Deadlocks page (/deadlocks).
		 *
		 * @return true if page is enabled
		 */
		@WithName("deadlocks")
		@WithDefault("true")
		boolean deadlocksEnabled();
	}

	// ========================================
	// Analysis Section
	// ========================================

	/**
	 * Configuration for the Analysis section.
	 * Pages: Index Advisor, Query Regressions, Table Maintenance, Baselines.
	 */
	interface AnalysisConfig {
		/**
		 * Enable or disable the entire Analysis section.
		 *
		 * @return true if section is enabled
		 */
		@WithDefault("true")
		boolean enabled();

		/**
		 * Enable or disable the Index Advisor page (/index-advisor).
		 *
		 * @return true if page is enabled
		 */
		@WithName("index-advisor")
		@WithDefault("true")
		boolean indexAdvisorEnabled();

		/**
		 * Enable or disable the Query Regressions page (/query-regressions).
		 *
		 * @return true if page is enabled
		 */
		@WithName("query-regressions")
		@WithDefault("true")
		boolean queryRegressionsEnabled();

		/**
		 * Enable or disable the Table Maintenance page (/table-maintenance).
		 *
		 * @return true if page is enabled
		 */
		@WithName("table-maintenance")
		@WithDefault("true")
		boolean tableMaintenanceEnabled();

		/**
		 * Enable or disable the Baselines page (/statements-management).
		 *
		 * @return true if page is enabled
		 */
		@WithName("baselines")
		@WithDefault("true")
		boolean baselinesEnabled();
	}

	// ========================================
	// Infrastructure Section
	// ========================================

	/**
	 * Configuration for the Infrastructure section.
	 * Pages: Replication, Infrastructure, WAL Receiver, Maintenance Progress, I/O Statistics,
	 * Functions, Config Files, Prepared Statements, Materialised Views, Sequences, Extensions.
	 */
	interface InfrastructureConfig {
		/**
		 * Enable or disable the entire Infrastructure section.
		 *
		 * @return true if section is enabled
		 */
		@WithDefault("true")
		boolean enabled();

		/**
		 * Enable or disable the Replication page (/replication).
		 *
		 * @return true if page is enabled
		 */
		@WithName("replication")
		@WithDefault("true")
		boolean replicationEnabled();

		/**
		 * Enable or disable the Infrastructure page (/infrastructure).
		 *
		 * @return true if page is enabled
		 */
		@WithName("infrastructure")
		@WithDefault("true")
		boolean infrastructureEnabled();

		/**
		 * Enable or disable the WAL Receiver page (/wal-receiver).
		 * Shows WAL receiver status for standby servers.
		 *
		 * @return true if page is enabled
		 */
		@WithName("wal-receiver")
		@WithDefault("true")
		boolean walReceiverEnabled();

		/**
		 * Enable or disable the Maintenance Progress page (/maintenance-progress).
		 * Shows progress of VACUUM, ANALYZE, CREATE INDEX, CLUSTER, COPY, and base backup operations.
		 *
		 * @return true if page is enabled
		 */
		@WithName("maintenance-progress")
		@WithDefault("true")
		boolean maintenanceProgressEnabled();

		/**
		 * Enable or disable the I/O Statistics page (/io-statistics).
		 * Shows I/O statistics by backend type (PostgreSQL 16+).
		 *
		 * @return true if page is enabled
		 */
		@WithName("io-statistics")
		@WithDefault("true")
		boolean ioStatisticsEnabled();

		/**
		 * Enable or disable the Functions page (/functions).
		 * Shows function/procedure performance statistics.
		 *
		 * @return true if page is enabled
		 */
		@WithName("functions")
		@WithDefault("true")
		boolean functionsEnabled();

		/**
		 * Enable or disable the Config Files page (/config-files).
		 * Shows configuration file settings and errors.
		 *
		 * @return true if page is enabled
		 */
		@WithName("config-files")
		@WithDefault("true")
		boolean configFilesEnabled();

		/**
		 * Enable or disable the Prepared Statements page (/prepared-statements).
		 * Shows prepared statements and open cursors.
		 *
		 * @return true if page is enabled
		 */
		@WithName("prepared-statements")
		@WithDefault("true")
		boolean preparedStatementsEnabled();

		/**
		 * Enable or disable the Materialised Views page (/matviews).
		 * Shows materialised view information and refresh status.
		 *
		 * @return true if page is enabled
		 */
		@WithName("matviews")
		@WithDefault("true")
		boolean matviewsEnabled();

		/**
		 * Enable or disable the Sequences page (/sequences).
		 * Shows sequence information and exhaustion warnings.
		 *
		 * @return true if page is enabled
		 */
		@WithName("sequences")
		@WithDefault("true")
		boolean sequencesEnabled();

		/**
		 * Enable or disable the Extensions page (/extensions).
		 * Shows installed and available PostgreSQL extensions.
		 *
		 * @return true if page is enabled
		 */
		@WithName("extensions")
		@WithDefault("true")
		boolean extensionsEnabled();
	}

	// ========================================
	// Data Control Section
	// ========================================

	/**
	 * Configuration for the Data Control section.
	 * Pages: Logical Replication, CDC, Data Lineage, Partitions.
	 */
	interface DataControlConfig {
		/**
		 * Enable or disable the entire Data Control section.
		 *
		 * @return true if section is enabled
		 */
		@WithDefault("true")
		boolean enabled();

		/**
		 * Enable or disable the Logical Replication page (/logical-replication).
		 *
		 * @return true if page is enabled
		 */
		@WithName("logical-replication")
		@WithDefault("true")
		boolean logicalReplicationEnabled();

		/**
		 * Enable or disable the CDC page (/cdc).
		 *
		 * @return true if page is enabled
		 */
		@WithName("cdc")
		@WithDefault("true")
		boolean cdcEnabled();

		/**
		 * Enable or disable the Data Lineage page (/data-lineage).
		 *
		 * @return true if page is enabled
		 */
		@WithName("data-lineage")
		@WithDefault("true")
		boolean dataLineageEnabled();

		/**
		 * Enable or disable the Partitions page (/partitions).
		 *
		 * @return true if page is enabled
		 */
		@WithName("partitions")
		@WithDefault("true")
		boolean partitionsEnabled();
	}

	// ========================================
	// Enterprise Section
	// ========================================

	/**
	 * Configuration for the Enterprise section.
	 * Pages: Comparison, Schema Comparison, Bookmarks, Audit Log.
	 */
	interface EnterpriseConfig {
		/**
		 * Enable or disable the entire Enterprise section.
		 *
		 * @return true if section is enabled
		 */
		@WithDefault("true")
		boolean enabled();

		/**
		 * Enable or disable the Instance Comparison page (/comparison).
		 *
		 * @return true if page is enabled
		 */
		@WithName("comparison")
		@WithDefault("true")
		boolean comparisonEnabled();

		/**
		 * Enable or disable the Schema Comparison page (/schema-comparison).
		 *
		 * @return true if page is enabled
		 */
		@WithName("schema-comparison")
		@WithDefault("true")
		boolean schemaComparisonEnabled();

		/**
		 * Enable or disable the Database Diff page (/database-diff).
		 * <p>
		 * Cross-database schema comparison allowing comparison of schemas
		 * across different databases on the same or different instances.
		 *
		 * @return true if page is enabled
		 */
		@WithName("database-diff")
		@WithDefault("true")
		boolean databaseDiffEnabled();

		/**
		 * Enable or disable the Bookmarks page (/bookmarks).
		 *
		 * @return true if page is enabled
		 */
		@WithName("bookmarks")
		@WithDefault("true")
		boolean bookmarksEnabled();

		/**
		 * Enable or disable the Audit Log page (/audit-log).
		 *
		 * @return true if page is enabled
		 */
		@WithName("audit-log")
		@WithDefault("true")
		boolean auditLogEnabled();

		/**
		 * Enable or disable the Schema Documentation page (/schema-docs).
		 *
		 * @return true if page is enabled
		 */
		@WithName("schema-docs")
		@WithDefault("true")
		boolean schemaDocsEnabled();

		/**
		 * Enable or disable the Custom Dashboards page (/dashboards/custom).
		 *
		 * @return true if page is enabled
		 */
		@WithName("custom-dashboards")
		@WithDefault("true")
		boolean customDashboardsEnabled();
	}

	// ========================================
	// Security Dashboard Section
	// ========================================

	/**
	 * Configuration for the Security section pages.
	 * Note: The main security toggle is controlled by pg-console.security.enabled.
	 * These toggles control individual security pages.
	 */
	interface SecurityDashboardConfig {
		/**
		 * Enable or disable the entire Security section.
		 * Note: This is separate from pg-console.security.enabled which controls authentication.
		 *
		 * @return true if section is enabled
		 */
		@WithDefault("true")
		boolean enabled();

		/**
		 * Enable or disable the Security Overview page (/security).
		 *
		 * @return true if page is enabled
		 */
		@WithName("overview")
		@WithDefault("true")
		boolean overviewEnabled();

		/**
		 * Enable or disable the Roles page (/security/roles).
		 *
		 * @return true if page is enabled
		 */
		@WithName("roles")
		@WithDefault("true")
		boolean rolesEnabled();

		/**
		 * Enable or disable the Connections page (/security/connections).
		 *
		 * @return true if page is enabled
		 */
		@WithName("connections")
		@WithDefault("true")
		boolean connectionsEnabled();

		/**
		 * Enable or disable the Access page (/security/access).
		 *
		 * @return true if page is enabled
		 */
		@WithName("access")
		@WithDefault("true")
		boolean accessEnabled();

		/**
		 * Enable or disable the Compliance page (/security/compliance).
		 *
		 * @return true if page is enabled
		 */
		@WithName("compliance")
		@WithDefault("true")
		boolean complianceEnabled();

		/**
		 * Enable or disable the Recommendations page (/security/recommendations).
		 *
		 * @return true if page is enabled
		 */
		@WithName("recommendations")
		@WithDefault("true")
		boolean recommendationsEnabled();
	}

	// ========================================
	// Insights Section
	// ========================================

	/**
	 * Configuration for the Insights section.
	 * Pages: Insights Dashboard, Anomalies, Forecasts, Recommendations, Runbooks.
	 */
	interface InsightsConfig {
		/**
		 * Enable or disable the entire Insights section.
		 *
		 * @return true if section is enabled
		 */
		@WithDefault("true")
		boolean enabled();

		/**
		 * Enable or disable the Insights Dashboard page (/insights).
		 *
		 * @return true if page is enabled
		 */
		@WithName("dashboard")
		@WithDefault("true")
		boolean dashboardEnabled();

		/**
		 * Enable or disable the Anomalies page (/insights/anomalies).
		 *
		 * @return true if page is enabled
		 */
		@WithName("anomalies")
		@WithDefault("true")
		boolean anomaliesEnabled();

		/**
		 * Enable or disable the Forecasts page (/insights/forecasts).
		 *
		 * @return true if page is enabled
		 */
		@WithName("forecasts")
		@WithDefault("true")
		boolean forecastsEnabled();

		/**
		 * Enable or disable the Recommendations page (/insights/recommendations).
		 *
		 * @return true if page is enabled
		 */
		@WithName("recommendations")
		@WithDefault("true")
		boolean recommendationsEnabled();

		/**
		 * Enable or disable the Runbooks page (/insights/runbooks).
		 *
		 * @return true if page is enabled
		 */
		@WithName("runbooks")
		@WithDefault("true")
		boolean runbooksEnabled();
	}

	// ========================================
	// Diagnostics Section (Phase 21)
	// ========================================

	/**
	 * Configuration for the Diagnostics section.
	 * Pages: Pipeline Risk, TOAST Bloat, Index Redundancy, Statistical Freshness,
	 * Write/Read Ratio, HOT Efficiency, Correlation, Live Charts, XID Wraparound.
	 */
	interface DiagnosticsConfig {
		/**
		 * Enable or disable the entire Diagnostics section.
		 *
		 * @return true if section is enabled
		 */
		@WithDefault("true")
		boolean enabled();

		/**
		 * Enable or disable the Pipeline Risk page (/diagnostics/pipeline-risk).
		 *
		 * @return true if page is enabled
		 */
		@WithName("pipeline-risk")
		@WithDefault("true")
		boolean pipelineRiskEnabled();

		/**
		 * Enable or disable the TOAST Bloat page (/diagnostics/toast-bloat).
		 *
		 * @return true if page is enabled
		 */
		@WithName("toast-bloat")
		@WithDefault("true")
		boolean toastBloatEnabled();

		/**
		 * Enable or disable the Index Redundancy page (/diagnostics/index-redundancy).
		 *
		 * @return true if page is enabled
		 */
		@WithName("index-redundancy")
		@WithDefault("true")
		boolean indexRedundancyEnabled();

		/**
		 * Enable or disable the Statistical Freshness page (/diagnostics/statistical-freshness).
		 *
		 * @return true if page is enabled
		 */
		@WithName("statistical-freshness")
		@WithDefault("true")
		boolean statisticalFreshnessEnabled();

		/**
		 * Enable or disable the Write/Read Ratio page (/diagnostics/write-read-ratio).
		 *
		 * @return true if page is enabled
		 */
		@WithName("write-read-ratio")
		@WithDefault("true")
		boolean writeReadRatioEnabled();

		/**
		 * Enable or disable the HOT Efficiency page (/diagnostics/hot-efficiency).
		 *
		 * @return true if page is enabled
		 */
		@WithName("hot-efficiency")
		@WithDefault("true")
		boolean hotEfficiencyEnabled();

		/**
		 * Enable or disable the Correlation page (/diagnostics/correlation).
		 *
		 * @return true if page is enabled
		 */
		@WithName("correlation")
		@WithDefault("true")
		boolean correlationEnabled();

		/**
		 * Enable or disable the Live Charts page (/diagnostics/live-charts).
		 *
		 * @return true if page is enabled
		 */
		@WithName("live-charts")
		@WithDefault("true")
		boolean liveChartsEnabled();

		/**
		 * Enable or disable the XID Wraparound page (/diagnostics/xid-wraparound).
		 *
		 * @return true if page is enabled
		 */
		@WithName("xid-wraparound")
		@WithDefault("true")
		boolean xidWraparoundEnabled();
	}
}
