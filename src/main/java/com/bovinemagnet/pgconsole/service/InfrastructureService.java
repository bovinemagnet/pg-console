package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.ArchiverStats;
import com.bovinemagnet.pgconsole.model.BgWriterStats;
import com.bovinemagnet.pgconsole.model.CheckpointStats;
import com.bovinemagnet.pgconsole.model.VacuumProgress;
import com.bovinemagnet.pgconsole.model.WalCheckpointOverview;
import com.bovinemagnet.pgconsole.model.WalStats;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Service for monitoring PostgreSQL infrastructure.
 * Includes vacuum progress, background processes, and storage insights.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class InfrastructureService {

	private static final Logger LOG = Logger.getLogger(InfrastructureService.class);

	@Inject
	DataSourceManager dataSourceManager;

	// ========== Vacuum Progress ==========

	/**
	 * Retrieves active vacuum operations from {@code pg_stat_progress_vacuum}.
	 * <p>
	 * Monitors ongoing VACUUM operations including both manual and autovacuum processes.
	 * Automatically detects PostgreSQL version and adapts queries accordingly:
	 * <ul>
	 * <li>PostgreSQL 17+: Uses renamed columns (dead_tuple_bytes, max_dead_tuple_bytes)</li>
	 * <li>PostgreSQL 12-16: Uses original column names (num_dead_tuples, max_dead_tuples)</li>
	 * </ul>
	 * Progress percentage is calculated based on heap blocks scanned versus total heap blocks.
	 *
	 * @param instanceName the database instance identifier
	 * @return list of active vacuum operations with progress metrics
	 */
	public List<VacuumProgress> getVacuumProgress(String instanceName) {
		List<VacuumProgress> vacuums = new ArrayList<>();

		// Check if we're on PG 17+ (columns renamed: num_dead_tuples -> dead_tuple_bytes)
		boolean isPg17Plus = false;
		try (
			Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT 1 FROM information_schema.columns " + "WHERE table_name = 'pg_stat_progress_vacuum' AND column_name = 'dead_tuple_bytes'")
		) {
			isPg17Plus = rs.next();
		} catch (SQLException e) {
			LOG.debugf("Could not check vacuum progress columns: %s", e.getMessage());
		}

		String sql;
		if (isPg17Plus) {
			// PostgreSQL 17+ - columns renamed to *_bytes
			sql = """
				SELECT
				    p.pid,
				    d.datname as database,
				    n.nspname as schema_name,
				    c.relname as table_name,
				    p.phase,
				    p.heap_blks_total,
				    p.heap_blks_scanned,
				    p.heap_blks_vacuumed,
				    p.index_vacuum_count,
				    p.dead_tuple_bytes as num_dead_tuples,
				    p.max_dead_tuple_bytes as max_dead_tuples,
				    a.query LIKE 'autovacuum:%' as is_autovacuum
				FROM pg_stat_progress_vacuum p
				JOIN pg_database d ON d.oid = p.datid
				JOIN pg_class c ON c.oid = p.relid
				JOIN pg_namespace n ON n.oid = c.relnamespace
				LEFT JOIN pg_stat_activity a ON a.pid = p.pid
				ORDER BY p.pid
				""";
		} else {
			// PostgreSQL 12-16
			sql = """
				SELECT
				    p.pid,
				    d.datname as database,
				    n.nspname as schema_name,
				    c.relname as table_name,
				    p.phase,
				    p.heap_blks_total,
				    p.heap_blks_scanned,
				    p.heap_blks_vacuumed,
				    p.index_vacuum_count,
				    p.num_dead_tuples,
				    p.max_dead_tuples,
				    a.query LIKE 'autovacuum:%' as is_autovacuum
				FROM pg_stat_progress_vacuum p
				JOIN pg_database d ON d.oid = p.datid
				JOIN pg_class c ON c.oid = p.relid
				JOIN pg_namespace n ON n.oid = c.relnamespace
				LEFT JOIN pg_stat_activity a ON a.pid = p.pid
				ORDER BY p.pid
				""";
		}

		try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				VacuumProgress vp = new VacuumProgress();
				vp.setPid(rs.getInt("pid"));
				vp.setDatabase(rs.getString("database"));
				vp.setSchemaName(rs.getString("schema_name"));
				vp.setTableName(rs.getString("table_name"));
				vp.setPhaseFromString(rs.getString("phase"));
				vp.setHeapBlksTotal(rs.getLong("heap_blks_total"));
				vp.setHeapBlksScanned(rs.getLong("heap_blks_scanned"));
				vp.setHeapBlksVacuumed(rs.getLong("heap_blks_vacuumed"));
				vp.setIndexVacuumCount(rs.getLong("index_vacuum_count"));
				vp.setNumDeadTuples(rs.getLong("num_dead_tuples"));
				vp.setMaxDeadTuples(rs.getLong("max_dead_tuples"));
				vp.setAutovacuum(rs.getBoolean("is_autovacuum"));
				vp.calculateProgress();

				vacuums.add(vp);
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get vacuum progress for %s: %s", instanceName, e.getMessage());
		}

		return vacuums;
	}

	// ========== Background Processes ==========

	/**
	 * Retrieves comprehensive background process statistics.
	 * <p>
	 * Collects statistics about autovacuum workers, checkpointer, background writer,
	 * and other PostgreSQL background processes. Automatically adapts to PostgreSQL version:
	 * <ul>
	 * <li>PostgreSQL 17+: Uses {@code pg_stat_checkpointer} and {@code pg_stat_bgwriter} views separately,
	 *     plus {@code pg_stat_io} for backend I/O statistics</li>
	 * <li>PostgreSQL 12-16: Uses consolidated {@code pg_stat_bgwriter} view for all statistics</li>
	 * </ul>
	 *
	 * @param instanceName the database instance identifier
	 * @return comprehensive background process statistics including autovacuum, checkpointer, and I/O metrics
	 */
	public BackgroundProcessStats getBackgroundProcessStats(String instanceName) {
		BackgroundProcessStats stats = new BackgroundProcessStats();

		// Autovacuum stats
		String autovacuumSql = """
			SELECT
			    (SELECT count(*) FROM pg_stat_activity WHERE backend_type = 'autovacuum launcher') as launcher_running,
			    (SELECT count(*) FROM pg_stat_activity WHERE backend_type = 'autovacuum worker') as workers_running,
			    (SELECT setting::int FROM pg_settings WHERE name = 'autovacuum_max_workers') as max_workers,
			    (SELECT setting FROM pg_settings WHERE name = 'autovacuum') as autovacuum_enabled
			""";

		try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(autovacuumSql)) {
			if (rs.next()) {
				stats.setAutovacuumLauncherRunning(rs.getInt("launcher_running") > 0);
				stats.setAutovacuumWorkersRunning(rs.getInt("workers_running"));
				stats.setMaxAutovacuumWorkers(rs.getInt("max_workers"));
				stats.setAutovacuumEnabled("on".equalsIgnoreCase(rs.getString("autovacuum_enabled")));
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get autovacuum stats for %s: %s", instanceName, e.getMessage());
		}

		// Check PostgreSQL version to determine which views to use
		// In PG 17+, checkpoint stats moved from pg_stat_bgwriter to pg_stat_checkpointer
		boolean hasPg17CheckpointerView = false;
		try (
			Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_catalog.pg_class WHERE relname = 'pg_stat_checkpointer' AND relkind = 'v'")
		) {
			hasPg17CheckpointerView = rs.next();
		} catch (SQLException e) {
			LOG.debugf("Could not check for pg_stat_checkpointer: %s", e.getMessage());
		}

		if (hasPg17CheckpointerView) {
			// PostgreSQL 17+ - use separate views for checkpointer and bgwriter
			String checkpointerSql = """
				SELECT
				    num_timed as checkpoints_timed,
				    num_requested as checkpoints_req,
				    write_time as checkpoint_write_time,
				    sync_time as checkpoint_sync_time,
				    buffers_written as buffers_checkpoint
				FROM pg_stat_checkpointer
				""";

			try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(checkpointerSql)) {
				if (rs.next()) {
					stats.setCheckpointsTimed(rs.getLong("checkpoints_timed"));
					stats.setCheckpointsReq(rs.getLong("checkpoints_req"));
					stats.setCheckpointWriteTime(rs.getDouble("checkpoint_write_time"));
					stats.setCheckpointSyncTime(rs.getDouble("checkpoint_sync_time"));
					stats.setBuffersCheckpoint(rs.getLong("buffers_checkpoint"));
				}
			} catch (SQLException e) {
				LOG.warnf("Failed to get checkpointer stats for %s: %s", instanceName, e.getMessage());
			}

			// Background writer stats (PG 17+)
			String bgWriterSql = """
				SELECT
				    buffers_clean,
				    maxwritten_clean,
				    buffers_alloc
				FROM pg_stat_bgwriter
				""";

			try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(bgWriterSql)) {
				if (rs.next()) {
					stats.setBuffersClean(rs.getLong("buffers_clean"));
					stats.setMaxwrittenClean(rs.getLong("maxwritten_clean"));
					stats.setBuffersAlloc(rs.getLong("buffers_alloc"));
				}
			} catch (SQLException e) {
				LOG.warnf("Failed to get bgwriter stats for %s: %s", instanceName, e.getMessage());
			}

			// Backend stats from pg_stat_io in PG 16+
			String ioSql = """
				SELECT
				    COALESCE(SUM(writes), 0) as buffers_backend,
				    COALESCE(SUM(fsyncs), 0) as buffers_backend_fsync
				FROM pg_stat_io
				WHERE backend_type = 'client backend'
				  AND context = 'normal'
				""";

			try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(ioSql)) {
				if (rs.next()) {
					stats.setBuffersBackend(rs.getLong("buffers_backend"));
					stats.setBuffersBackendFsync(rs.getLong("buffers_backend_fsync"));
				}
			} catch (SQLException e) {
				LOG.debugf("Failed to get IO stats for %s: %s", instanceName, e.getMessage());
			}
		} else {
			// PostgreSQL 12-16 - all stats in pg_stat_bgwriter
			String bgWriterSql = """
				SELECT
				    checkpoints_timed,
				    checkpoints_req,
				    checkpoint_write_time,
				    checkpoint_sync_time,
				    buffers_checkpoint,
				    buffers_clean,
				    maxwritten_clean,
				    buffers_backend,
				    buffers_backend_fsync,
				    buffers_alloc
				FROM pg_stat_bgwriter
				""";

			try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(bgWriterSql)) {
				if (rs.next()) {
					stats.setCheckpointsTimed(rs.getLong("checkpoints_timed"));
					stats.setCheckpointsReq(rs.getLong("checkpoints_req"));
					stats.setCheckpointWriteTime(rs.getDouble("checkpoint_write_time"));
					stats.setCheckpointSyncTime(rs.getDouble("checkpoint_sync_time"));
					stats.setBuffersCheckpoint(rs.getLong("buffers_checkpoint"));
					stats.setBuffersClean(rs.getLong("buffers_clean"));
					stats.setMaxwrittenClean(rs.getLong("maxwritten_clean"));
					stats.setBuffersBackend(rs.getLong("buffers_backend"));
					stats.setBuffersBackendFsync(rs.getLong("buffers_backend_fsync"));
					stats.setBuffersAlloc(rs.getLong("buffers_alloc"));
				}
			} catch (SQLException e) {
				LOG.warnf("Failed to get background writer stats for %s: %s", instanceName, e.getMessage());
			}
		}

		// Count other background processes
		String bgProcessSql = """
			SELECT backend_type, count(*) as count
			FROM pg_stat_activity
			WHERE backend_type IS NOT NULL
			GROUP BY backend_type
			ORDER BY count DESC
			""";

		try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(bgProcessSql)) {
			List<BackgroundProcess> processes = new ArrayList<>();
			while (rs.next()) {
				BackgroundProcess bp = new BackgroundProcess();
				bp.setType(rs.getString("backend_type"));
				bp.setCount(rs.getInt("count"));
				processes.add(bp);
			}
			stats.setBackgroundProcesses(processes);
		} catch (SQLException e) {
			LOG.warnf("Failed to get background processes for %s: %s", instanceName, e.getMessage());
		}

		return stats;
	}

	// ========== Storage Insights ==========

	/**
	 * Retrieves comprehensive storage statistics including tablespaces, databases, and WAL.
	 * <p>
	 * Collects information about:
	 * <ul>
	 * <li>Tablespace locations and sizes</li>
	 * <li>Database sizes (excluding template databases)</li>
	 * <li>Write-Ahead Logging (WAL) configuration and current usage</li>
	 * <li>Temporary file statistics across all databases</li>
	 * </ul>
	 *
	 * @param instanceName the database instance identifier
	 * @return storage statistics including tablespace, database, WAL, and temporary file metrics
	 */
	public StorageStats getStorageStats(String instanceName) {
		StorageStats stats = new StorageStats();

		// Tablespace info
		String tablespaceSql = """
			SELECT
			    spcname as name,
			    pg_tablespace_location(oid) as location,
			    pg_tablespace_size(oid) as size_bytes
			FROM pg_tablespace
			ORDER BY pg_tablespace_size(oid) DESC
			""";

		try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(tablespaceSql)) {
			List<TablespaceInfo> tablespaces = new ArrayList<>();
			while (rs.next()) {
				TablespaceInfo ts = new TablespaceInfo();
				ts.setName(rs.getString("name"));
				ts.setLocation(rs.getString("location"));
				ts.setSizeBytes(rs.getLong("size_bytes"));
				tablespaces.add(ts);
			}
			stats.setTablespaces(tablespaces);
		} catch (SQLException e) {
			LOG.warnf("Failed to get tablespace stats for %s: %s", instanceName, e.getMessage());
		}

		// Database sizes
		String dbSizeSql = """
			SELECT
			    datname as name,
			    pg_database_size(datname) as size_bytes
			FROM pg_database
			WHERE datistemplate = false
			ORDER BY pg_database_size(datname) DESC
			""";

		try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(dbSizeSql)) {
			List<DatabaseSize> databases = new ArrayList<>();
			long totalSize = 0;
			while (rs.next()) {
				DatabaseSize db = new DatabaseSize();
				db.setName(rs.getString("name"));
				db.setSizeBytes(rs.getLong("size_bytes"));
				databases.add(db);
				totalSize += db.getSizeBytes();
			}
			stats.setDatabases(databases);
			stats.setTotalDatabaseSize(totalSize);
		} catch (SQLException e) {
			LOG.warnf("Failed to get database sizes for %s: %s", instanceName, e.getMessage());
		}

		// WAL stats
		String walSql = """
			SELECT
			    (SELECT setting FROM pg_settings WHERE name = 'data_directory') as data_directory,
			    (SELECT setting::bigint FROM pg_settings WHERE name = 'wal_segment_size') as wal_segment_size,
			    (SELECT count(*) FROM pg_ls_waldir()) as wal_file_count
			""";

		try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(walSql)) {
			if (rs.next()) {
				stats.setDataDirectory(rs.getString("data_directory"));
				long segmentSize = rs.getLong("wal_segment_size");
				long fileCount = rs.getLong("wal_file_count");
				stats.setWalSegmentSize(segmentSize);
				stats.setWalFileCount(fileCount);
				stats.setWalTotalSize(segmentSize * fileCount);
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get WAL stats for %s: %s", instanceName, e.getMessage());
		}

		// Temp file stats
		String tempSql = """
			SELECT
			    COALESCE(SUM(temp_files), 0) as temp_files,
			    COALESCE(SUM(temp_bytes), 0) as temp_bytes
			FROM pg_stat_database
			""";

		try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(tempSql)) {
			if (rs.next()) {
				stats.setTempFiles(rs.getLong("temp_files"));
				stats.setTempBytes(rs.getLong("temp_bytes"));
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get temp file stats for %s: %s", instanceName, e.getMessage());
		}

		return stats;
	}

	// ========== WAL & Checkpoint Monitoring ==========

	/**
	 * Retrieves comprehensive WAL and checkpoint statistics for dashboard display.
	 * <p>
	 * Collects metrics from multiple PostgreSQL system views, adapting queries based on
	 * the PostgreSQL version:
	 * <ul>
	 *   <li>{@code pg_stat_wal} (PG14+) - WAL generation metrics</li>
	 *   <li>{@code pg_stat_bgwriter} - Background writer and buffer statistics</li>
	 *   <li>{@code pg_stat_checkpointer} (PG17+) - Checkpoint-specific metrics</li>
	 *   <li>{@code pg_stat_archiver} - WAL archiving status</li>
	 * </ul>
	 *
	 * @param instanceName the database instance identifier
	 * @return aggregated WAL and checkpoint statistics
	 */
	public WalCheckpointOverview getWalCheckpointOverview(String instanceName) {
		WalCheckpointOverview overview = new WalCheckpointOverview();

		// Get PostgreSQL version first
		int pgVersionNum = 0;
		String pgVersion = "";
		String versionSql = "SELECT current_setting('server_version_num')::int as version_num, version() as version_full";

		try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
		     Statement stmt = conn.createStatement();
		     ResultSet rs = stmt.executeQuery(versionSql)) {
			if (rs.next()) {
				pgVersionNum = rs.getInt("version_num");
				pgVersion = rs.getString("version_full");
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get PostgreSQL version for %s: %s", instanceName, e.getMessage());
		}

		overview.setPgVersionNum(pgVersionNum);
		overview.setPgVersion(pgVersion);

		// Get WAL stats (PG14+)
		if (pgVersionNum >= 140000) {
			overview.setWalStats(getWalStats(instanceName, pgVersionNum));
		}

		// Get background writer stats (always available)
		overview.setBgWriterStats(getBgWriterStats(instanceName, pgVersionNum));

		// Get checkpoint stats (PG17+ has separate view)
		if (pgVersionNum >= 170000) {
			overview.setCheckpointStats(getCheckpointStats(instanceName));
		}

		// Get archiver stats (always available)
		overview.setArchiverStats(getArchiverStats(instanceName));

		return overview;
	}

	/**
	 * Retrieves WAL generation statistics from pg_stat_wal (PostgreSQL 14+).
	 *
	 * @param instanceName the database instance identifier
	 * @param pgVersionNum PostgreSQL version number
	 * @return WAL statistics or null if not available
	 */
	private WalStats getWalStats(String instanceName, int pgVersionNum) {
		WalStats stats = new WalStats();
		stats.setPgVersionNum(pgVersionNum);

		String sql = """
			SELECT
			    wal_records,
			    wal_fpi,
			    wal_bytes,
			    wal_buffers_full,
			    wal_write,
			    wal_sync,
			    wal_write_time,
			    wal_sync_time,
			    stats_reset
			FROM pg_stat_wal
			""";

		try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
		     Statement stmt = conn.createStatement();
		     ResultSet rs = stmt.executeQuery(sql)) {
			if (rs.next()) {
				stats.setWalRecords(rs.getLong("wal_records"));
				stats.setWalFpi(rs.getLong("wal_fpi"));
				stats.setWalBytes(rs.getLong("wal_bytes"));
				stats.setWalBuffersFull(rs.getLong("wal_buffers_full"));
				stats.setWalWrite(rs.getLong("wal_write"));
				stats.setWalSync(rs.getLong("wal_sync"));
				stats.setWalWriteTime(rs.getDouble("wal_write_time"));
				stats.setWalSyncTime(rs.getDouble("wal_sync_time"));
				Timestamp resetTs = rs.getTimestamp("stats_reset");
				if (resetTs != null) {
					stats.setStatsReset(resetTs.toInstant());
				}
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get WAL stats for %s: %s", instanceName, e.getMessage());
			return null;
		}

		return stats;
	}

	/**
	 * Retrieves background writer statistics from pg_stat_bgwriter.
	 * <p>
	 * In PostgreSQL 17+, checkpoint stats have moved to pg_stat_checkpointer,
	 * so this method adapts accordingly.
	 *
	 * @param instanceName the database instance identifier
	 * @param pgVersionNum PostgreSQL version number
	 * @return background writer statistics
	 */
	private BgWriterStats getBgWriterStats(String instanceName, int pgVersionNum) {
		BgWriterStats stats = new BgWriterStats();

		String sql;
		if (pgVersionNum >= 170000) {
			// PG17+ - checkpoint stats moved to pg_stat_checkpointer
			sql = """
				SELECT
				    0 as checkpoints_timed,
				    0 as checkpoints_req,
				    0 as checkpoint_write_time,
				    0 as checkpoint_sync_time,
				    0 as buffers_checkpoint,
				    buffers_clean,
				    maxwritten_clean,
				    buffers_alloc,
				    stats_reset
				FROM pg_stat_bgwriter
				""";
		} else {
			// PG12-16 - all stats in pg_stat_bgwriter
			sql = """
				SELECT
				    checkpoints_timed,
				    checkpoints_req,
				    checkpoint_write_time,
				    checkpoint_sync_time,
				    buffers_checkpoint,
				    buffers_clean,
				    maxwritten_clean,
				    buffers_alloc,
				    stats_reset
				FROM pg_stat_bgwriter
				""";
		}

		try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
		     Statement stmt = conn.createStatement();
		     ResultSet rs = stmt.executeQuery(sql)) {
			if (rs.next()) {
				stats.setCheckpointsTimed(rs.getLong("checkpoints_timed"));
				stats.setCheckpointsReq(rs.getLong("checkpoints_req"));
				stats.setCheckpointWriteTime(rs.getDouble("checkpoint_write_time"));
				stats.setCheckpointSyncTime(rs.getDouble("checkpoint_sync_time"));
				stats.setBuffersCheckpoint(rs.getLong("buffers_checkpoint"));
				stats.setBuffersClean(rs.getLong("buffers_clean"));
				stats.setMaxwrittenClean(rs.getLong("maxwritten_clean"));
				stats.setBuffersAlloc(rs.getLong("buffers_alloc"));
				Timestamp resetTs = rs.getTimestamp("stats_reset");
				if (resetTs != null) {
					stats.setStatsReset(resetTs.toInstant());
				}
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get bgwriter stats for %s: %s", instanceName, e.getMessage());
		}

		// Get backend buffer writes from pg_stat_io for PG16+
		if (pgVersionNum >= 160000) {
			String ioSql = """
				SELECT
				    COALESCE(SUM(writes), 0) as buffers_backend,
				    COALESCE(SUM(fsyncs), 0) as buffers_backend_fsync
				FROM pg_stat_io
				WHERE backend_type = 'client backend'
				  AND context = 'normal'
				""";

			try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
			     Statement stmt = conn.createStatement();
			     ResultSet rs = stmt.executeQuery(ioSql)) {
				if (rs.next()) {
					stats.setBuffersBackend(rs.getLong("buffers_backend"));
					stats.setBuffersFsyncBackend(rs.getLong("buffers_backend_fsync"));
				}
			} catch (SQLException e) {
				LOG.debugf("Failed to get IO stats for %s: %s", instanceName, e.getMessage());
			}
		} else {
			// Pre-PG16 - get from pg_stat_bgwriter
			String bgSql = """
				SELECT buffers_backend, buffers_backend_fsync
				FROM pg_stat_bgwriter
				""";

			try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
			     Statement stmt = conn.createStatement();
			     ResultSet rs = stmt.executeQuery(bgSql)) {
				if (rs.next()) {
					stats.setBuffersBackend(rs.getLong("buffers_backend"));
					stats.setBuffersFsyncBackend(rs.getLong("buffers_backend_fsync"));
				}
			} catch (SQLException e) {
				LOG.debugf("Failed to get backend buffer stats for %s: %s", instanceName, e.getMessage());
			}
		}

		return stats;
	}

	/**
	 * Retrieves checkpoint statistics from pg_stat_checkpointer (PostgreSQL 17+).
	 *
	 * @param instanceName the database instance identifier
	 * @return checkpoint statistics
	 */
	private CheckpointStats getCheckpointStats(String instanceName) {
		CheckpointStats stats = new CheckpointStats();

		String sql = """
			SELECT
			    num_timed,
			    num_requested,
			    restartpoints_timed,
			    restartpoints_req,
			    restartpoints_done,
			    write_time,
			    sync_time,
			    buffers_written,
			    stats_reset
			FROM pg_stat_checkpointer
			""";

		try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
		     Statement stmt = conn.createStatement();
		     ResultSet rs = stmt.executeQuery(sql)) {
			if (rs.next()) {
				stats.setNumTimed(rs.getLong("num_timed"));
				stats.setNumRequested(rs.getLong("num_requested"));
				stats.setRestartpointsTimed(rs.getLong("restartpoints_timed"));
				stats.setRestartpointsReq(rs.getLong("restartpoints_req"));
				stats.setRestartpointsDone(rs.getLong("restartpoints_done"));
				stats.setWriteTime(rs.getDouble("write_time"));
				stats.setSyncTime(rs.getDouble("sync_time"));
				stats.setBuffersWritten(rs.getLong("buffers_written"));
				Timestamp resetTs = rs.getTimestamp("stats_reset");
				if (resetTs != null) {
					stats.setStatsReset(resetTs.toInstant());
				}
				stats.setAvailable(true);
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get checkpointer stats for %s: %s", instanceName, e.getMessage());
		}

		return stats;
	}

	/**
	 * Retrieves WAL archiver statistics from pg_stat_archiver.
	 *
	 * @param instanceName the database instance identifier
	 * @return archiver statistics including configuration
	 */
	private ArchiverStats getArchiverStats(String instanceName) {
		ArchiverStats stats = new ArchiverStats();

		// Get archiver stats
		String sql = """
			SELECT
			    archived_count,
			    last_archived_wal,
			    last_archived_time,
			    failed_count,
			    last_failed_wal,
			    last_failed_time,
			    stats_reset
			FROM pg_stat_archiver
			""";

		try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
		     Statement stmt = conn.createStatement();
		     ResultSet rs = stmt.executeQuery(sql)) {
			if (rs.next()) {
				stats.setArchivedCount(rs.getLong("archived_count"));
				stats.setLastArchivedWal(rs.getString("last_archived_wal"));
				Timestamp archivedTs = rs.getTimestamp("last_archived_time");
				if (archivedTs != null) {
					stats.setLastArchivedTime(archivedTs.toInstant());
				}
				stats.setFailedCount(rs.getLong("failed_count"));
				stats.setLastFailedWal(rs.getString("last_failed_wal"));
				Timestamp failedTs = rs.getTimestamp("last_failed_time");
				if (failedTs != null) {
					stats.setLastFailedTime(failedTs.toInstant());
				}
				Timestamp resetTs = rs.getTimestamp("stats_reset");
				if (resetTs != null) {
					stats.setStatsReset(resetTs.toInstant());
				}
			}
		} catch (SQLException e) {
			LOG.warnf("Failed to get archiver stats for %s: %s", instanceName, e.getMessage());
		}

		// Get archive configuration
		String configSql = """
			SELECT
			    (SELECT setting FROM pg_settings WHERE name = 'archive_mode') as archive_mode,
			    (SELECT setting FROM pg_settings WHERE name = 'archive_command') as archive_command
			""";

		try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
		     Statement stmt = conn.createStatement();
		     ResultSet rs = stmt.executeQuery(configSql)) {
			if (rs.next()) {
				stats.setArchiveMode(rs.getString("archive_mode"));
				stats.setArchiveCommand(rs.getString("archive_command"));
			}
		} catch (SQLException e) {
			LOG.debugf("Failed to get archive config for %s: %s", instanceName, e.getMessage());
		}

		return stats;
	}

	// ========== Inner Classes ==========

	/**
	 * Encapsulates statistics about PostgreSQL background processes.
	 * <p>
	 * Includes autovacuum configuration and activity, checkpoint statistics,
	 * background writer metrics, and buffer allocation patterns.
	 */
	public static class BackgroundProcessStats {

		private boolean autovacuumEnabled;
		private boolean autovacuumLauncherRunning;
		private int autovacuumWorkersRunning;
		private int maxAutovacuumWorkers;

		private long checkpointsTimed;
		private long checkpointsReq;
		private double checkpointWriteTime;
		private double checkpointSyncTime;
		private long buffersCheckpoint;
		private long buffersClean;
		private long maxwrittenClean;
		private long buffersBackend;
		private long buffersBackendFsync;
		private long buffersAlloc;

		private List<BackgroundProcess> backgroundProcesses = new ArrayList<>();

		// Getters and setters
		public boolean isAutovacuumEnabled() {
			return autovacuumEnabled;
		}

		public void setAutovacuumEnabled(boolean enabled) {
			this.autovacuumEnabled = enabled;
		}

		public boolean isAutovacuumLauncherRunning() {
			return autovacuumLauncherRunning;
		}

		public void setAutovacuumLauncherRunning(boolean running) {
			this.autovacuumLauncherRunning = running;
		}

		public int getAutovacuumWorkersRunning() {
			return autovacuumWorkersRunning;
		}

		public void setAutovacuumWorkersRunning(int workers) {
			this.autovacuumWorkersRunning = workers;
		}

		public int getMaxAutovacuumWorkers() {
			return maxAutovacuumWorkers;
		}

		public void setMaxAutovacuumWorkers(int max) {
			this.maxAutovacuumWorkers = max;
		}

		public long getCheckpointsTimed() {
			return checkpointsTimed;
		}

		public void setCheckpointsTimed(long val) {
			this.checkpointsTimed = val;
		}

		public long getCheckpointsReq() {
			return checkpointsReq;
		}

		public void setCheckpointsReq(long val) {
			this.checkpointsReq = val;
		}

		public double getCheckpointWriteTime() {
			return checkpointWriteTime;
		}

		public void setCheckpointWriteTime(double val) {
			this.checkpointWriteTime = val;
		}

		public double getCheckpointSyncTime() {
			return checkpointSyncTime;
		}

		public void setCheckpointSyncTime(double val) {
			this.checkpointSyncTime = val;
		}

		public long getBuffersCheckpoint() {
			return buffersCheckpoint;
		}

		public void setBuffersCheckpoint(long val) {
			this.buffersCheckpoint = val;
		}

		public long getBuffersClean() {
			return buffersClean;
		}

		public void setBuffersClean(long val) {
			this.buffersClean = val;
		}

		public long getMaxwrittenClean() {
			return maxwrittenClean;
		}

		public void setMaxwrittenClean(long val) {
			this.maxwrittenClean = val;
		}

		public long getBuffersBackend() {
			return buffersBackend;
		}

		public void setBuffersBackend(long val) {
			this.buffersBackend = val;
		}

		public long getBuffersBackendFsync() {
			return buffersBackendFsync;
		}

		public void setBuffersBackendFsync(long val) {
			this.buffersBackendFsync = val;
		}

		public long getBuffersAlloc() {
			return buffersAlloc;
		}

		public void setBuffersAlloc(long val) {
			this.buffersAlloc = val;
		}

		public List<BackgroundProcess> getBackgroundProcesses() {
			return backgroundProcesses;
		}

		public void setBackgroundProcesses(List<BackgroundProcess> processes) {
			this.backgroundProcesses = processes;
		}

		public String getCheckpointWriteTimeFormatted() {
			return formatTime(checkpointWriteTime);
		}

		public String getCheckpointSyncTimeFormatted() {
			return formatTime(checkpointSyncTime);
		}

		public long getTotalCheckpoints() {
			return checkpointsTimed + checkpointsReq;
		}

		public double getRequestedCheckpointRatio() {
			long total = getTotalCheckpoints();
			if (total == 0) return 0;
			return (checkpointsReq * 100.0) / total;
		}

		public String getRequestedCheckpointRatioFormatted() {
			return String.format("%.1f", getRequestedCheckpointRatio());
		}

		private String formatTime(double ms) {
			if (ms < 1000) return String.format("%.0f ms", ms);
			if (ms < 60000) return String.format("%.1f s", ms / 1000);
			if (ms < 3600000) return String.format("%.1f min", ms / 60000);
			return String.format("%.1f hours", ms / 3600000);
		}
	}

	/**
	 * Represents a single type of background process with its count.
	 * <p>
	 * Background process types include autovacuum launcher, autovacuum worker,
	 * background writer, checkpointer, WAL writer, and client backends.
	 */
	public static class BackgroundProcess {

		private String type;
		private int count;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public String getTypeCssClass() {
			return switch (type) {
				case "autovacuum launcher" -> "bg-info";
				case "autovacuum worker" -> "bg-warning text-dark";
				case "background writer" -> "bg-secondary";
				case "checkpointer" -> "bg-primary";
				case "walwriter" -> "bg-success";
				case "client backend" -> "bg-light text-dark";
				default -> "bg-secondary";
			};
		}
	}

	/**
	 * Encapsulates comprehensive storage statistics for a PostgreSQL instance.
	 * <p>
	 * Includes tablespace information, database sizes, WAL metrics, and temporary
	 * file usage statistics.
	 */
	public static class StorageStats {

		private String dataDirectory;
		private List<TablespaceInfo> tablespaces = new ArrayList<>();
		private List<DatabaseSize> databases = new ArrayList<>();
		private long totalDatabaseSize;
		private long walSegmentSize;
		private long walFileCount;
		private long walTotalSize;
		private long tempFiles;
		private long tempBytes;

		public String getDataDirectory() {
			return dataDirectory;
		}

		public void setDataDirectory(String dir) {
			this.dataDirectory = dir;
		}

		public List<TablespaceInfo> getTablespaces() {
			return tablespaces;
		}

		public void setTablespaces(List<TablespaceInfo> ts) {
			this.tablespaces = ts;
		}

		public List<DatabaseSize> getDatabases() {
			return databases;
		}

		public void setDatabases(List<DatabaseSize> dbs) {
			this.databases = dbs;
		}

		public long getTotalDatabaseSize() {
			return totalDatabaseSize;
		}

		public void setTotalDatabaseSize(long size) {
			this.totalDatabaseSize = size;
		}

		public String getTotalDatabaseSizeFormatted() {
			return formatBytes(totalDatabaseSize);
		}

		public long getWalSegmentSize() {
			return walSegmentSize;
		}

		public void setWalSegmentSize(long size) {
			this.walSegmentSize = size;
		}

		public long getWalFileCount() {
			return walFileCount;
		}

		public void setWalFileCount(long count) {
			this.walFileCount = count;
		}

		public long getWalTotalSize() {
			return walTotalSize;
		}

		public void setWalTotalSize(long size) {
			this.walTotalSize = size;
		}

		public String getWalTotalSizeFormatted() {
			return formatBytes(walTotalSize);
		}

		public long getTempFiles() {
			return tempFiles;
		}

		public void setTempFiles(long files) {
			this.tempFiles = files;
		}

		public long getTempBytes() {
			return tempBytes;
		}

		public void setTempBytes(long bytes) {
			this.tempBytes = bytes;
		}

		public String getTempBytesFormatted() {
			return formatBytes(tempBytes);
		}

		private String formatBytes(long bytes) {
			if (bytes < 1024) return bytes + " B";
			if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
			if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
			if (bytes < 1024L * 1024 * 1024 * 1024) return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
			return String.format("%.1f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
		}
	}

	/**
	 * Represents a PostgreSQL tablespace with its location and size.
	 * <p>
	 * Tablespaces allow database administrators to define filesystem locations
	 * where database objects can be stored.
	 */
	public static class TablespaceInfo {

		private String name;
		private String location;
		private long sizeBytes;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getLocation() {
			return location;
		}

		public void setLocation(String location) {
			this.location = location;
		}

		public long getSizeBytes() {
			return sizeBytes;
		}

		public void setSizeBytes(long bytes) {
			this.sizeBytes = bytes;
		}

		public String getSizeFormatted() {
			return formatBytes(sizeBytes);
		}

		private String formatBytes(long bytes) {
			if (bytes < 1024) return bytes + " B";
			if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
			if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
			if (bytes < 1024L * 1024 * 1024 * 1024) return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
			return String.format("%.1f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
		}
	}

	/**
	 * Represents a database with its storage size.
	 * <p>
	 * Excludes template databases from size calculations.
	 */
	public static class DatabaseSize {

		private String name;
		private long sizeBytes;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public long getSizeBytes() {
			return sizeBytes;
		}

		public void setSizeBytes(long bytes) {
			this.sizeBytes = bytes;
		}

		public String getSizeFormatted() {
			return formatBytes(sizeBytes);
		}

		private String formatBytes(long bytes) {
			if (bytes < 1024) return bytes + " B";
			if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
			if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
			if (bytes < 1024L * 1024 * 1024 * 1024) return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
			return String.format("%.1f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
		}
	}
}
