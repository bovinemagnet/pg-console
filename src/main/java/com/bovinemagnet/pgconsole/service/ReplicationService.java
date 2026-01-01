package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.ReplicationSlot;
import com.bovinemagnet.pgconsole.model.ReplicationStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for monitoring PostgreSQL replication status.
 * Queries pg_stat_replication, pg_replication_slots, and related views.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class ReplicationService {

    private static final Logger LOG = Logger.getLogger(ReplicationService.class);

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Retrieves streaming replication status for all connected replicas.
     * <p>
     * Queries the {@code pg_stat_replication} system view to obtain detailed information
     * about each streaming replication connection, including LSN positions, lag metrics,
     * and synchronisation state.
     *
     * @param instanceName the database instance identifier
     * @return list of replication status objects for all connected replicas, ordered by application name.
     *         Returns an empty list if no replicas are connected or if an error occurs.
     * @see ReplicationStatus
     */
    public List<ReplicationStatus> getStreamingReplication(String instanceName) {
        List<ReplicationStatus> replicas = new ArrayList<>();

        String sql = """
            SELECT
                pid,
                usename,
                application_name,
                client_addr::text,
                client_hostname,
                client_port,
                backend_start,
                state,
                sent_lsn::text,
                write_lsn::text,
                flush_lsn::text,
                replay_lsn::text,
                EXTRACT(EPOCH FROM write_lag) * 1000 as write_lag_ms,
                EXTRACT(EPOCH FROM flush_lag) * 1000 as flush_lag_ms,
                EXTRACT(EPOCH FROM replay_lag) * 1000 as replay_lag_ms,
                sync_priority::text,
                sync_state,
                reply_time,
                pg_wal_lsn_diff(sent_lsn, replay_lsn) as lag_bytes
            FROM pg_stat_replication
            ORDER BY application_name
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ReplicationStatus status = new ReplicationStatus();
                status.setPid(rs.getInt("pid"));
                status.setUseName(rs.getString("usename"));
                status.setApplicationName(rs.getString("application_name"));
                status.setClientAddr(rs.getString("client_addr"));
                status.setClientHostname(rs.getString("client_hostname"));
                status.setClientPort(rs.getInt("client_port"));

                Timestamp backendStart = rs.getTimestamp("backend_start");
                if (backendStart != null) {
                    status.setBackendStart(backendStart.toInstant());
                }

                status.setStateFromString(rs.getString("state"));
                status.setSentLsn(rs.getString("sent_lsn"));
                status.setWriteLsn(rs.getString("write_lsn"));
                status.setFlushLsn(rs.getString("flush_lsn"));
                status.setReplayLsn(rs.getString("replay_lsn"));
                status.setWriteLag((long) rs.getDouble("write_lag_ms"));
                status.setFlushLag((long) rs.getDouble("flush_lag_ms"));
                status.setReplayLag((long) rs.getDouble("replay_lag_ms"));
                status.setSyncPriority(rs.getString("sync_priority"));
                status.setSyncState(rs.getString("sync_state"));

                Timestamp replyTime = rs.getTimestamp("reply_time");
                if (replyTime != null) {
                    status.setReplyTime(replyTime.toInstant());
                }

                status.setLagBytes(rs.getLong("lag_bytes"));

                replicas.add(status);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get streaming replication for %s: %s", instanceName, e.getMessage());
        }

        return replicas;
    }

    /**
     * Retrieves all replication slots configured on the database instance.
     * <p>
     * Queries the {@code pg_replication_slots} system view to obtain information about
     * both physical and logical replication slots, including their current state,
     * WAL retention, and whether they are safe to delete.
     *
     * @param instanceName the database instance identifier
     * @return list of replication slot objects, ordered by slot type and name.
     *         Returns an empty list if no slots exist or if an error occurs.
     * @see ReplicationSlot
     */
    public List<ReplicationSlot> getReplicationSlots(String instanceName) {
        List<ReplicationSlot> slots = new ArrayList<>();

        String sql = """
            SELECT
                slot_name,
                plugin,
                slot_type,
                database,
                temporary,
                active,
                active_pid,
                xmin::text,
                catalog_xmin::text,
                restart_lsn::text,
                confirmed_flush_lsn::text,
                pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) as wal_retained_bytes,
                NOT active AND restart_lsn IS NOT NULL as safe_to_delete
            FROM pg_replication_slots
            ORDER BY slot_type, slot_name
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ReplicationSlot slot = new ReplicationSlot();
                slot.setSlotName(rs.getString("slot_name"));
                slot.setPlugin(rs.getString("plugin"));
                slot.setSlotTypeFromString(rs.getString("slot_type"));
                slot.setDatabase(rs.getString("database"));
                slot.setTemporary(rs.getBoolean("temporary"));
                slot.setActive(rs.getBoolean("active"));
                slot.setActivePid(rs.getInt("active_pid"));
                slot.setXmin(rs.getString("xmin"));
                slot.setCatalogXmin(rs.getString("catalog_xmin"));
                slot.setRestartLsn(rs.getString("restart_lsn"));
                slot.setConfirmedFlushLsn(rs.getString("confirmed_flush_lsn"));
                slot.setWalRetainedBytes(rs.getLong("wal_retained_bytes"));
                slot.setSafeToDelete(rs.getBoolean("safe_to_delete"));

                slots.add(slot);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get replication slots for %s: %s", instanceName, e.getMessage());
        }

        return slots;
    }

    /**
     * Retrieves Write-Ahead Log (WAL) statistics and configuration for the database instance.
     * <p>
     * Collects comprehensive WAL metrics including current LSN position, WAL configuration
     * parameters (wal_level, max_wal_senders, max_replication_slots), usage statistics,
     * and WAL retention information based on active replication slots.
     *
     * @param instanceName the database instance identifier
     * @return WAL statistics object containing current state and configuration.
     *         Individual fields may be zero or null if retrieval fails.
     * @see WalStats
     */
    public WalStats getWalStats(String instanceName) {
        WalStats stats = new WalStats();

        String sql = """
            SELECT
                pg_current_wal_lsn()::text as current_lsn,
                pg_walfile_name(pg_current_wal_lsn()) as current_wal_file,
                (SELECT setting FROM pg_settings WHERE name = 'wal_level') as wal_level,
                (SELECT setting FROM pg_settings WHERE name = 'max_wal_senders') as max_wal_senders,
                (SELECT count(*) FROM pg_stat_replication) as active_senders,
                (SELECT setting FROM pg_settings WHERE name = 'max_replication_slots') as max_replication_slots,
                (SELECT count(*) FROM pg_replication_slots) as used_slots,
                (SELECT setting::bigint FROM pg_settings WHERE name = 'wal_segment_size') as wal_segment_size
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                stats.setCurrentLsn(rs.getString("current_lsn"));
                stats.setCurrentWalFile(rs.getString("current_wal_file"));
                stats.setWalLevel(rs.getString("wal_level"));
                stats.setMaxWalSenders(rs.getInt("max_wal_senders"));
                stats.setActiveWalSenders(rs.getInt("active_senders"));
                stats.setMaxReplicationSlots(rs.getInt("max_replication_slots"));
                stats.setUsedReplicationSlots(rs.getInt("used_slots"));
                stats.setWalSegmentSize(rs.getLong("wal_segment_size"));
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get WAL stats for %s: %s", instanceName, e.getMessage());
        }

        // Get WAL directory size
        String walSizeSql = """
            SELECT pg_wal_lsn_diff(pg_current_wal_lsn(),
                   (SELECT restart_lsn FROM pg_replication_slots
                    WHERE restart_lsn IS NOT NULL
                    ORDER BY restart_lsn LIMIT 1)) as wal_retained
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(walSizeSql)) {

            if (rs.next()) {
                stats.setWalRetainedBySlots(rs.getLong("wal_retained"));
            }
        } catch (SQLException e) {
            // May fail if no slots exist
            LOG.debugf("Could not get WAL retained size: %s", e.getMessage());
        }

        return stats;
    }

    /**
     * Generates a comprehensive replication summary for dashboard display.
     * <p>
     * Aggregates data from streaming replication status, replication slots, and WAL statistics
     * to provide an overview of the replication health, including replica counts, lag metrics,
     * slot usage, and WAL retention.
     *
     * @param instanceName the database instance identifier
     * @return summary object containing aggregated replication metrics
     * @see ReplicationSummary
     * @see #getStreamingReplication(String)
     * @see #getReplicationSlots(String)
     * @see #getWalStats(String)
     */
    public ReplicationSummary getSummary(String instanceName) {
        ReplicationSummary summary = new ReplicationSummary();

        List<ReplicationStatus> replicas = getStreamingReplication(instanceName);
        List<ReplicationSlot> slots = getReplicationSlots(instanceName);
        WalStats walStats = getWalStats(instanceName);

        summary.setTotalReplicas(replicas.size());
        summary.setStreamingReplicas((int) replicas.stream()
            .filter(r -> r.getState() == ReplicationStatus.ReplicationState.STREAMING)
            .count());
        summary.setLaggingReplicas((int) replicas.stream()
            .filter(ReplicationStatus::isHasLag)
            .count());

        summary.setTotalSlots(slots.size());
        summary.setActiveSlots((int) slots.stream().filter(ReplicationSlot::isActive).count());
        summary.setInactiveSlots((int) slots.stream().filter(s -> !s.isActive()).count());

        // Calculate max lag
        long maxLagBytes = replicas.stream()
            .mapToLong(ReplicationStatus::getLagBytes)
            .max()
            .orElse(0);
        summary.setMaxLagBytes(maxLagBytes);

        // Calculate total WAL retained by slots
        long totalRetained = slots.stream()
            .mapToLong(ReplicationSlot::getWalRetainedBytes)
            .sum();
        summary.setTotalWalRetained(totalRetained);

        summary.setWalLevel(walStats.getWalLevel());
        summary.setIsPrimary(replicas.size() > 0 || "replica".equals(walStats.getWalLevel()) == false);

        return summary;
    }

    /**
     * Checks whether the database instance is currently in recovery mode (replica/standby).
     * <p>
     * Uses the PostgreSQL {@code pg_is_in_recovery()} function to determine if the instance
     * is operating as a standby server receiving WAL records from a primary.
     *
     * @param instanceName the database instance identifier
     * @return {@code true} if the instance is in recovery mode (standby), {@code false} if it is
     *         a primary server or if an error occurs
     */
    public boolean isReplica(String instanceName) {
        String sql = "SELECT pg_is_in_recovery()";

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getBoolean(1);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to check recovery status for %s: %s", instanceName, e.getMessage());
        }

        return false;
    }

    /**
     * Encapsulates Write-Ahead Log (WAL) statistics and configuration parameters.
     * <p>
     * Provides both current operational metrics (LSN position, WAL file name) and
     * configuration settings (wal_level, sender limits, slot limits) for monitoring
     * WAL activity and capacity planning.
     */
    public static class WalStats {
        private String currentLsn;
        private String currentWalFile;
        private String walLevel;
        private int maxWalSenders;
        private int activeWalSenders;
        private int maxReplicationSlots;
        private int usedReplicationSlots;
        private long walSegmentSize;
        private long walRetainedBySlots;

        public String getCurrentLsn() { return currentLsn; }
        public void setCurrentLsn(String currentLsn) { this.currentLsn = currentLsn; }

        public String getCurrentWalFile() { return currentWalFile; }
        public void setCurrentWalFile(String currentWalFile) { this.currentWalFile = currentWalFile; }

        public String getWalLevel() { return walLevel; }
        public void setWalLevel(String walLevel) { this.walLevel = walLevel; }

        public int getMaxWalSenders() { return maxWalSenders; }
        public void setMaxWalSenders(int maxWalSenders) { this.maxWalSenders = maxWalSenders; }

        public int getActiveWalSenders() { return activeWalSenders; }
        public void setActiveWalSenders(int activeWalSenders) { this.activeWalSenders = activeWalSenders; }

        public int getMaxReplicationSlots() { return maxReplicationSlots; }
        public void setMaxReplicationSlots(int maxReplicationSlots) { this.maxReplicationSlots = maxReplicationSlots; }

        public int getUsedReplicationSlots() { return usedReplicationSlots; }
        public void setUsedReplicationSlots(int usedReplicationSlots) { this.usedReplicationSlots = usedReplicationSlots; }

        public long getWalSegmentSize() { return walSegmentSize; }
        public void setWalSegmentSize(long walSegmentSize) { this.walSegmentSize = walSegmentSize; }

        public long getWalRetainedBySlots() { return walRetainedBySlots; }
        public void setWalRetainedBySlots(long walRetainedBySlots) { this.walRetainedBySlots = walRetainedBySlots; }

        public String getWalSegmentSizeFormatted() {
            return formatBytes(walSegmentSize);
        }

        public String getWalRetainedFormatted() {
            return formatBytes(walRetainedBySlots);
        }

        private String formatBytes(long bytes) {
            if (bytes < 0) return "N/A";
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Aggregated replication summary for dashboard display.
     * <p>
     * Consolidates key replication metrics including replica counts by state,
     * replication slot usage, maximum lag measurements, and WAL retention totals
     * to provide a quick overview of replication health.
     */
    public static class ReplicationSummary {
        private int totalReplicas;
        private int streamingReplicas;
        private int laggingReplicas;
        private int totalSlots;
        private int activeSlots;
        private int inactiveSlots;
        private long maxLagBytes;
        private long totalWalRetained;
        private String walLevel;
        private boolean isPrimary;

        public int getTotalReplicas() { return totalReplicas; }
        public void setTotalReplicas(int totalReplicas) { this.totalReplicas = totalReplicas; }

        public int getStreamingReplicas() { return streamingReplicas; }
        public void setStreamingReplicas(int streamingReplicas) { this.streamingReplicas = streamingReplicas; }

        public int getLaggingReplicas() { return laggingReplicas; }
        public void setLaggingReplicas(int laggingReplicas) { this.laggingReplicas = laggingReplicas; }

        public int getTotalSlots() { return totalSlots; }
        public void setTotalSlots(int totalSlots) { this.totalSlots = totalSlots; }

        public int getActiveSlots() { return activeSlots; }
        public void setActiveSlots(int activeSlots) { this.activeSlots = activeSlots; }

        public int getInactiveSlots() { return inactiveSlots; }
        public void setInactiveSlots(int inactiveSlots) { this.inactiveSlots = inactiveSlots; }

        public long getMaxLagBytes() { return maxLagBytes; }
        public void setMaxLagBytes(long maxLagBytes) { this.maxLagBytes = maxLagBytes; }

        public String getMaxLagFormatted() {
            return formatBytes(maxLagBytes);
        }

        public long getTotalWalRetained() { return totalWalRetained; }
        public void setTotalWalRetained(long totalWalRetained) { this.totalWalRetained = totalWalRetained; }

        public String getTotalWalRetainedFormatted() {
            return formatBytes(totalWalRetained);
        }

        public String getWalLevel() { return walLevel; }
        public void setWalLevel(String walLevel) { this.walLevel = walLevel; }

        public boolean isPrimary() { return isPrimary; }
        public void setIsPrimary(boolean isPrimary) { this.isPrimary = isPrimary; }

        public boolean hasReplicas() { return totalReplicas > 0; }
        public boolean hasSlots() { return totalSlots > 0; }

        private String formatBytes(long bytes) {
            if (bytes < 0) return "N/A";
            if (bytes == 0) return "0 B";
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
