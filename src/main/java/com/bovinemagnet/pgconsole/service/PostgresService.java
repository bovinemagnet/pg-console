package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.Activity;
import com.bovinemagnet.pgconsole.model.BlockingTree;
import com.bovinemagnet.pgconsole.model.ColumnCorrelation;
import com.bovinemagnet.pgconsole.model.ConfigSetting;
import com.bovinemagnet.pgconsole.model.DeadlockConfig;
import com.bovinemagnet.pgconsole.model.DeadlockStats;
import com.bovinemagnet.pgconsole.model.DatabaseMetricsHistory;
import com.bovinemagnet.pgconsole.model.DatabaseInfo;
import com.bovinemagnet.pgconsole.model.DatabaseMetrics;
import com.bovinemagnet.pgconsole.model.ExplainPlan;
import com.bovinemagnet.pgconsole.model.HotUpdateEfficiency;
import com.bovinemagnet.pgconsole.model.IndexRedundancy;
import com.bovinemagnet.pgconsole.model.InstanceInfo;
import com.bovinemagnet.pgconsole.model.LiveChartData;
import com.bovinemagnet.pgconsole.model.LockInfo;
import com.bovinemagnet.pgconsole.model.OverviewStats;
import com.bovinemagnet.pgconsole.model.PipelineRisk;
import com.bovinemagnet.pgconsole.model.SlowQuery;
import com.bovinemagnet.pgconsole.model.StatisticalFreshness;
import com.bovinemagnet.pgconsole.model.TableStats;
import com.bovinemagnet.pgconsole.model.ToastBloat;
import com.bovinemagnet.pgconsole.model.WaitEventSummary;
import com.bovinemagnet.pgconsole.model.WriteReadRatio;
import com.bovinemagnet.pgconsole.model.XidWraparound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Service for querying PostgreSQL statistics and metrics.
 * Supports multiple database instances via DataSourceManager.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class PostgresService {

	private static final Logger LOG = Logger.getLogger(PostgresService.class);

	@Inject
	DataSourceManager dataSourceManager;

	@ConfigProperty(name = "pg-console.databases")
	Optional<String> databaseFilter;

	/**
	 * Retrieves the datasource for the specified instance.
	 *
	 * @param instanceName the name of the PostgreSQL instance
	 * @return the datasource for the instance
	 */
	private DataSource getDataSource(String instanceName) {
		return dataSourceManager.getDataSource(instanceName);
	}

	/**
	 * Retrieves the list of configured PostgreSQL instances with their connection status and metadata.
	 *
	 * @return list of instance information objects containing name, display name, connection status, and version details
	 * @see DataSourceManager#getInstanceInfoList()
	 */
	public List<InstanceInfo> getInstanceList() {
		return dataSourceManager.getInstanceInfoList();
	}

	/**
	 * Retrieves the list of available PostgreSQL instance names.
	 *
	 * @return list of configured instance names
	 * @see DataSourceManager#getAvailableInstances()
	 */
	public List<String> getAvailableInstances() {
		return dataSourceManager.getAvailableInstances();
	}

	/**
	 * Retrieves the set of database names to monitor based on configured filters.
	 * <p>
	 * If the filter configuration is empty or blank, returns an empty set,
	 * which indicates that all non-template databases should be shown.
	 *
	 * @return set of database names to include in monitoring, or empty set to include all databases
	 */
	private Set<String> getDatabaseFilterSet() {
		if (databaseFilter.isEmpty() || databaseFilter.get().isBlank()) {
			return Set.of();
		}
		return Arrays.stream(databaseFilter.get().split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.collect(Collectors.toSet());
	}

	/**
	 * Determines whether the specified database should be included in monitoring based on configured filters.
	 * <p>
	 * If no filter is configured (empty set), all databases are included.
	 *
	 * @param dbName the name of the database to check
	 * @return true if the database should be included, false otherwise
	 */
	private boolean shouldIncludeDatabase(String dbName) {
		Set<String> filter = getDatabaseFilterSet();
		return filter.isEmpty() || filter.contains(dbName);
	}

	// ========== Slow Queries ==========

	/**
	 * Retrieves slow queries from pg_stat_statements for the specified instance.
	 * <p>
	 * Queries pg_stat_statements to identify queries with high execution times or call counts.
	 * Note that pg_stat_statements extension must be installed and enabled for this method to return results.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @param sortBy the column to sort by (e.g., "totalTime", "calls", "meanTime", "maxTime", "rows")
	 * @param order the sort order ("asc" for ascending, "desc" for descending)
	 * @return list of slow query records, limited to 100 results; empty list if pg_stat_statements is unavailable
	 * @see #getSlowQueryById(String, String)
	 */
	public List<SlowQuery> getSlowQueries(String instanceName, String sortBy, String order) {
		List<SlowQuery> queries = new ArrayList<>();
		String orderClause = getOrderClause(sortBy, order);

		String sql =
			"""
				SELECT
				    md5(query) as queryid,
				    query,
				    calls as total_calls,
				    total_exec_time as total_time,
				    mean_exec_time as mean_time,
				    min_exec_time as min_time,
				    max_exec_time as max_time,
				    stddev_exec_time as stddev_time,
				    rows,
				    shared_blks_hit,
				    shared_blks_read,
				    shared_blks_written,
				    temp_blks_read,
				    temp_blks_written,
				    'unknown' as user,
				    'current' as database
				FROM pg_stat_statements
				WHERE query NOT LIKE '%pg_stat_statements%'
				ORDER BY """ +
			" " +
			orderClause +
			" LIMIT 100";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				SlowQuery query = new SlowQuery();
				query.setQueryId(rs.getString("queryid"));
				query.setQuery(rs.getString("query"));
				query.setTotalCalls(rs.getLong("total_calls"));
				query.setTotalTime(rs.getDouble("total_time"));
				query.setMeanTime(rs.getDouble("mean_time"));
				query.setMinTime(rs.getDouble("min_time"));
				query.setMaxTime(rs.getDouble("max_time"));
				query.setStddevTime(rs.getDouble("stddev_time"));
				query.setRows(rs.getLong("rows"));
				query.setSharedBlksHit(rs.getLong("shared_blks_hit"));
				query.setSharedBlksRead(rs.getLong("shared_blks_read"));
				query.setSharedBlksWritten(rs.getLong("shared_blks_written"));
				query.setTempBlksRead(rs.getLong("temp_blks_read"));
				query.setTempBlksWritten(rs.getLong("temp_blks_written"));
				query.setUser(rs.getString("user"));
				query.setDatabase(rs.getString("database"));
				queries.add(query);
			}
		} catch (SQLException e) {
			LOG.warnf("Could not query pg_stat_statements on %s: %s", instanceName, e.getMessage());
		}

		return queries;
	}

	/**
	 * Retrieves slow queries from pg_stat_statements for the default instance.
	 * <p>
	 * This is a convenience method that delegates to {@link #getSlowQueries(String, String, String)}
	 * using "default" as the instance name.
	 *
	 * @param sortBy the column to sort by (e.g., "totalTime", "calls", "meanTime", "maxTime", "rows")
	 * @param order the sort order ("asc" for ascending, "desc" for descending)
	 * @return list of slow query records, limited to 100 results
	 * @see #getSlowQueries(String, String, String)
	 */
	public List<SlowQuery> getSlowQueries(String sortBy, String order) {
		return getSlowQueries("default", sortBy, order);
	}

	/**
	 * Constructs an ORDER BY clause for slow query sorting.
	 *
	 * @param sortBy the field to sort by; defaults to "totalTime" if null
	 * @param order the sort direction ("asc" or "desc"); defaults to "DESC" if not "asc"
	 * @return SQL ORDER BY clause fragment (e.g., "total_exec_time DESC")
	 */
	private String getOrderClause(String sortBy, String order) {
		String column = switch (sortBy != null ? sortBy : "totalTime") {
			case "calls" -> "calls";
			case "meanTime" -> "mean_exec_time";
			case "totalTime" -> "total_exec_time";
			case "maxTime" -> "max_exec_time";
			case "rows" -> "rows";
			case "cacheHitRatio" -> "CASE WHEN shared_blks_hit + shared_blks_read = 0 THEN 100.0 ELSE (shared_blks_hit * 100.0) / (shared_blks_hit + shared_blks_read) END";
			default -> "total_exec_time";
		};
		String direction = "asc".equalsIgnoreCase(order) ? "ASC" : "DESC";
		return column + " " + direction;
	}

	/**
	 * Retrieves detailed information about a specific slow query by its query ID.
	 * <p>
	 * Returns comprehensive statistics including execution time details, buffer I/O metrics,
	 * and temporary file usage from pg_stat_statements.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @param queryId the MD5 hash of the query text used as identifier
	 * @return the slow query details, or null if not found
	 * @see #getSlowQueries(String, String, String)
	 */
	public SlowQuery getSlowQueryById(String instanceName, String queryId) {
		String sql = """
			SELECT
			    md5(query) as queryid,
			    query,
			    calls as total_calls,
			    total_exec_time as total_time,
			    mean_exec_time as mean_time,
			    min_exec_time as min_time,
			    max_exec_time as max_time,
			    stddev_exec_time as stddev_time,
			    rows,
			    shared_blks_hit,
			    shared_blks_read,
			    shared_blks_written,
			    temp_blks_read,
			    temp_blks_written,
			    'unknown' as user,
			    'current' as database
			FROM pg_stat_statements
			WHERE md5(query) = ?
			LIMIT 1
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, queryId);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					SlowQuery query = new SlowQuery();
					query.setQueryId(rs.getString("queryid"));
					query.setQuery(rs.getString("query"));
					query.setTotalCalls(rs.getLong("total_calls"));
					query.setTotalTime(rs.getDouble("total_time"));
					query.setMeanTime(rs.getDouble("mean_time"));
					query.setMinTime(rs.getDouble("min_time"));
					query.setMaxTime(rs.getDouble("max_time"));
					query.setStddevTime(rs.getDouble("stddev_time"));
					query.setRows(rs.getLong("rows"));
					query.setSharedBlksHit(rs.getLong("shared_blks_hit"));
					query.setSharedBlksRead(rs.getLong("shared_blks_read"));
					query.setSharedBlksWritten(rs.getLong("shared_blks_written"));
					query.setTempBlksRead(rs.getLong("temp_blks_read"));
					query.setTempBlksWritten(rs.getLong("temp_blks_written"));
					query.setUser(rs.getString("user"));
					query.setDatabase(rs.getString("database"));
					return query;
				}
			}
		} catch (SQLException e) {
			LOG.warnf("Could not query pg_stat_statements on %s: %s", instanceName, e.getMessage());
		}

		return null;
	}

	/**
	 * Retrieves detailed information about a specific slow query by its query ID for the default instance.
	 *
	 * @param queryId the MD5 hash of the query text used as identifier
	 * @return the slow query details, or null if not found
	 * @see #getSlowQueryById(String, String)
	 */
	public SlowQuery getSlowQueryById(String queryId) {
		return getSlowQueryById("default", queryId);
	}

	// ========== Activity ==========

	/**
	 * Retrieves current activity from pg_stat_activity for the specified instance.
	 * <p>
	 * Returns information about all non-idle backend processes, excluding the current backend.
	 * Includes query text, connection information, wait events, and blocking relationships.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return list of active backend processes, limited to 50 most recent by query start time
	 * @throws RuntimeException if the query fails
	 * @see #getBlockingTree(String)
	 */
	public List<Activity> getCurrentActivity(String instanceName) {
		List<Activity> activities = new ArrayList<>();

		String sql = """
			SELECT
			    pid,
			    usename as user,
			    datname as database,
			    application_name,
			    client_addr::text as client_addr,
			    backend_start,
			    xact_start,
			    query_start,
			    state_change,
			    state,
			    wait_event_type,
			    wait_event,
			    query,
			    (SELECT pid FROM pg_stat_activity WHERE pid = ANY(pg_blocking_pids(pg_stat_activity.pid)) LIMIT 1) as blocking_pid
			FROM pg_stat_activity
			WHERE pid != pg_backend_pid()
			  AND state != 'idle'
			ORDER BY query_start DESC NULLS LAST
			LIMIT 50
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				Activity activity = new Activity();
				activity.setPid(rs.getInt("pid"));
				activity.setUser(rs.getString("user"));
				activity.setDatabase(rs.getString("database"));
				activity.setApplicationName(rs.getString("application_name"));
				activity.setClientAddr(rs.getString("client_addr"));
				activity.setBackendStart(rs.getTimestamp("backend_start") != null ? rs.getTimestamp("backend_start").toLocalDateTime() : null);
				activity.setXactStart(rs.getTimestamp("xact_start") != null ? rs.getTimestamp("xact_start").toLocalDateTime() : null);
				activity.setQueryStart(rs.getTimestamp("query_start") != null ? rs.getTimestamp("query_start").toLocalDateTime() : null);
				activity.setStateChange(rs.getTimestamp("state_change") != null ? rs.getTimestamp("state_change").toLocalDateTime() : null);
				activity.setState(rs.getString("state"));
				activity.setWaitEventType(rs.getString("wait_event_type"));
				activity.setWaitEvent(rs.getString("wait_event"));
				activity.setQuery(rs.getString("query"));
				int blockingPid = rs.getInt("blocking_pid");
				activity.setBlockingPid(rs.wasNull() ? null : blockingPid);
				activities.add(activity);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to query current activity on " + instanceName, e);
		}

		return activities;
	}

	/**
	 * Retrieves current activity from pg_stat_activity for the default instance.
	 *
	 * @return list of active backend processes, limited to 50 most recent
	 * @throws RuntimeException if the query fails
	 * @see #getCurrentActivity(String)
	 */
	public List<Activity> getCurrentActivity() {
		return getCurrentActivity("default");
	}

	// ========== Cancel/Terminate Queries ==========

	/**
	 * Cancels the current query running on a PostgreSQL backend process.
	 * <p>
	 * Uses {@code pg_cancel_backend()} which sends a SIGINT signal to cancel the query.
	 * This is less aggressive than {@link #terminateQuery(String, int)} as it only cancels
	 * the current query whilst leaving the connection open.
	 *
	 * @param instanceName the name of the PostgreSQL instance
	 * @param pid the backend process ID to cancel
	 * @return true if the signal was sent successfully, false otherwise
	 * @see #terminateQuery(String, int)
	 */
	public boolean cancelQuery(String instanceName, int pid) {
		String sql = "SELECT pg_cancel_backend(?)";
		try (Connection conn = getDataSource(instanceName).getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setInt(1, pid);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					boolean result = rs.getBoolean(1);
					LOG.infof("Cancel query on %s pid %d: %s", instanceName, pid, result ? "success" : "failed");
					return result;
				}
			}
		} catch (SQLException e) {
			LOG.errorf("Failed to cancel backend %d on %s: %s", pid, instanceName, e.getMessage());
		}
		return false;
	}

	/**
	 * Terminates a PostgreSQL backend process, forcefully disconnecting the client.
	 * <p>
	 * Uses {@code pg_terminate_backend()} which sends a SIGTERM signal to terminate the entire
	 * backend process. This is more aggressive than {@link #cancelQuery(String, int)} as it
	 * closes the client connection. Use with caution.
	 *
	 * @param instanceName the name of the PostgreSQL instance
	 * @param pid the backend process ID to terminate
	 * @return true if the signal was sent successfully, false otherwise
	 * @see #cancelQuery(String, int)
	 */
	public boolean terminateQuery(String instanceName, int pid) {
		String sql = "SELECT pg_terminate_backend(?)";
		try (Connection conn = getDataSource(instanceName).getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setInt(1, pid);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					boolean result = rs.getBoolean(1);
					LOG.infof("Terminate backend on %s pid %d: %s", instanceName, pid, result ? "success" : "failed");
					return result;
				}
			}
		} catch (SQLException e) {
			LOG.errorf("Failed to terminate backend %d on %s: %s", pid, instanceName, e.getMessage());
		}
		return false;
	}

	// ========== Table Stats ==========

	/**
	 * Retrieves table statistics from pg_stat_user_tables for the specified instance.
	 * <p>
	 * Returns statistics about user-defined tables, including scan counts, tuple operations,
	 * and live/dead tuple counts. Excludes system catalogues.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return list of table statistics, ordered by total tuples (live + dead) descending, limited to 50
	 * @throws RuntimeException if the query fails
	 */
	public List<TableStats> getTableStats(String instanceName) {
		List<TableStats> stats = new ArrayList<>();

		String sql = """
			SELECT
			    schemaname,
			    relname as tablename,
			    seq_scan,
			    seq_tup_read,
			    COALESCE(idx_scan, 0) as idx_scan,
			    COALESCE(idx_tup_fetch, 0) as idx_tup_fetch,
			    n_tup_ins,
			    n_tup_upd,
			    n_tup_del,
			    n_live_tup,
			    n_dead_tup
			FROM pg_stat_user_tables
			WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
			ORDER BY (n_live_tup + n_dead_tup) DESC
			LIMIT 50
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				TableStats stat = new TableStats();
				stat.setSchemaName(rs.getString("schemaname"));
				stat.setTableName(rs.getString("tablename"));
				stat.setSeqScan(rs.getLong("seq_scan"));
				stat.setSeqTupRead(rs.getLong("seq_tup_read"));
				stat.setIdxScan(rs.getLong("idx_scan"));
				stat.setIdxTupFetch(rs.getLong("idx_tup_fetch"));
				stat.setnTupIns(rs.getLong("n_tup_ins"));
				stat.setnTupUpd(rs.getLong("n_tup_upd"));
				stat.setnTupDel(rs.getLong("n_tup_del"));
				stat.setnLiveTup(rs.getLong("n_live_tup"));
				stat.setnDeadTup(rs.getLong("n_dead_tup"));
				stats.add(stat);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to query table stats on " + instanceName, e);
		}

		return stats;
	}

	/**
	 * Retrieves table statistics from pg_stat_user_tables for the default instance.
	 *
	 * @return list of table statistics, ordered by total tuples descending, limited to 50
	 * @throws RuntimeException if the query fails
	 * @see #getTableStats(String)
	 */
	public List<TableStats> getTableStats() {
		return getTableStats("default");
	}

	// ========== Database Info ==========

	/**
	 * Retrieves general database information and configuration for the specified instance.
	 * <p>
	 * Returns PostgreSQL version, current database and user, server encoding, start time,
	 * and checks for the presence of the pg_stat_statements extension.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return database information object with metadata and extension status
	 * @throws RuntimeException if the basic info query fails
	 */
	public DatabaseInfo getDatabaseInfo(String instanceName) {
		DatabaseInfo info = new DatabaseInfo();

		String sql = """
			SELECT
			    version() as postgres_version,
			    current_database() as current_database,
			    current_user as current_user,
			    (SELECT setting FROM pg_settings WHERE name = 'server_encoding') as server_encoding,
			    (SELECT pg_postmaster_start_time()::text) as server_start_time
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			if (rs.next()) {
				info.setPostgresVersion(rs.getString("postgres_version"));
				info.setCurrentDatabase(rs.getString("current_database"));
				info.setCurrentUser(rs.getString("current_user"));
				info.setServerEncoding(rs.getString("server_encoding"));
				info.setServerStartTime(rs.getString("server_start_time"));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to query database info on " + instanceName, e);
		}

		// Check for pg_stat_statements extension
		String extSql = """
			SELECT extversion FROM pg_extension WHERE extname = 'pg_stat_statements'
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(extSql)) {
			if (rs.next()) {
				info.setPgStatStatementsEnabled(true);
				info.setPgStatStatementsVersion(rs.getString("extversion"));
			} else {
				info.setPgStatStatementsEnabled(false);
			}
		} catch (SQLException e) {
			info.setPgStatStatementsEnabled(false);
		}

		return info;
	}

	/**
	 * Retrieves general database information and configuration for the default instance.
	 *
	 * @return database information object with metadata and extension status
	 * @throws RuntimeException if the basic info query fails
	 * @see #getDatabaseInfo(String)
	 */
	public DatabaseInfo getDatabaseInfo() {
		return getDatabaseInfo("default");
	}

	// ========== Overview Stats ==========

	/**
	 * Retrieves comprehensive overview statistics for the specified instance.
	 * <p>
	 * Returns a consolidated view including connection usage, active/blocked query counts,
	 * PostgreSQL version, longest running query duration, cache hit ratio, database size,
	 * and the top 10 largest tables and indexes.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return overview statistics aggregating multiple metrics
	 * @throws RuntimeException if any critical query fails
	 */
	public OverviewStats getOverviewStats(String instanceName) {
		OverviewStats stats = new OverviewStats();

		// Get connection counts and max connections
		String connectionsSql = """
			SELECT
			    (SELECT count(*) FROM pg_stat_activity) as connections_used,
			    (SELECT setting::int FROM pg_settings WHERE name = 'max_connections') as max_connections,
			    (SELECT count(*) FROM pg_stat_activity WHERE state = 'active' AND pid != pg_backend_pid()) as active_queries,
			    (SELECT count(*) FROM pg_stat_activity WHERE cardinality(pg_blocking_pids(pid)) > 0) as blocked_queries
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(connectionsSql)) {
			if (rs.next()) {
				stats.setConnectionsUsed(rs.getInt("connections_used"));
				stats.setConnectionsMax(rs.getInt("max_connections"));
				stats.setActiveQueries(rs.getInt("active_queries"));
				stats.setBlockedQueries(rs.getInt("blocked_queries"));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to query connection stats on " + instanceName, e);
		}

		// Get PostgreSQL version
		String versionSql = "SELECT version()";
		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(versionSql)) {
			if (rs.next()) {
				String fullVersion = rs.getString(1);
				// Extract just the major version (e.g., "PostgreSQL 16.2")
				if (fullVersion != null && fullVersion.startsWith("PostgreSQL")) {
					int endIdx = fullVersion.indexOf(" on ");
					if (endIdx > 0) {
						stats.setVersion(fullVersion.substring(0, endIdx));
					} else {
						stats.setVersion(fullVersion.split(",")[0]);
					}
				} else {
					stats.setVersion(fullVersion);
				}
			}
		} catch (SQLException e) {
			stats.setVersion("Unknown");
		}

		// Get longest running query duration
		String longestQuerySql = """
			SELECT
			    COALESCE(
			        (SELECT age(now(), query_start)::text
			         FROM pg_stat_activity
			         WHERE state = 'active' AND pid != pg_backend_pid()
			         ORDER BY query_start ASC NULLS LAST
			         LIMIT 1),
			        'None'
			    ) as longest_duration
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(longestQuerySql)) {
			if (rs.next()) {
				stats.setLongestQueryDuration(rs.getString("longest_duration"));
			}
		} catch (SQLException e) {
			stats.setLongestQueryDuration("Unknown");
		}

		// Get cache hit ratio
		String cacheSql = """
			SELECT
			    CASE WHEN (blks_hit + blks_read) = 0 THEN 0
			         ELSE (blks_hit::float / (blks_hit + blks_read)) * 100
			    END as cache_hit_ratio
			FROM pg_stat_database
			WHERE datname = current_database()
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(cacheSql)) {
			if (rs.next()) {
				stats.setCacheHitRatio(rs.getDouble("cache_hit_ratio"));
			}
		} catch (SQLException e) {
			stats.setCacheHitRatio(0);
		}

		// Get database size
		String sizeSql = """
			SELECT pg_size_pretty(pg_database_size(current_database())) as db_size
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sizeSql)) {
			if (rs.next()) {
				stats.setDatabaseSize(rs.getString("db_size"));
			}
		} catch (SQLException e) {
			stats.setDatabaseSize("Unknown");
		}

		// Get top 10 tables by size
		String tablesSql = """
			SELECT
			    schemaname,
			    relname as tablename,
			    pg_size_pretty(pg_total_relation_size(schemaname || '.' || relname)) as size,
			    pg_total_relation_size(schemaname || '.' || relname) as size_bytes
			FROM pg_stat_user_tables
			ORDER BY pg_total_relation_size(schemaname || '.' || relname) DESC
			LIMIT 10
			""";

		List<OverviewStats.TableSize> tables = new ArrayList<>();
		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(tablesSql)) {
			while (rs.next()) {
				OverviewStats.TableSize table = new OverviewStats.TableSize();
				table.setSchemaName(rs.getString("schemaname"));
				table.setTableName(rs.getString("tablename"));
				table.setSize(rs.getString("size"));
				table.setSizeBytes(rs.getLong("size_bytes"));
				tables.add(table);
			}
		} catch (SQLException e) {
			// Ignore - empty list is fine
		}
		stats.setTopTablesBySize(tables);

		// Get top 10 indexes by size
		String indexesSql = """
			SELECT
			    schemaname,
			    indexrelname as indexname,
			    relname as tablename,
			    pg_size_pretty(pg_relation_size(indexrelid)) as size,
			    pg_relation_size(indexrelid) as size_bytes
			FROM pg_stat_user_indexes
			ORDER BY pg_relation_size(indexrelid) DESC
			LIMIT 10
			""";

		List<OverviewStats.IndexSize> indexes = new ArrayList<>();
		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(indexesSql)) {
			while (rs.next()) {
				OverviewStats.IndexSize index = new OverviewStats.IndexSize();
				index.setSchemaName(rs.getString("schemaname"));
				index.setIndexName(rs.getString("indexname"));
				index.setTableName(rs.getString("tablename"));
				index.setSize(rs.getString("size"));
				index.setSizeBytes(rs.getLong("size_bytes"));
				indexes.add(index);
			}
		} catch (SQLException e) {
			// Ignore - empty list is fine
		}
		stats.setTopIndexesBySize(indexes);

		return stats;
	}

	/**
	 * Retrieves comprehensive overview statistics for the default instance.
	 *
	 * @return overview statistics aggregating multiple metrics
	 * @throws RuntimeException if any critical query fails
	 * @see #getOverviewStats(String)
	 */
	public OverviewStats getOverviewStats() {
		return getOverviewStats("default");
	}

	// ========== Blocking Tree ==========

	/**
	 * Retrieves the lock blocking hierarchy for the specified instance.
	 * <p>
	 * Identifies blocked queries and their blocking processes by analysing pg_locks and pg_stat_activity.
	 * Returns pairs of blocked and blocker processes with query details and lock modes.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return list of blocking relationships showing which queries are blocking others; empty list if query fails
	 * @see #getLockInfo(String)
	 */
	public List<BlockingTree> getBlockingTree(String instanceName) {
		List<BlockingTree> tree = new ArrayList<>();

		String sql = """
			SELECT
			    blocked.pid as blocked_pid,
			    blocked.usename as blocked_user,
			    blocked.query as blocked_query,
			    blocked.state as blocked_state,
			    age(now(), blocked.query_start)::text as blocked_duration,
			    blocker.pid as blocker_pid,
			    blocker.usename as blocker_user,
			    blocker.query as blocker_query,
			    blocker.state as blocker_state,
			    blocked_locks.mode as lock_mode
			FROM pg_stat_activity blocked
			JOIN pg_locks blocked_locks ON blocked.pid = blocked_locks.pid AND NOT blocked_locks.granted
			JOIN pg_locks blocker_locks ON blocker_locks.granted
			    AND blocked_locks.locktype = blocker_locks.locktype
			    AND blocked_locks.database IS NOT DISTINCT FROM blocker_locks.database
			    AND blocked_locks.relation IS NOT DISTINCT FROM blocker_locks.relation
			    AND blocked_locks.page IS NOT DISTINCT FROM blocker_locks.page
			    AND blocked_locks.tuple IS NOT DISTINCT FROM blocker_locks.tuple
			    AND blocked_locks.virtualxid IS NOT DISTINCT FROM blocker_locks.virtualxid
			    AND blocked_locks.transactionid IS NOT DISTINCT FROM blocker_locks.transactionid
			    AND blocked_locks.classid IS NOT DISTINCT FROM blocker_locks.classid
			    AND blocked_locks.objid IS NOT DISTINCT FROM blocker_locks.objid
			    AND blocked_locks.objsubid IS NOT DISTINCT FROM blocker_locks.objsubid
			    AND blocked_locks.pid != blocker_locks.pid
			JOIN pg_stat_activity blocker ON blocker.pid = blocker_locks.pid
			ORDER BY blocked.query_start
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				BlockingTree item = new BlockingTree();
				item.setBlockedPid(rs.getInt("blocked_pid"));
				item.setBlockedUser(rs.getString("blocked_user"));
				item.setBlockedQuery(rs.getString("blocked_query"));
				item.setBlockedState(rs.getString("blocked_state"));
				item.setBlockedDuration(rs.getString("blocked_duration"));
				item.setBlockerPid(rs.getInt("blocker_pid"));
				item.setBlockerUser(rs.getString("blocker_user"));
				item.setBlockerQuery(rs.getString("blocker_query"));
				item.setBlockerState(rs.getString("blocker_state"));
				item.setLockMode(rs.getString("lock_mode"));
				tree.add(item);
			}
		} catch (SQLException e) {
			LOG.warnf("Could not query blocking tree on %s: %s", instanceName, e.getMessage());
		}

		return tree;
	}

	/**
	 * Retrieves the lock blocking hierarchy for the default instance.
	 *
	 * @return list of blocking relationships; empty list if query fails
	 * @see #getBlockingTree(String)
	 */
	public List<BlockingTree> getBlockingTree() {
		return getBlockingTree("default");
	}

	// ========== Lock Info ==========

	/**
	 * Retrieves current lock information from pg_locks for the specified instance.
	 * <p>
	 * Returns details about all locks held or awaited by backend processes, joined with
	 * activity information. Excludes the current backend.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return list of lock information including granted/waiting status, lock type, and relation details; limited to 100 records
	 * @throws RuntimeException if the query fails
	 * @see #getBlockingTree(String)
	 */
	public List<LockInfo> getLockInfo(String instanceName) {
		List<LockInfo> locks = new ArrayList<>();

		String sql = """
			SELECT
			    l.pid,
			    l.locktype as lock_type,
			    d.datname as database,
			    COALESCE(c.relname, l.locktype || ':' || COALESCE(l.transactionid::text, l.virtualxid::text, '')) as relation,
			    l.mode,
			    l.granted,
			    a.query,
			    a.state,
			    a.wait_event_type,
			    a.wait_event,
			    a.usename as user
			FROM pg_locks l
			LEFT JOIN pg_database d ON l.database = d.oid
			LEFT JOIN pg_class c ON l.relation = c.oid
			JOIN pg_stat_activity a ON l.pid = a.pid
			WHERE l.pid != pg_backend_pid()
			ORDER BY l.granted, l.pid
			LIMIT 100
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				LockInfo lock = new LockInfo();
				lock.setPid(rs.getInt("pid"));
				lock.setLockType(rs.getString("lock_type"));
				lock.setDatabase(rs.getString("database"));
				lock.setRelation(rs.getString("relation"));
				lock.setMode(rs.getString("mode"));
				lock.setGranted(rs.getBoolean("granted"));
				lock.setQuery(rs.getString("query"));
				lock.setState(rs.getString("state"));
				lock.setWaitEventType(rs.getString("wait_event_type"));
				lock.setWaitEvent(rs.getString("wait_event"));
				lock.setUser(rs.getString("user"));
				locks.add(lock);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to query lock info on " + instanceName, e);
		}

		return locks;
	}

	/**
	 * Retrieves current lock information from pg_locks for the default instance.
	 *
	 * @return list of lock information; limited to 100 records
	 * @throws RuntimeException if the query fails
	 * @see #getLockInfo(String)
	 */
	public List<LockInfo> getLockInfo() {
		return getLockInfo("default");
	}

	// ========== Database List ==========

	/**
	 * Retrieves the list of non-template databases for the specified instance.
	 * <p>
	 * Returns database names from pg_database, filtered by the configured database filter
	 * and excluding template databases.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return list of database names, filtered according to configured filters
	 * @throws RuntimeException if the query fails
	 * @see #shouldIncludeDatabase(String)
	 */
	public List<String> getDatabaseList(String instanceName) {
		List<String> databases = new ArrayList<>();

		String sql = """
			SELECT datname
			FROM pg_database
			WHERE datistemplate = false
			ORDER BY datname
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				String dbName = rs.getString("datname");
				if (shouldIncludeDatabase(dbName)) {
					databases.add(dbName);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to query database list on " + instanceName, e);
		}

		return databases;
	}

	/**
	 * Retrieves the list of non-template databases for the default instance.
	 *
	 * @return list of database names, filtered according to configured filters
	 * @throws RuntimeException if the query fails
	 * @see #getDatabaseList(String)
	 */
	public List<String> getDatabaseList() {
		return getDatabaseList("default");
	}

	// ========== Database Metrics ==========

	/**
	 * Retrieves comprehensive metrics for all databases on the specified instance.
	 * <p>
	 * Returns statistics from pg_stat_database including transaction counts, block I/O,
	 * tuple operations, conflicts, deadlocks, session statistics, and database sizes.
	 * Excludes template databases and applies configured database filters.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return list of database metrics for all monitored databases
	 * @throws RuntimeException if the query fails
	 * @see #getDatabaseMetrics(String, String)
	 */
	public List<DatabaseMetrics> getAllDatabaseMetrics(String instanceName) {
		List<DatabaseMetrics> metrics = new ArrayList<>();

		String sql = """
			SELECT
			    d.datid,
			    d.datname,
			    d.numbackends,
			    d.xact_commit,
			    d.xact_rollback,
			    d.blks_read,
			    d.blks_hit,
			    d.tup_returned,
			    d.tup_fetched,
			    d.tup_inserted,
			    d.tup_updated,
			    d.tup_deleted,
			    d.conflicts,
			    d.temp_files,
			    d.temp_bytes,
			    d.deadlocks,
			    COALESCE(d.blk_read_time, 0) as blk_read_time,
			    COALESCE(d.blk_write_time, 0) as blk_write_time,
			    COALESCE(d.session_time, 0) as session_time,
			    COALESCE(d.active_time, 0) as active_time,
			    COALESCE(d.idle_in_transaction_time, 0) as idle_in_transaction_time,
			    COALESCE(d.sessions, 0) as sessions,
			    COALESCE(d.sessions_abandoned, 0) as sessions_abandoned,
			    COALESCE(d.sessions_fatal, 0) as sessions_fatal,
			    COALESCE(d.sessions_killed, 0) as sessions_killed,
			    d.stats_reset::text as stats_reset,
			    has_database_privilege(d.datname, 'CONNECT') as has_access,
			    CASE WHEN has_database_privilege(d.datname, 'CONNECT')
			         THEN pg_size_pretty(pg_database_size(d.datname))
			         ELSE 'N/A' END as database_size,
			    CASE WHEN has_database_privilege(d.datname, 'CONNECT')
			         THEN pg_database_size(d.datname)
			         ELSE 0 END as database_size_bytes,
			    EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_stat_statements') as pg_stat_statements_enabled
			FROM pg_stat_database d
			JOIN pg_database db ON d.datid = db.oid
			WHERE db.datistemplate = false
			ORDER BY d.datname
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				DatabaseMetrics m = mapDatabaseMetrics(rs);
				if (shouldIncludeDatabase(m.getDatname())) {
					metrics.add(m);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to query database metrics on " + instanceName, e);
		}

		return metrics;
	}

	/**
	 * Retrieves comprehensive metrics for all databases on the default instance.
	 *
	 * @return list of database metrics for all monitored databases
	 * @throws RuntimeException if the query fails
	 * @see #getAllDatabaseMetrics(String)
	 */
	public List<DatabaseMetrics> getAllDatabaseMetrics() {
		return getAllDatabaseMetrics("default");
	}

	/**
	 * Retrieves comprehensive metrics for a specific database on the specified instance.
	 * <p>
	 * Returns statistics from pg_stat_database for a single database, including transaction counts,
	 * block I/O, tuple operations, conflicts, deadlocks, session statistics, and database size.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @param databaseName the name of the database to retrieve metrics for
	 * @return database metrics, or null if the database is not found
	 * @throws RuntimeException if the query fails
	 * @see #getAllDatabaseMetrics(String)
	 */
	public DatabaseMetrics getDatabaseMetrics(String instanceName, String databaseName) {
		String sql = """
			SELECT
			    d.datid,
			    d.datname,
			    d.numbackends,
			    d.xact_commit,
			    d.xact_rollback,
			    d.blks_read,
			    d.blks_hit,
			    d.tup_returned,
			    d.tup_fetched,
			    d.tup_inserted,
			    d.tup_updated,
			    d.tup_deleted,
			    d.conflicts,
			    d.temp_files,
			    d.temp_bytes,
			    d.deadlocks,
			    COALESCE(d.blk_read_time, 0) as blk_read_time,
			    COALESCE(d.blk_write_time, 0) as blk_write_time,
			    COALESCE(d.session_time, 0) as session_time,
			    COALESCE(d.active_time, 0) as active_time,
			    COALESCE(d.idle_in_transaction_time, 0) as idle_in_transaction_time,
			    COALESCE(d.sessions, 0) as sessions,
			    COALESCE(d.sessions_abandoned, 0) as sessions_abandoned,
			    COALESCE(d.sessions_fatal, 0) as sessions_fatal,
			    COALESCE(d.sessions_killed, 0) as sessions_killed,
			    d.stats_reset::text as stats_reset,
			    has_database_privilege(d.datname, 'CONNECT') as has_access,
			    CASE WHEN has_database_privilege(d.datname, 'CONNECT')
			         THEN pg_size_pretty(pg_database_size(d.datname))
			         ELSE 'N/A' END as database_size,
			    CASE WHEN has_database_privilege(d.datname, 'CONNECT')
			         THEN pg_database_size(d.datname)
			         ELSE 0 END as database_size_bytes,
			    EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_stat_statements') as pg_stat_statements_enabled
			FROM pg_stat_database d
			WHERE d.datname = ?
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, databaseName);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return mapDatabaseMetrics(rs);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to query database metrics for " + databaseName + " on " + instanceName, e);
		}

		return null;
	}

	/**
	 * Retrieves comprehensive metrics for a specific database on the default instance.
	 *
	 * @param databaseName the name of the database to retrieve metrics for
	 * @return database metrics, or null if the database is not found
	 * @throws RuntimeException if the query fails
	 * @see #getDatabaseMetrics(String, String)
	 */
	public DatabaseMetrics getDatabaseMetrics(String databaseName) {
		return getDatabaseMetrics("default", databaseName);
	}

	/**
	 * Maps a ResultSet row to a DatabaseMetrics object.
	 * <p>
	 * This is a utility method used internally to convert database query results
	 * into strongly-typed model objects.
	 *
	 * @param rs the ResultSet positioned at the row to map
	 * @return populated DatabaseMetrics object
	 * @throws SQLException if there is an error reading from the ResultSet
	 */
	private DatabaseMetrics mapDatabaseMetrics(ResultSet rs) throws SQLException {
		DatabaseMetrics m = new DatabaseMetrics();
		m.setDatid(rs.getLong("datid"));
		m.setDatname(rs.getString("datname"));
		m.setNumBackends(rs.getInt("numbackends"));
		m.setXactCommit(rs.getLong("xact_commit"));
		m.setXactRollback(rs.getLong("xact_rollback"));
		m.setBlksRead(rs.getLong("blks_read"));
		m.setBlksHit(rs.getLong("blks_hit"));
		m.setTupReturned(rs.getLong("tup_returned"));
		m.setTupFetched(rs.getLong("tup_fetched"));
		m.setTupInserted(rs.getLong("tup_inserted"));
		m.setTupUpdated(rs.getLong("tup_updated"));
		m.setTupDeleted(rs.getLong("tup_deleted"));
		m.setConflicts(rs.getLong("conflicts"));
		m.setTempFiles(rs.getLong("temp_files"));
		m.setTempBytes(rs.getLong("temp_bytes"));
		m.setDeadlocks(rs.getLong("deadlocks"));
		m.setBlkReadTime(rs.getDouble("blk_read_time"));
		m.setBlkWriteTime(rs.getDouble("blk_write_time"));
		m.setSessionTime(rs.getDouble("session_time"));
		m.setActiveTime(rs.getDouble("active_time"));
		m.setIdleInTransactionTime(rs.getDouble("idle_in_transaction_time"));
		m.setSessions(rs.getLong("sessions"));
		m.setSessionsAbandoned(rs.getLong("sessions_abandoned"));
		m.setSessionsFatal(rs.getLong("sessions_fatal"));
		m.setSessionsKilled(rs.getLong("sessions_killed"));
		m.setStatsReset(rs.getString("stats_reset"));
		m.setDatabaseSize(rs.getString("database_size"));
		m.setDatabaseSizeBytes(rs.getLong("database_size_bytes"));
		m.setPgStatStatementsEnabled(rs.getBoolean("pg_stat_statements_enabled"));
		m.setHasAccess(rs.getBoolean("has_access"));
		return m;
	}

	// ========== Wait Events ==========

	/**
	 * Gets a summary of current wait events from pg_stat_activity.
	 *
	 * @param instanceName the instance to query
	 * @return list of wait event summaries
	 */
	public List<WaitEventSummary> getWaitEventSummary(String instanceName) {
		List<WaitEventSummary> summaries = new ArrayList<>();

		String sql = """
			SELECT
			    wait_event_type,
			    wait_event,
			    COUNT(*) as session_count
			FROM pg_stat_activity
			WHERE pid != pg_backend_pid()
			GROUP BY wait_event_type, wait_event
			ORDER BY
			    CASE WHEN wait_event_type IS NULL THEN 0 ELSE 1 END,
			    wait_event_type,
			    session_count DESC
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				WaitEventSummary summary = new WaitEventSummary();
				summary.setWaitEventType(rs.getString("wait_event_type"));
				summary.setWaitEvent(rs.getString("wait_event"));
				summary.setSessionCount(rs.getInt("session_count"));
				summaries.add(summary);
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get wait event summary for %s: %s", instanceName, e.getMessage());
		}

		return summaries;
	}

	/**
	 * Retrieves a summary of current wait events from pg_stat_activity for the default instance.
	 *
	 * @return list of wait event summaries with session counts
	 * @see #getWaitEventSummary(String)
	 */
	public List<WaitEventSummary> getWaitEventSummary() {
		return getWaitEventSummary("default");
	}

	/**
	 * Retrieves wait event totals grouped by wait_event_type for the specified instance.
	 * <p>
	 * Aggregates sessions from pg_stat_activity by wait_event_type to provide a high-level
	 * view of what types of events backends are waiting on.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return list of wait event type summaries with session counts, sorted by session count descending
	 */
	public List<WaitEventSummary> getWaitEventTypeSummary(String instanceName) {
		List<WaitEventSummary> summaries = new ArrayList<>();

		String sql = """
			SELECT
			    wait_event_type,
			    COUNT(*) as session_count
			FROM pg_stat_activity
			WHERE pid != pg_backend_pid()
			GROUP BY wait_event_type
			ORDER BY
			    CASE WHEN wait_event_type IS NULL THEN 0 ELSE 1 END,
			    session_count DESC
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				WaitEventSummary summary = new WaitEventSummary();
				summary.setWaitEventType(rs.getString("wait_event_type"));
				summary.setSessionCount(rs.getInt("session_count"));
				summaries.add(summary);
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get wait event type summary for %s: %s", instanceName, e.getMessage());
		}

		return summaries;
	}

	/**
	 * Retrieves wait event totals grouped by wait_event_type for the default instance.
	 *
	 * @return list of wait event type summaries with session counts
	 * @see #getWaitEventTypeSummary(String)
	 */
	public List<WaitEventSummary> getWaitEventTypeSummary() {
		return getWaitEventTypeSummary("default");
	}

	// =============================================
	// EXPLAIN Plan Methods
	// =============================================

	/**
	 * Generates an EXPLAIN plan for a query on the specified instance.
	 * <p>
	 * Only SELECT, WITH (CTE), and VALUES queries are allowed for safety reasons.
	 * The query is NOT executed unless {@code analyse} is true, in which case EXPLAIN ANALYZE
	 * runs the query and provides actual execution statistics.
	 * <p>
	 * Note: Using EXPLAIN ANALYZE will execute the query, which may modify data if DDL/DML
	 * is embedded in functions or triggers. This method blocks such statements at the top level.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @param query the SQL query to explain (must be SELECT, WITH, or VALUES)
	 * @param analyse if true, uses EXPLAIN ANALYZE which actually executes the query
	 * @param buffers if true, includes buffer usage information in the plan output
	 * @return the explain plan result containing plan text or error message
	 * @see #isExplainSafe(String)
	 */
	public ExplainPlan explainQuery(String instanceName, String query, boolean analyse, boolean buffers) {
		ExplainPlan plan = new ExplainPlan();
		plan.setQuery(query);
		plan.setAnalyse(analyse);
		plan.setBuffers(buffers);

		// Validate the query is safe to explain
		String trimmedQuery = query.trim().toUpperCase();
		if (!isExplainSafe(trimmedQuery)) {
			plan.setError("Only SELECT, WITH, and VALUES queries can be explained for safety reasons.");
			return plan;
		}

		// Build the EXPLAIN command
		StringBuilder explainCmd = new StringBuilder("EXPLAIN ");
		if (analyse || buffers) {
			explainCmd.append("(");
			if (analyse) {
				explainCmd.append("ANALYZE");
			}
			if (buffers) {
				if (analyse) explainCmd.append(", ");
				explainCmd.append("BUFFERS");
			}
			explainCmd.append(") ");
		}
		explainCmd.append(query);

		StringBuilder planText = new StringBuilder();

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(explainCmd.toString())) {
			while (rs.next()) {
				String line = rs.getString(1);
				plan.addPlanLine(line);
				if (planText.length() > 0) {
					planText.append("\n");
				}
				planText.append(line);
			}
			plan.setPlanText(planText.toString());
		} catch (SQLException e) {
			LOG.warnf("Failed to explain query for %s: %s", instanceName, e.getMessage());
			plan.setError("Failed to generate explain plan: " + e.getMessage());
		}

		return plan;
	}

	/**
	 * Validates whether a query is safe to execute with EXPLAIN.
	 * <p>
	 * Only allows SELECT, WITH (CTE), and VALUES statements to prevent accidental
	 * execution of DDL or DML operations.
	 *
	 * @param upperQuery the SQL query in uppercase with leading whitespace removed
	 * @return true if the query starts with SELECT, WITH, or VALUES; false otherwise
	 */
	private boolean isExplainSafe(String upperQuery) {
		// Remove leading whitespace and check first keyword
		String cleaned = upperQuery.replaceAll("^\\s+", "");

		// Allow SELECT, WITH, and VALUES
		if (cleaned.startsWith("SELECT") || cleaned.startsWith("WITH") || cleaned.startsWith("VALUES")) {
			return true;
		}

		// Check for common DML that we should block
		if (cleaned.startsWith("INSERT") || cleaned.startsWith("UPDATE") || cleaned.startsWith("DELETE") || cleaned.startsWith("DROP") || cleaned.startsWith("CREATE") || cleaned.startsWith("ALTER") || cleaned.startsWith("TRUNCATE")) {
			return false;
		}

		// Default to false for safety
		return false;
	}

	/**
	 * Generates an EXPLAIN plan for a query on the default instance.
	 *
	 * @param query the SQL query to explain (must be SELECT, WITH, or VALUES)
	 * @param analyse if true, uses EXPLAIN ANALYZE which actually executes the query
	 * @param buffers if true, includes buffer usage information in the plan output
	 * @return the explain plan result containing plan text or error message
	 * @see #explainQuery(String, String, boolean, boolean)
	 */
	public ExplainPlan explainQuery(String query, boolean analyse, boolean buffers) {
		return explainQuery("default", query, analyse, buffers);
	}

	// ========================================
	// Phase 21: Enhanced Database Diagnostics
	// ========================================

	// ========== Pipeline Risk Monitoring ==========

	/**
	 * Retrieves pipeline/queue table risk metrics for the specified instance.
	 * <p>
	 * Identifies tables that may be queue or pipeline tables based on naming patterns
	 * and tracks the age of the oldest row to detect processing backlogs.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @param tablePatterns list of table name patterns to check (e.g., "%queue%", "%event%")
	 * @param staleThresholdHours number of hours after which a row is considered stale
	 * @return list of pipeline risk metrics
	 */
	public List<PipelineRisk> getPipelineRisk(String instanceName, List<String> tablePatterns, int staleThresholdHours) {
		List<PipelineRisk> risks = new ArrayList<>();

		// First, find tables matching the patterns
		StringBuilder patternCondition = new StringBuilder();
		if (tablePatterns != null && !tablePatterns.isEmpty()) {
			patternCondition.append("(");
			for (int i = 0; i < tablePatterns.size(); i++) {
				if (i > 0) patternCondition.append(" OR ");
				patternCondition.append("relname ILIKE ?");
			}
			patternCondition.append(")");
		} else {
			patternCondition.append("(relname ILIKE '%queue%' OR relname ILIKE '%event%' OR relname ILIKE '%job%' OR relname ILIKE '%task%')");
		}

		String findTablesSql =
			"""
				SELECT
				    schemaname,
				    relname as tablename,
				    n_live_tup as row_count
				FROM pg_stat_user_tables
				WHERE """ +
			patternCondition +
			"""
				ORDER BY n_live_tup DESC
				LIMIT 50
				""";

		try (Connection conn = getDataSource(instanceName).getConnection(); PreparedStatement stmt = conn.prepareStatement(findTablesSql)) {
			if (tablePatterns != null && !tablePatterns.isEmpty()) {
				for (int i = 0; i < tablePatterns.size(); i++) {
					stmt.setString(i + 1, tablePatterns.get(i));
				}
			}

			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					PipelineRisk risk = new PipelineRisk();
					risk.setSchemaName(rs.getString("schemaname"));
					risk.setTableName(rs.getString("tablename"));
					risk.setRowCount(rs.getLong("row_count"));
					risks.add(risk);
				}
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to query pipeline tables on %s: %s", instanceName, e.getMessage());
		}

		// For each table, try to get the oldest row timestamp
		// This requires tables to have a timestamp column - we check for common patterns
		for (PipelineRisk risk : risks) {
			String checkOldestSql = """
				SELECT
				    column_name
				FROM information_schema.columns
				WHERE table_schema = ? AND table_name = ?
				AND (column_name ILIKE '%created%' OR column_name ILIKE '%timestamp%'
				     OR column_name ILIKE '%time%' OR column_name ILIKE '%date%')
				AND data_type IN ('timestamp without time zone', 'timestamp with time zone', 'timestamptz')
				ORDER BY ordinal_position
				LIMIT 1
				""";

			try (Connection conn = getDataSource(instanceName).getConnection(); PreparedStatement stmt = conn.prepareStatement(checkOldestSql)) {
				stmt.setString(1, risk.getSchemaName());
				stmt.setString(2, risk.getTableName());

				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						String timeColumn = rs.getString("column_name");
						risk.setTimestampColumn(timeColumn);

						// Get the oldest row age
						String oldestSql = String.format("SELECT EXTRACT(EPOCH FROM (NOW() - MIN(%s)))::bigint as oldest_age_seconds FROM %s.%s", timeColumn, risk.getSchemaName(), risk.getTableName());

						try (Statement ageStmt = conn.createStatement(); ResultSet ageRs = ageStmt.executeQuery(oldestSql)) {
							if (ageRs.next()) {
								long ageSeconds = ageRs.getLong("oldest_age_seconds");
								if (!ageRs.wasNull()) {
									risk.setOldestRowTimestamp(Instant.now().minusSeconds(ageSeconds));
								}
							}
						}
					}
				}
			} catch (SQLException e) {
				LOG.debugf("Could not determine oldest row for %s.%s: %s", risk.getSchemaName(), risk.getTableName(), e.getMessage());
			}
		}

		return risks;
	}

	/**
	 * Retrieves pipeline/queue table risk metrics for the default instance.
	 *
	 * @param tablePatterns list of table name patterns to check
	 * @param staleThresholdHours number of hours after which a row is considered stale
	 * @return list of pipeline risk metrics
	 * @see #getPipelineRisk(String, List, int)
	 */
	public List<PipelineRisk> getPipelineRisk(List<String> tablePatterns, int staleThresholdHours) {
		return getPipelineRisk("default", tablePatterns, staleThresholdHours);
	}

	// ========== TOAST Bloat Analysis ==========

	/**
	 * Retrieves TOAST table bloat analysis for the specified instance.
	 * <p>
	 * Analyses tables with TOAST storage to identify bloat in out-of-line storage.
	 * TOAST tables store large values separately from the main table.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return list of TOAST bloat metrics
	 */
	public List<ToastBloat> getToastBloat(String instanceName) {
		List<ToastBloat> bloats = new ArrayList<>();

		String sql = """
			SELECT
			    c.relnamespace::regnamespace::text as schema_name,
			    c.relname as table_name,
			    pg_relation_size(c.oid) as main_table_bytes,
			    COALESCE(pg_relation_size(c.reltoastrelid), 0) as toast_table_bytes,
			    pg_total_relation_size(c.oid) as total_bytes,
			    t.relname as toast_table_name,
			    COALESCE(pg_stat_get_live_tuples(c.reltoastrelid), 0) as toast_live_tuples,
			    COALESCE(pg_stat_get_dead_tuples(c.reltoastrelid), 0) as toast_dead_tuples
			FROM pg_class c
			LEFT JOIN pg_class t ON c.reltoastrelid = t.oid
			WHERE c.relkind = 'r'
			  AND c.reltoastrelid != 0
			  AND c.relnamespace::regnamespace::text NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
			ORDER BY pg_relation_size(c.reltoastrelid) DESC NULLS LAST
			LIMIT 50
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				ToastBloat bloat = new ToastBloat();
				bloat.setSchemaName(rs.getString("schema_name"));
				bloat.setTableName(rs.getString("table_name"));
				bloat.setMainTableSizeBytes(rs.getLong("main_table_bytes"));
				bloat.setToastTableSizeBytes(rs.getLong("toast_table_bytes"));
				bloat.setToastTableName(rs.getString("toast_table_name"));
				bloat.setnLiveTup(rs.getLong("toast_live_tuples"));
				bloat.setnDeadTup(rs.getLong("toast_dead_tuples"));
				bloats.add(bloat);
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to query TOAST bloat on %s: %s", instanceName, e.getMessage());
		}

		return bloats;
	}

	/**
	 * Retrieves TOAST table bloat analysis for the default instance.
	 *
	 * @return list of TOAST bloat metrics
	 * @see #getToastBloat(String)
	 */
	public List<ToastBloat> getToastBloat() {
		return getToastBloat("default");
	}

	// ========== Index Redundancy Detection ==========

	/**
	 * Retrieves redundant and overlapping index analysis for the specified instance.
	 * <p>
	 * Identifies indexes that are potentially redundant because they duplicate
	 * or are subsets of other indexes on the same table.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return list of index redundancy findings
	 */
	public List<IndexRedundancy> getIndexRedundancy(String instanceName) {
		List<IndexRedundancy> redundancies = new ArrayList<>();

		String sql = """
			WITH index_cols AS (
			    SELECT
			        i.indexrelid,
			        i.indrelid,
			        i.indkey::int[] as col_array,
			        pg_get_indexdef(i.indexrelid) as index_def,
			        c.relname as index_name,
			        t.relname as table_name,
			        n.nspname as schema_name,
			        pg_relation_size(i.indexrelid) as index_size,
			        COALESCE(s.idx_scan, 0) as idx_scan,
			        COALESCE(s.idx_tup_read, 0) as idx_tup_read
			    FROM pg_index i
			    JOIN pg_class c ON i.indexrelid = c.oid
			    JOIN pg_class t ON i.indrelid = t.oid
			    JOIN pg_namespace n ON t.relnamespace = n.oid
			    LEFT JOIN pg_stat_user_indexes s ON i.indexrelid = s.indexrelid
			    WHERE n.nspname NOT IN ('pg_catalog', 'information_schema')
			      AND NOT i.indisprimary
			)
			SELECT
			    a.schema_name,
			    a.table_name,
			    a.index_name,
			    a.index_def,
			    a.index_size,
			    a.idx_scan,
			    b.index_name as overlapping_index,
			    b.index_def as overlapping_def,
			    b.index_size as overlapping_size,
			    b.idx_scan as overlapping_idx_scan,
			    'OVERLAPPING' as redundancy_type
			FROM index_cols a
			JOIN index_cols b ON a.indrelid = b.indrelid
			    AND a.indexrelid != b.indexrelid
			    AND a.col_array[1:array_length(b.col_array, 1)] = b.col_array
			    AND array_length(a.col_array, 1) > array_length(b.col_array, 1)
			ORDER BY a.index_size DESC
			LIMIT 50
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				IndexRedundancy redundancy = new IndexRedundancy();
				redundancy.setSchemaName(rs.getString("schema_name"));
				redundancy.setTableName(rs.getString("table_name"));
				redundancy.setIndexName(rs.getString("index_name"));
				redundancy.setIndexSizeBytes(rs.getLong("index_size"));
				redundancy.setIndexScans(rs.getLong("idx_scan"));
				redundancy.setRelatedIndexName(rs.getString("overlapping_index"));
				redundancy.setRedundancyType(IndexRedundancy.RedundancyType.OVERLAPPING);
				redundancies.add(redundancy);
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to query index redundancy on %s: %s", instanceName, e.getMessage());
		}

		return redundancies;
	}

	/**
	 * Retrieves redundant and overlapping index analysis for the default instance.
	 *
	 * @return list of index redundancy findings
	 * @see #getIndexRedundancy(String)
	 */
	public List<IndexRedundancy> getIndexRedundancy() {
		return getIndexRedundancy("default");
	}

	// ========== Statistical Freshness Monitoring ==========

	/**
	 * Retrieves table statistics freshness for the specified instance.
	 * <p>
	 * Tracks when tables were last analysed and estimates how stale
	 * the statistics might be based on DML activity.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return list of statistical freshness metrics
	 */
	public List<StatisticalFreshness> getStatisticalFreshness(String instanceName) {
		List<StatisticalFreshness> results = new ArrayList<>();

		String sql = """
			SELECT
			    schemaname as schema_name,
			    relname as table_name,
			    n_live_tup as live_tuples,
			    n_dead_tup as dead_tuples,
			    n_mod_since_analyze as modified_since_analyze,
			    last_analyze,
			    last_autoanalyze,
			    GREATEST(last_analyze, last_autoanalyze) as last_stats_update,
			    n_tup_ins + n_tup_upd + n_tup_del as total_modifications,
			    CASE
			        WHEN n_live_tup = 0 THEN 0
			        ELSE ROUND(100.0 * n_mod_since_analyze / GREATEST(n_live_tup, 1), 2)
			    END as percent_modified
			FROM pg_stat_user_tables
			WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
			ORDER BY n_mod_since_analyze DESC NULLS LAST
			LIMIT 100
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				StatisticalFreshness freshness = new StatisticalFreshness();
				freshness.setSchemaName(rs.getString("schema_name"));
				freshness.setTableName(rs.getString("table_name"));
				freshness.setnLiveTup(rs.getLong("live_tuples"));
				freshness.setnDeadTup(rs.getLong("dead_tuples"));
				freshness.setnModSinceAnalyze(rs.getLong("modified_since_analyze"));

				Timestamp lastAnalyze = rs.getTimestamp("last_analyze");
				freshness.setLastAnalyze(lastAnalyze != null ? lastAnalyze.toInstant() : null);

				Timestamp lastAutoanalyze = rs.getTimestamp("last_autoanalyze");
				freshness.setLastAutoanalyze(lastAutoanalyze != null ? lastAutoanalyze.toInstant() : null);

				// Note: last_stats_update, total_modifications, percent_modified are calculated
				results.add(freshness);
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to query statistical freshness on %s: %s", instanceName, e.getMessage());
		}

		return results;
	}

	/**
	 * Retrieves table statistics freshness for the default instance.
	 *
	 * @return list of statistical freshness metrics
	 * @see #getStatisticalFreshness(String)
	 */
	public List<StatisticalFreshness> getStatisticalFreshness() {
		return getStatisticalFreshness("default");
	}

	// ========== Write/Read Ratio Analysis ==========

	/**
	 * Retrieves write/read ratio analysis for the specified instance.
	 * <p>
	 * Identifies table access patterns to classify tables as read-heavy,
	 * write-heavy, or balanced based on DML vs scan operations.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return list of write/read ratio metrics
	 */
	public List<WriteReadRatio> getWriteReadRatio(String instanceName) {
		List<WriteReadRatio> results = new ArrayList<>();

		String sql = """
			SELECT
			    schemaname as schema_name,
			    relname as table_name,
			    seq_scan,
			    seq_tup_read,
			    COALESCE(idx_scan, 0) as idx_scan,
			    COALESCE(idx_tup_fetch, 0) as idx_tup_fetch,
			    n_tup_ins as inserts,
			    n_tup_upd as updates,
			    n_tup_del as deletes,
			    n_tup_ins + n_tup_upd + n_tup_del as total_writes,
			    seq_scan + COALESCE(idx_scan, 0) as total_scans,
			    n_live_tup as live_tuples
			FROM pg_stat_user_tables
			WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
			ORDER BY (n_tup_ins + n_tup_upd + n_tup_del + seq_scan + COALESCE(idx_scan, 0)) DESC
			LIMIT 100
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				WriteReadRatio ratio = new WriteReadRatio();
				ratio.setSchemaName(rs.getString("schema_name"));
				ratio.setTableName(rs.getString("table_name"));
				ratio.setSeqScan(rs.getLong("seq_scan"));
				ratio.setSeqTupRead(rs.getLong("seq_tup_read"));
				ratio.setIdxScan(rs.getLong("idx_scan"));
				ratio.setIdxTupFetch(rs.getLong("idx_tup_fetch"));
				ratio.setnTupIns(rs.getLong("inserts"));
				ratio.setnTupUpd(rs.getLong("updates"));
				ratio.setnTupDel(rs.getLong("deletes"));
				ratio.setnLiveTup(rs.getLong("live_tuples"));
				// Note: total_writes and total_scans are calculated by the model
				results.add(ratio);
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to query write/read ratio on %s: %s", instanceName, e.getMessage());
		}

		return results;
	}

	/**
	 * Retrieves write/read ratio analysis for the default instance.
	 *
	 * @return list of write/read ratio metrics
	 * @see #getWriteReadRatio(String)
	 */
	public List<WriteReadRatio> getWriteReadRatio() {
		return getWriteReadRatio("default");
	}

	// ========== HOT Update Efficiency ==========

	/**
	 * Retrieves HOT (Heap-Only Tuple) update efficiency for the specified instance.
	 * <p>
	 * HOT updates avoid index maintenance when only non-indexed columns change.
	 * Low HOT efficiency indicates potential for fill factor tuning.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return list of HOT efficiency metrics
	 */
	public List<HotUpdateEfficiency> getHotEfficiency(String instanceName) {
		List<HotUpdateEfficiency> results = new ArrayList<>();

		String sql = """
			SELECT
			    schemaname as schema_name,
			    relname as table_name,
			    n_tup_upd as total_updates,
			    n_tup_hot_upd as hot_updates,
			    CASE
			        WHEN n_tup_upd = 0 THEN 100.0
			        ELSE ROUND(100.0 * n_tup_hot_upd / n_tup_upd, 2)
			    END as hot_ratio_percent,
			    n_live_tup as live_tuples,
			    n_dead_tup as dead_tuples,
			    pg_total_relation_size(schemaname || '.' || relname) as table_size_bytes
			FROM pg_stat_user_tables
			WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
			  AND n_tup_upd > 0
			ORDER BY n_tup_upd DESC
			LIMIT 100
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				HotUpdateEfficiency efficiency = new HotUpdateEfficiency();
				efficiency.setSchemaName(rs.getString("schema_name"));
				efficiency.setTableName(rs.getString("table_name"));
				efficiency.setnTupUpd(rs.getLong("total_updates"));
				efficiency.setnTupHotUpd(rs.getLong("hot_updates"));
				efficiency.setnLiveTup(rs.getLong("live_tuples"));
				efficiency.setnDeadTup(rs.getLong("dead_tuples"));
				efficiency.setTableSizeBytes(rs.getLong("table_size_bytes"));
				results.add(efficiency);
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to query HOT efficiency on %s: %s", instanceName, e.getMessage());
		}

		return results;
	}

	/**
	 * Retrieves HOT (Heap-Only Tuple) update efficiency for the default instance.
	 *
	 * @return list of HOT efficiency metrics
	 * @see #getHotEfficiency(String)
	 */
	public List<HotUpdateEfficiency> getHotEfficiency() {
		return getHotEfficiency("default");
	}

	// ========== Column Correlation Statistics ==========

	/**
	 * Retrieves column correlation statistics for CLUSTER recommendations.
	 * <p>
	 * Low correlation between physical and logical row ordering can hurt
	 * range query performance. CLUSTER can improve this.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return list of column correlation metrics
	 */
	public List<ColumnCorrelation> getColumnCorrelation(String instanceName) {
		List<ColumnCorrelation> results = new ArrayList<>();

		String sql = """
			SELECT
			    s.schemaname as schema_name,
			    s.tablename as table_name,
			    s.attname as column_name,
			    s.correlation,
			    s.n_distinct,
			    s.null_frac,
			    pg_total_relation_size(s.schemaname || '.' || s.tablename) as table_size_bytes,
			    t.seq_scan,
			    t.idx_scan,
			    i.indexname as index_name
			FROM pg_stats s
			JOIN pg_stat_user_tables t ON s.schemaname = t.schemaname AND s.tablename = t.relname
			LEFT JOIN pg_indexes i ON s.schemaname = i.schemaname
			    AND s.tablename = i.tablename
			    AND i.indexdef LIKE '%(' || s.attname || ')%'
			WHERE s.schemaname NOT IN ('pg_catalog', 'information_schema')
			  AND s.correlation IS NOT NULL
			  AND ABS(s.correlation) < 0.9
			ORDER BY ABS(s.correlation) ASC, pg_total_relation_size(s.schemaname || '.' || s.tablename) DESC
			LIMIT 100
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				ColumnCorrelation corr = new ColumnCorrelation();
				corr.setSchemaName(rs.getString("schema_name"));
				corr.setTableName(rs.getString("table_name"));
				corr.setColumnName(rs.getString("column_name"));
				corr.setCorrelation(rs.getDouble("correlation"));
				corr.setnDistinct(rs.getLong("n_distinct"));
				corr.setNullFrac(rs.getDouble("null_frac"));
				corr.setTableSizeBytes(rs.getLong("table_size_bytes"));
				corr.setSeqScan(rs.getLong("seq_scan"));
				corr.setIdxScan(rs.getLong("idx_scan"));
				corr.setIndexName(rs.getString("index_name"));
				results.add(corr);
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to query column correlation on %s: %s", instanceName, e.getMessage());
		}

		return results;
	}

	/**
	 * Retrieves column correlation statistics for the default instance.
	 *
	 * @return list of column correlation metrics
	 * @see #getColumnCorrelation(String)
	 */
	public List<ColumnCorrelation> getColumnCorrelation() {
		return getColumnCorrelation("default");
	}

	// ========== XID Wraparound Monitoring ==========

	/**
	 * Retrieves XID (Transaction ID) wraparound risk metrics for the specified instance.
	 * <p>
	 * PostgreSQL uses 32-bit transaction IDs that wrap around. Aggressive vacuuming
	 * is required to prevent wraparound, which would cause data loss.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return list of XID wraparound metrics per database
	 */
	public List<XidWraparound> getXidWraparound(String instanceName) {
		List<XidWraparound> results = new ArrayList<>();

		String sql = """
			SELECT
			    d.datname as database_name,
			    d.datfrozenxid::text::bigint as datfrozenxid,
			    age(d.datfrozenxid) as xid_age,
			    ROUND(100.0 * age(d.datfrozenxid) / 2147483647, 2) as percent_to_wraparound,
			    (SELECT setting::bigint FROM pg_settings WHERE name = 'autovacuum_freeze_max_age') as autovacuum_freeze_max_age
			FROM pg_database d
			WHERE d.datistemplate = false
			ORDER BY age(d.datfrozenxid) DESC
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				XidWraparound xid = new XidWraparound();
				xid.setDatabaseName(rs.getString("database_name"));
				xid.setDatfrozenxid(rs.getLong("datfrozenxid"));
				xid.setXidAge(rs.getLong("xid_age"));
				xid.setPercentToWraparound(rs.getLong("percent_to_wraparound"));
				xid.setAutovacuumFreezeMaxAge(rs.getLong("autovacuum_freeze_max_age"));
				results.add(xid);
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to query XID wraparound on %s: %s", instanceName, e.getMessage());
		}

		// Get oldest unfrozen table for the CURRENT database only
		// (We can only query pg_class for the database we're connected to)
		String oldestTableSql = """
			SELECT
			    current_database() as current_db,
			    n.nspname as schema_name,
			    c.relname as table_name,
			    c.relfrozenxid::text::bigint as relfrozenxid,
			    age(c.relfrozenxid) as rel_xid_age
			FROM pg_class c
			JOIN pg_namespace n ON c.relnamespace = n.oid
			WHERE c.relkind = 'r'
			  AND n.nspname NOT IN ('pg_catalog', 'information_schema')
			ORDER BY age(c.relfrozenxid) DESC
			LIMIT 1
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(oldestTableSql)) {
			if (rs.next()) {
				String currentDb = rs.getString("current_db");
				String schemaName = rs.getString("schema_name");
				String tableName = rs.getString("table_name");
				long relXidAge = rs.getLong("rel_xid_age");

				// Only assign to the matching database record
				for (XidWraparound xid : results) {
					if (currentDb.equals(xid.getDatabaseName())) {
						xid.setOldestXidSchema(schemaName);
						xid.setOldestXidTable(tableName);
						xid.setOldestRelFrozenXid(relXidAge);
						break;
					}
				}
			}
		} catch (SQLException e) {
			LOG.debugf("Could not get oldest table for XID on %s: %s", instanceName, e.getMessage());
		}

		return results;
	}

	/**
	 * Retrieves XID (Transaction ID) wraparound risk metrics for the default instance.
	 *
	 * @return list of XID wraparound metrics per database
	 * @see #getXidWraparound(String)
	 */
	public List<XidWraparound> getXidWraparound() {
		return getXidWraparound("default");
	}

	// ========== Live Chart Data ==========

	/**
	 * Retrieves live chart data for connection monitoring.
	 * <p>
	 * Returns current connection counts by state for real-time charting.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return LiveChartData with connection series
	 */
	public LiveChartData getConnectionsChartData(String instanceName) {
		LiveChartData chart = LiveChartData.createConnectionsChart();
		chart.setLastUpdated(Instant.now());

		String sql = """
			SELECT
			    COUNT(*) FILTER (WHERE state = 'active') as active,
			    COUNT(*) FILTER (WHERE state = 'idle') as idle,
			    COUNT(*) FILTER (WHERE state = 'idle in transaction') as idle_in_transaction
			FROM pg_stat_activity
			WHERE pid != pg_backend_pid()
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			if (rs.next()) {
				Instant now = Instant.now();
				chart.getSeriesByName("Active").addPoint(now, rs.getDouble("active"));
				chart.getSeriesByName("Idle").addPoint(now, rs.getDouble("idle"));
				chart.getSeriesByName("Idle in Transaction").addPoint(now, rs.getDouble("idle_in_transaction"));
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get connections chart data on %s: %s", instanceName, e.getMessage());
		}

		return chart;
	}

	/**
	 * Retrieves live chart data for connection monitoring on the default instance.
	 *
	 * @return LiveChartData with connection series
	 * @see #getConnectionsChartData(String)
	 */
	public LiveChartData getConnectionsChartData() {
		return getConnectionsChartData("default");
	}

	/**
	 * Retrieves live chart data for transaction rate monitoring.
	 * <p>
	 * Returns current commit and rollback counts for rate calculation.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return LiveChartData with transaction series
	 */
	public LiveChartData getTransactionsChartData(String instanceName) {
		LiveChartData chart = LiveChartData.createTransactionsChart();
		chart.setLastUpdated(Instant.now());

		String sql = """
			SELECT
			    xact_commit as commits,
			    xact_rollback as rollbacks
			FROM pg_stat_database
			WHERE datname = current_database()
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			if (rs.next()) {
				Instant now = Instant.now();
				chart.getSeriesByName("Commits").addPoint(now, rs.getDouble("commits"));
				chart.getSeriesByName("Rollbacks").addPoint(now, rs.getDouble("rollbacks"));
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get transactions chart data on %s: %s", instanceName, e.getMessage());
		}

		return chart;
	}

	/**
	 * Retrieves live chart data for transaction rate monitoring on the default instance.
	 *
	 * @return LiveChartData with transaction series
	 * @see #getTransactionsChartData(String)
	 */
	public LiveChartData getTransactionsChartData() {
		return getTransactionsChartData("default");
	}

	/**
	 * Retrieves live chart data for tuple operations monitoring.
	 * <p>
	 * Returns current insert, update, and delete counts.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return LiveChartData with tuple operation series
	 */
	public LiveChartData getTuplesChartData(String instanceName) {
		LiveChartData chart = LiveChartData.createTuplesChart();
		chart.setLastUpdated(Instant.now());

		String sql = """
			SELECT
			    tup_inserted as inserted,
			    tup_updated as updated,
			    tup_deleted as deleted
			FROM pg_stat_database
			WHERE datname = current_database()
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			if (rs.next()) {
				Instant now = Instant.now();
				chart.getSeriesByName("Inserted").addPoint(now, rs.getDouble("inserted"));
				chart.getSeriesByName("Updated").addPoint(now, rs.getDouble("updated"));
				chart.getSeriesByName("Deleted").addPoint(now, rs.getDouble("deleted"));
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get tuples chart data on %s: %s", instanceName, e.getMessage());
		}

		return chart;
	}

	/**
	 * Retrieves live chart data for tuple operations monitoring on the default instance.
	 *
	 * @return LiveChartData with tuple operation series
	 * @see #getTuplesChartData(String)
	 */
	public LiveChartData getTuplesChartData() {
		return getTuplesChartData("default");
	}

	/**
	 * Retrieves live chart data for cache hit ratio monitoring.
	 * <p>
	 * Returns current buffer and index cache hit ratios.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return LiveChartData with cache hit ratio series
	 */
	public LiveChartData getCacheChartData(String instanceName) {
		LiveChartData chart = LiveChartData.createCacheChart();
		chart.setLastUpdated(Instant.now());

		String sql = """
			SELECT
			    CASE WHEN (blks_hit + blks_read) = 0 THEN 100.0
			         ELSE 100.0 * blks_hit / (blks_hit + blks_read)
			    END as buffer_hit_ratio
			FROM pg_stat_database
			WHERE datname = current_database()
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			if (rs.next()) {
				Instant now = Instant.now();
				chart.getSeriesByName("Buffer Cache").addPoint(now, rs.getDouble("buffer_hit_ratio"));
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get cache chart data on %s: %s", instanceName, e.getMessage());
		}

		// Get index hit ratio separately
		String indexSql = """
			SELECT
			    CASE WHEN (idx_blks_hit + idx_blks_read) = 0 THEN 100.0
			         ELSE 100.0 * idx_blks_hit / (idx_blks_hit + idx_blks_read)
			    END as index_hit_ratio
			FROM pg_statio_user_tables
			WHERE idx_blks_hit + idx_blks_read > 0
			LIMIT 1
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(indexSql)) {
			if (rs.next()) {
				Instant now = Instant.now();
				chart.getSeriesByName("Index Cache").addPoint(now, rs.getDouble("index_hit_ratio"));
			}
		} catch (SQLException e) {
			// Index cache data may not be available
			LOG.debugf("Could not get index cache data on %s: %s", instanceName, e.getMessage());
		}

		return chart;
	}

	/**
	 * Retrieves live chart data for cache hit ratio monitoring on the default instance.
	 *
	 * @return LiveChartData with cache hit ratio series
	 * @see #getCacheChartData(String)
	 */
	public LiveChartData getCacheChartData() {
		return getCacheChartData("default");
	}

	/**
	 * Retrieves PostgreSQL configuration settings with health status assessment.
	 * <p>
	 * Evaluates critical configuration parameters and flags potential issues
	 * such as suboptimal memory settings, disabled features, or missing extensions.
	 *
	 * @param instanceName the name of the PostgreSQL instance
	 * @return list of configuration settings with status indicators
	 */
	public List<ConfigSetting> getConfigurationHealth(String instanceName) {
		List<ConfigSetting> settings = new ArrayList<>();

		String sql = """
			SELECT name, setting, unit, source, short_desc
			FROM pg_settings
			WHERE name IN (
			    'max_connections', 'shared_buffers', 'work_mem', 'maintenance_work_mem',
			    'effective_cache_size', 'checkpoint_timeout', 'max_wal_size', 'min_wal_size',
			    'wal_compression', 'autovacuum', 'autovacuum_max_workers',
			    'autovacuum_vacuum_scale_factor', 'autovacuum_analyze_scale_factor',
			    'log_min_duration_statement', 'log_lock_waits', 'deadlock_timeout',
			    'track_io_timing', 'track_functions', 'track_activity_query_size',
			    'random_page_cost', 'effective_io_concurrency', 'huge_pages'
			)
			ORDER BY name
			""";

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				String name = rs.getString("name");
				String value = rs.getString("setting");
				String unit = rs.getString("unit");
				String source = rs.getString("source");
				String desc = rs.getString("short_desc");

				ConfigSetting setting = evaluateSetting(name, value, unit, source, desc);
				settings.add(setting);
			}
		} catch (SQLException e) {
			LOG.warnf("Could not query pg_settings on %s: %s", instanceName, e.getMessage());
		}

		// Check for pg_stat_statements extension
		settings.add(checkPgStatStatements(instanceName));

		return settings;
	}

	/**
	 * Evaluates a PostgreSQL setting and determines its health status.
	 */
	private ConfigSetting evaluateSetting(String name, String value, String unit, String source, String desc) {
		ConfigSetting.Status status = ConfigSetting.Status.OK;
		ConfigSetting.Category category = ConfigSetting.Category.MONITORING;
		String recommendation = null;
		String recommendedValue = null;

		switch (name) {
			case "max_connections" -> {
				category = ConfigSetting.Category.CONNECTIONS;
				int val = Integer.parseInt(value);
				if (val > 500) {
					status = ConfigSetting.Status.WARNING;
					recommendation = "High max_connections can cause memory pressure. Consider using connection pooling (PgBouncer/PgPool).";
					recommendedValue = "<= 200 with pooler";
				} else if (val > 200) {
					status = ConfigSetting.Status.INFO;
					recommendation = "Consider connection pooling for better resource utilisation.";
				}
			}
			case "shared_buffers" -> {
				category = ConfigSetting.Category.MEMORY;
				long bytes = parseBytes(value, unit);
				long mb = bytes / (1024 * 1024);
				if (mb < 128) {
					status = ConfigSetting.Status.CRITICAL;
					recommendation = "shared_buffers is very low. Should be 25% of system RAM or at least 1GB.";
					recommendedValue = ">= 1GB (25% RAM)";
				} else if (mb < 1024) {
					status = ConfigSetting.Status.WARNING;
					recommendation = "shared_buffers may be too low for production. Consider 25% of system RAM.";
					recommendedValue = ">= 1GB (25% RAM)";
				}
			}
			case "work_mem" -> {
				category = ConfigSetting.Category.MEMORY;
				long bytes = parseBytes(value, unit);
				long mb = bytes / (1024 * 1024);
				if (mb < 4) {
					status = ConfigSetting.Status.WARNING;
					recommendation = "Low work_mem can cause sorts/hashes to spill to disk.";
					recommendedValue = ">= 4MB";
				} else if (mb > 256) {
					status = ConfigSetting.Status.INFO;
					recommendation = "High work_mem. Ensure you account for concurrent connections.";
				}
			}
			case "maintenance_work_mem" -> {
				category = ConfigSetting.Category.MEMORY;
				long bytes = parseBytes(value, unit);
				long mb = bytes / (1024 * 1024);
				if (mb < 64) {
					status = ConfigSetting.Status.WARNING;
					recommendation = "Low maintenance_work_mem can slow VACUUM and index creation.";
					recommendedValue = ">= 256MB";
				}
			}
			case "effective_cache_size" -> {
				category = ConfigSetting.Category.MEMORY;
				// This is a hint to the planner, hard to assess without knowing RAM
			}
			case "checkpoint_timeout" -> {
				category = ConfigSetting.Category.WAL;
				int seconds = Integer.parseInt(value);
				if (seconds < 300) {
					status = ConfigSetting.Status.WARNING;
					recommendation = "Frequent checkpoints increase I/O overhead. Consider 5-15 minutes.";
					recommendedValue = ">= 5min";
				}
			}
			case "max_wal_size" -> {
				category = ConfigSetting.Category.WAL;
				long bytes = parseBytes(value, unit);
				long gb = bytes / (1024 * 1024 * 1024);
				if (gb < 1) {
					status = ConfigSetting.Status.WARNING;
					recommendation = "Small max_wal_size causes frequent checkpoints. Consider 2-8GB.";
					recommendedValue = ">= 2GB";
				}
			}
			case "min_wal_size", "wal_compression" -> category = ConfigSetting.Category.WAL;
			case "autovacuum" -> {
				category = ConfigSetting.Category.VACUUM;
				if ("off".equals(value)) {
					status = ConfigSetting.Status.CRITICAL;
					recommendation = "Autovacuum is disabled! This will cause table bloat and transaction ID wraparound.";
					recommendedValue = "on";
				}
			}
			case "autovacuum_max_workers", "autovacuum_vacuum_scale_factor", "autovacuum_analyze_scale_factor" -> category = ConfigSetting.Category.VACUUM;
			case "log_min_duration_statement" -> {
				category = ConfigSetting.Category.LOGGING;
				int ms = Integer.parseInt(value);
				if (ms == -1) {
					status = ConfigSetting.Status.INFO;
					recommendation = "Slow query logging is disabled. Consider enabling (e.g., 250ms-1000ms) for diagnostics.";
					recommendedValue = "250-1000 ms";
				}
			}
			case "log_lock_waits" -> {
				category = ConfigSetting.Category.LOGGING;
				if ("off".equals(value)) {
					status = ConfigSetting.Status.INFO;
					recommendation = "Enable log_lock_waits to detect lock contention issues.";
					recommendedValue = "on";
				}
			}
			case "deadlock_timeout" -> category = ConfigSetting.Category.LOGGING;
			case "track_io_timing" -> {
				category = ConfigSetting.Category.MONITORING;
				if ("off".equals(value)) {
					status = ConfigSetting.Status.WARNING;
					recommendation = "Enable track_io_timing for I/O diagnostics in pg_stat_statements. Low overhead, high value.";
					recommendedValue = "on";
				}
			}
			case "track_functions", "track_activity_query_size" -> category = ConfigSetting.Category.MONITORING;
			case "random_page_cost" -> {
				category = ConfigSetting.Category.MONITORING;
				double cost = Double.parseDouble(value);
				if (cost > 2.0) {
					status = ConfigSetting.Status.INFO;
					recommendation = "For SSD storage, random_page_cost can be lowered (1.1-1.5) to encourage index usage.";
				}
			}
			case "effective_io_concurrency" -> {
				category = ConfigSetting.Category.MONITORING;
				int val = Integer.parseInt(value);
				if (val <= 1) {
					status = ConfigSetting.Status.INFO;
					recommendation = "For SSD storage, increase effective_io_concurrency (e.g., 200) to improve parallel I/O.";
				}
			}
			case "huge_pages" -> category = ConfigSetting.Category.MEMORY;
		}

		return ConfigSetting.builder().name(name).currentValue(value).unit(unit != null ? unit : "").source(source).description(desc).status(status).category(category).recommendation(recommendation).recommendedValue(recommendedValue).build();
	}

	/**
	 * Parses a PostgreSQL size value to bytes.
	 */
	private long parseBytes(String value, String unit) {
		long val = Long.parseLong(value);
		if (unit == null) return val;
		return switch (unit) {
			case "8kB" -> val * 8 * 1024;
			case "kB" -> val * 1024;
			case "MB" -> val * 1024 * 1024;
			case "GB" -> val * 1024 * 1024 * 1024;
			case "TB" -> val * 1024L * 1024 * 1024 * 1024;
			default -> val;
		};
	}

	/**
	 * Checks if pg_stat_statements extension is installed.
	 */
	private ConfigSetting checkPgStatStatements(String instanceName) {
		String sql = "SELECT COUNT(*) FROM pg_extension WHERE extname = 'pg_stat_statements'";
		boolean installed = false;

		try (Connection conn = getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			if (rs.next()) {
				installed = rs.getInt(1) > 0;
			}
		} catch (SQLException e) {
			LOG.debugf("Could not check pg_stat_statements on %s: %s", instanceName, e.getMessage());
		}

		ConfigSetting.Status status = installed ? ConfigSetting.Status.OK : ConfigSetting.Status.WARNING;
		String recommendation = installed ? null : "pg_stat_statements is not installed. This extension is essential for query performance monitoring.";

		return ConfigSetting.builder()
			.name("pg_stat_statements")
			.currentValue(installed ? "installed" : "not installed")
			.unit("")
			.source("extension")
			.description("Query performance statistics extension")
			.status(status)
			.category(ConfigSetting.Category.EXTENSIONS)
			.recommendation(recommendation)
			.recommendedValue(installed ? null : "CREATE EXTENSION pg_stat_statements")
			.build();
	}

	/**
	 * Retrieves configuration health for the default instance.
	 *
	 * @return list of configuration settings with status indicators
	 * @see #getConfigurationHealth(String)
	 */
	public List<ConfigSetting> getConfigurationHealth() {
		return getConfigurationHealth("default");
	}

	// ========== Deadlock Monitoring ==========

	/**
	 * Retrieves deadlock statistics for all databases on the specified instance.
	 * <p>
	 * Queries pg_stat_database for cumulative deadlock counts and calculates
	 * deadlock rates based on historical sampling data when available.
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return list of deadlock statistics per database, ordered by count descending
	 */
	public List<DeadlockStats> getDeadlockStats(String instanceName) {
		List<DeadlockStats> stats = new ArrayList<>();

		String sql = """
			SELECT
			    datname,
			    deadlocks,
			    stats_reset
			FROM pg_stat_database
			WHERE datname NOT LIKE 'template%'
			  AND datname IS NOT NULL
			ORDER BY deadlocks DESC
			""";

		try (Connection conn = getDataSource(instanceName).getConnection();
		     Statement stmt = conn.createStatement();
		     ResultSet rs = stmt.executeQuery(sql)) {

			while (rs.next()) {
				DeadlockStats stat = new DeadlockStats();
				stat.setDatabaseName(rs.getString("datname"));
				stat.setDeadlockCount(rs.getLong("deadlocks"));

				Timestamp statsReset = rs.getTimestamp("stats_reset");
				if (statsReset != null) {
					stat.setStatsReset(statsReset.toInstant());
				}

				// Default rate to -1 (unavailable) - will be populated by caller if history is available
				stat.setDeadlocksPerHour(-1);
				stat.setSparklineSvg("");

				stats.add(stat);
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get deadlock stats for %s: %s", instanceName, e.getMessage());
		}

		return stats;
	}

	/**
	 * Retrieves deadlock statistics for all databases on the default instance.
	 *
	 * @return list of deadlock statistics per database
	 * @see #getDeadlockStats(String)
	 */
	public List<DeadlockStats> getDeadlockStats() {
		return getDeadlockStats("default");
	}

	/**
	 * Retrieves deadlock-related PostgreSQL configuration settings for the specified instance.
	 * <p>
	 * Returns settings that affect deadlock detection and logging, including:
	 * <ul>
	 *   <li>deadlock_timeout - time to wait before checking for deadlocks</li>
	 *   <li>log_lock_waits - whether to log lock wait events</li>
	 *   <li>lock_timeout - maximum time to wait for a lock</li>
	 * </ul>
	 *
	 * @param instanceName the name of the PostgreSQL instance to query
	 * @return DeadlockConfig containing current settings and recommendations
	 */
	public DeadlockConfig getDeadlockConfig(String instanceName) {
		DeadlockConfig config = new DeadlockConfig();

		String sql = """
			SELECT name, setting
			FROM pg_settings
			WHERE name IN ('deadlock_timeout', 'log_lock_waits', 'lock_timeout')
			""";

		try (Connection conn = getDataSource(instanceName).getConnection();
		     Statement stmt = conn.createStatement();
		     ResultSet rs = stmt.executeQuery(sql)) {

			while (rs.next()) {
				String name = rs.getString("name");
				String value = rs.getString("setting");

				switch (name) {
					case "deadlock_timeout" -> config.setDeadlockTimeout(value);
					case "log_lock_waits" -> config.setLogLockWaits("on".equalsIgnoreCase(value));
					case "lock_timeout" -> config.setLockTimeout(value);
				}
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get deadlock config for %s: %s", instanceName, e.getMessage());
		}

		return config;
	}

	/**
	 * Retrieves deadlock-related PostgreSQL configuration settings for the default instance.
	 *
	 * @return DeadlockConfig containing current settings and recommendations
	 * @see #getDeadlockConfig(String)
	 */
	public DeadlockConfig getDeadlockConfig() {
		return getDeadlockConfig("default");
	}

	/**
	 * Calculates the deadlock rate per hour for a database based on historical samples.
	 * <p>
	 * Uses the oldest and newest samples in the history to calculate the rate
	 * of deadlock increase over the sampling period.
	 *
	 * @param history list of database metrics history ordered by time ascending
	 * @return deadlocks per hour, or -1 if insufficient data
	 */
	public double calculateDeadlockRate(List<DatabaseMetricsHistory> history) {
		if (history == null || history.size() < 2) {
			return -1;
		}

		DatabaseMetricsHistory oldest = history.get(0);
		DatabaseMetricsHistory newest = history.get(history.size() - 1);

		Long oldestDeadlocks = oldest.getDeadlocks();
		Long newestDeadlocks = newest.getDeadlocks();

		if (oldestDeadlocks == null || newestDeadlocks == null) {
			return -1;
		}

		long deadlockDelta = newestDeadlocks - oldestDeadlocks;
		if (deadlockDelta < 0) {
			// Stats were reset, use newest value as the count since reset
			deadlockDelta = newestDeadlocks;
		}

		Instant oldestTime = oldest.getSampledAt();
		Instant newestTime = newest.getSampledAt();

		if (oldestTime == null || newestTime == null) {
			return -1;
		}

		long hoursBetween = java.time.Duration.between(oldestTime, newestTime).toHours();
		if (hoursBetween < 1) {
			// Less than an hour of data, extrapolate from minutes
			long minutesBetween = java.time.Duration.between(oldestTime, newestTime).toMinutes();
			if (minutesBetween < 1) {
				return -1;
			}
			return (double) deadlockDelta * 60 / minutesBetween;
		}

		return (double) deadlockDelta / hoursBetween;
	}
}
