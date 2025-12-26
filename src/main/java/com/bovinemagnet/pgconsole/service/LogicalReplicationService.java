package com.bovinemagnet.pgconsole.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for monitoring logical replication: publications, subscriptions, and origins.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class LogicalReplicationService {

    private static final Logger LOG = Logger.getLogger(LogicalReplicationService.class);

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Gets all publications in the database.
     */
    public List<Publication> getPublications(String instanceName) {
        List<Publication> publications = new ArrayList<>();

        String sql = """
            SELECT
                p.oid,
                p.pubname,
                p.pubowner::regrole::text as owner,
                p.puballtables,
                p.pubinsert,
                p.pubupdate,
                p.pubdelete,
                p.pubtruncate,
                (SELECT count(*) FROM pg_publication_tables pt WHERE pt.pubname = p.pubname) as table_count
            FROM pg_publication p
            ORDER BY p.pubname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Publication pub = new Publication();
                pub.setOid(rs.getLong("oid"));
                pub.setName(rs.getString("pubname"));
                pub.setOwner(rs.getString("owner"));
                pub.setAllTables(rs.getBoolean("puballtables"));
                pub.setInsert(rs.getBoolean("pubinsert"));
                pub.setUpdate(rs.getBoolean("pubupdate"));
                pub.setDelete(rs.getBoolean("pubdelete"));
                pub.setTruncate(rs.getBoolean("pubtruncate"));
                pub.setTableCount(rs.getInt("table_count"));
                publications.add(pub);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get publications for %s: %s", instanceName, e.getMessage());
        }

        return publications;
    }

    /**
     * Gets tables in a publication.
     */
    public List<PublicationTable> getPublicationTables(String instanceName, String pubName) {
        List<PublicationTable> tables = new ArrayList<>();

        String sql = """
            SELECT
                schemaname,
                tablename,
                attnames,
                rowfilter
            FROM pg_publication_tables
            WHERE pubname = ?
            ORDER BY schemaname, tablename
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, pubName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PublicationTable table = new PublicationTable();
                    table.setSchemaName(rs.getString("schemaname"));
                    table.setTableName(rs.getString("tablename"));

                    var attnames = rs.getArray("attnames");
                    if (attnames != null) {
                        table.setColumns(List.of((String[]) attnames.getArray()));
                    }

                    table.setRowFilter(rs.getString("rowfilter"));
                    tables.add(table);
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get publication tables for %s.%s: %s", instanceName, pubName, e.getMessage());
        }

        return tables;
    }

    /**
     * Gets all subscriptions in the database.
     */
    public List<Subscription> getSubscriptions(String instanceName) {
        List<Subscription> subscriptions = new ArrayList<>();

        String sql = """
            SELECT
                s.oid,
                s.subname,
                s.subowner::regrole::text as owner,
                s.subenabled,
                s.subconninfo,
                s.subslotname,
                s.subsynccommit,
                s.subpublications,
                (SELECT count(*) FROM pg_subscription_rel sr WHERE sr.srsubid = s.oid) as table_count
            FROM pg_subscription s
            ORDER BY s.subname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Subscription sub = new Subscription();
                sub.setOid(rs.getLong("oid"));
                sub.setName(rs.getString("subname"));
                sub.setOwner(rs.getString("owner"));
                sub.setEnabled(rs.getBoolean("subenabled"));
                sub.setConnectionInfo(maskConnectionString(rs.getString("subconninfo")));
                sub.setSlotName(rs.getString("subslotname"));
                sub.setSyncCommit(rs.getString("subsynccommit"));

                var pubs = rs.getArray("subpublications");
                if (pubs != null) {
                    sub.setPublications(List.of((String[]) pubs.getArray()));
                }

                sub.setTableCount(rs.getInt("table_count"));
                subscriptions.add(sub);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get subscriptions for %s: %s", instanceName, e.getMessage());
        }

        return subscriptions;
    }

    /**
     * Gets subscription table states.
     */
    public List<SubscriptionTable> getSubscriptionTables(String instanceName, String subName) {
        List<SubscriptionTable> tables = new ArrayList<>();

        String sql = """
            SELECT
                sr.srsubid,
                sr.srrelid::regclass::text as table_name,
                sr.srsubstate,
                sr.srsublsn
            FROM pg_subscription_rel sr
            JOIN pg_subscription s ON s.oid = sr.srsubid
            WHERE s.subname = ?
            ORDER BY table_name
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, subName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SubscriptionTable table = new SubscriptionTable();
                    table.setTableName(rs.getString("table_name"));
                    table.setState(rs.getString("srsubstate"));
                    table.setLsn(rs.getString("srsublsn"));
                    tables.add(table);
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get subscription tables for %s.%s: %s", instanceName, subName, e.getMessage());
        }

        return tables;
    }

    /**
     * Gets replication origins.
     */
    public List<ReplicationOrigin> getReplicationOrigins(String instanceName) {
        List<ReplicationOrigin> origins = new ArrayList<>();

        String sql = """
            SELECT
                ro.roident,
                ro.roname,
                rop.remote_lsn,
                rop.local_lsn
            FROM pg_replication_origin ro
            LEFT JOIN pg_replication_origin_status rop ON ro.roident = rop.local_id
            ORDER BY ro.roname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ReplicationOrigin origin = new ReplicationOrigin();
                origin.setId(rs.getInt("roident"));
                origin.setName(rs.getString("roname"));
                origin.setRemoteLsn(rs.getString("remote_lsn"));
                origin.setLocalLsn(rs.getString("local_lsn"));
                origins.add(origin);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get replication origins for %s: %s", instanceName, e.getMessage());
        }

        return origins;
    }

    /**
     * Gets subscription statistics (PG 15+).
     */
    public List<SubscriptionStats> getSubscriptionStats(String instanceName) {
        List<SubscriptionStats> stats = new ArrayList<>();

        // Check if pg_stat_subscription_stats exists (PG 15+)
        String checkSql = """
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = 'pg_catalog'
            AND table_name = 'pg_stat_subscription_stats'
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement checkStmt = conn.createStatement();
             ResultSet checkRs = checkStmt.executeQuery(checkSql)) {

            if (!checkRs.next()) {
                // PG 14 or earlier - use pg_stat_subscription instead
                return getSubscriptionStatsLegacy(instanceName);
            }
        } catch (SQLException e) {
            return stats;
        }

        String sql = """
            SELECT
                s.subname,
                ss.apply_error_count,
                ss.sync_error_count,
                ss.stats_reset
            FROM pg_stat_subscription_stats ss
            JOIN pg_subscription s ON s.oid = ss.subid
            ORDER BY s.subname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                SubscriptionStats stat = new SubscriptionStats();
                stat.setName(rs.getString("subname"));
                stat.setApplyErrorCount(rs.getLong("apply_error_count"));
                stat.setSyncErrorCount(rs.getLong("sync_error_count"));
                Timestamp reset = rs.getTimestamp("stats_reset");
                if (reset != null) {
                    stat.setStatsReset(reset.toInstant());
                }
                stats.add(stat);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get subscription stats for %s: %s", instanceName, e.getMessage());
        }

        return stats;
    }

    private List<SubscriptionStats> getSubscriptionStatsLegacy(String instanceName) {
        List<SubscriptionStats> stats = new ArrayList<>();

        String sql = """
            SELECT
                s.subname,
                ss.received_lsn,
                ss.last_msg_send_time,
                ss.last_msg_receipt_time,
                ss.latest_end_lsn,
                ss.latest_end_time
            FROM pg_stat_subscription ss
            JOIN pg_subscription s ON s.oid = ss.subid
            ORDER BY s.subname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                SubscriptionStats stat = new SubscriptionStats();
                stat.setName(rs.getString("subname"));
                stat.setReceivedLsn(rs.getString("received_lsn"));
                stat.setLatestEndLsn(rs.getString("latest_end_lsn"));

                Timestamp lastMsg = rs.getTimestamp("last_msg_receipt_time");
                if (lastMsg != null) {
                    stat.setLastMessageTime(lastMsg.toInstant());
                }

                stats.add(stat);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get subscription stats (legacy) for %s: %s", instanceName, e.getMessage());
        }

        return stats;
    }

    /**
     * Gets summary statistics.
     */
    public LogicalReplicationSummary getSummary(String instanceName) {
        LogicalReplicationSummary summary = new LogicalReplicationSummary();

        String sql = """
            SELECT
                (SELECT count(*) FROM pg_publication) as publication_count,
                (SELECT count(*) FROM pg_subscription) as subscription_count,
                (SELECT count(*) FROM pg_subscription WHERE subenabled = true) as enabled_subscriptions,
                (SELECT count(*) FROM pg_replication_origin) as origin_count
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                summary.setPublicationCount(rs.getInt("publication_count"));
                summary.setSubscriptionCount(rs.getInt("subscription_count"));
                summary.setEnabledSubscriptions(rs.getInt("enabled_subscriptions"));
                summary.setOriginCount(rs.getInt("origin_count"));
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get logical replication summary for %s: %s", instanceName, e.getMessage());
        }

        return summary;
    }

    /**
     * Masks password in connection string.
     */
    private String maskConnectionString(String connInfo) {
        if (connInfo == null) return null;
        return connInfo.replaceAll("password=[^ ]+", "password=****");
    }

    // --- Model Classes ---

    public static class Publication {
        private long oid;
        private String name;
        private String owner;
        private boolean allTables;
        private boolean insert;
        private boolean update;
        private boolean delete;
        private boolean truncate;
        private int tableCount;

        public long getOid() { return oid; }
        public void setOid(long oid) { this.oid = oid; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }

        public boolean isAllTables() { return allTables; }
        public void setAllTables(boolean allTables) { this.allTables = allTables; }

        public boolean isInsert() { return insert; }
        public void setInsert(boolean insert) { this.insert = insert; }

        public boolean isUpdate() { return update; }
        public void setUpdate(boolean update) { this.update = update; }

        public boolean isDelete() { return delete; }
        public void setDelete(boolean delete) { this.delete = delete; }

        public boolean isTruncate() { return truncate; }
        public void setTruncate(boolean truncate) { this.truncate = truncate; }

        public int getTableCount() { return tableCount; }
        public void setTableCount(int tableCount) { this.tableCount = tableCount; }

        public String getActionsDisplay() {
            List<String> actions = new ArrayList<>();
            if (insert) actions.add("INSERT");
            if (update) actions.add("UPDATE");
            if (delete) actions.add("DELETE");
            if (truncate) actions.add("TRUNCATE");
            return String.join(", ", actions);
        }
    }

    public static class PublicationTable {
        private String schemaName;
        private String tableName;
        private List<String> columns;
        private String rowFilter;

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }

        public String getFullName() {
            return schemaName + "." + tableName;
        }

        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }

        public String getColumnsDisplay() {
            if (columns == null || columns.isEmpty()) return "ALL";
            return String.join(", ", columns);
        }

        public String getRowFilter() { return rowFilter; }
        public void setRowFilter(String rowFilter) { this.rowFilter = rowFilter; }

        public boolean hasRowFilter() { return rowFilter != null && !rowFilter.isEmpty(); }
    }

    public static class Subscription {
        private long oid;
        private String name;
        private String owner;
        private boolean enabled;
        private String connectionInfo;
        private String slotName;
        private String syncCommit;
        private List<String> publications;
        private int tableCount;

        public long getOid() { return oid; }
        public void setOid(long oid) { this.oid = oid; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getConnectionInfo() { return connectionInfo; }
        public void setConnectionInfo(String connectionInfo) { this.connectionInfo = connectionInfo; }

        public String getSlotName() { return slotName; }
        public void setSlotName(String slotName) { this.slotName = slotName; }

        public String getSyncCommit() { return syncCommit; }
        public void setSyncCommit(String syncCommit) { this.syncCommit = syncCommit; }

        public List<String> getPublications() { return publications; }
        public void setPublications(List<String> publications) { this.publications = publications; }

        public String getPublicationsDisplay() {
            if (publications == null) return "";
            return String.join(", ", publications);
        }

        public int getTableCount() { return tableCount; }
        public void setTableCount(int tableCount) { this.tableCount = tableCount; }

        public String getStatusCssClass() {
            return enabled ? "bg-success" : "bg-secondary";
        }
    }

    public static class SubscriptionTable {
        private String tableName;
        private String state;
        private String lsn;

        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public String getStateDisplay() {
            if (state == null) return "Unknown";
            return switch (state) {
                case "i" -> "Initialising";
                case "d" -> "Data copy";
                case "f" -> "Finished table copy";
                case "s" -> "Synchronised";
                case "r" -> "Ready";
                default -> state;
            };
        }

        public String getStateCssClass() {
            if (state == null) return "bg-secondary";
            return switch (state) {
                case "r", "s" -> "bg-success";
                case "i", "d", "f" -> "bg-warning text-dark";
                default -> "bg-secondary";
            };
        }

        public String getLsn() { return lsn; }
        public void setLsn(String lsn) { this.lsn = lsn; }
    }

    public static class ReplicationOrigin {
        private int id;
        private String name;
        private String remoteLsn;
        private String localLsn;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getRemoteLsn() { return remoteLsn; }
        public void setRemoteLsn(String remoteLsn) { this.remoteLsn = remoteLsn; }

        public String getLocalLsn() { return localLsn; }
        public void setLocalLsn(String localLsn) { this.localLsn = localLsn; }
    }

    public static class SubscriptionStats {
        private String name;
        private long applyErrorCount;
        private long syncErrorCount;
        private Instant statsReset;
        private String receivedLsn;
        private String latestEndLsn;
        private Instant lastMessageTime;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public long getApplyErrorCount() { return applyErrorCount; }
        public void setApplyErrorCount(long count) { this.applyErrorCount = count; }

        public long getSyncErrorCount() { return syncErrorCount; }
        public void setSyncErrorCount(long count) { this.syncErrorCount = count; }

        public Instant getStatsReset() { return statsReset; }
        public void setStatsReset(Instant time) { this.statsReset = time; }

        public String getReceivedLsn() { return receivedLsn; }
        public void setReceivedLsn(String lsn) { this.receivedLsn = lsn; }

        public String getLatestEndLsn() { return latestEndLsn; }
        public void setLatestEndLsn(String lsn) { this.latestEndLsn = lsn; }

        public Instant getLastMessageTime() { return lastMessageTime; }
        public void setLastMessageTime(Instant time) { this.lastMessageTime = time; }

        public boolean hasErrors() {
            return applyErrorCount > 0 || syncErrorCount > 0;
        }
    }

    public static class LogicalReplicationSummary {
        private int publicationCount;
        private int subscriptionCount;
        private int enabledSubscriptions;
        private int originCount;

        public int getPublicationCount() { return publicationCount; }
        public void setPublicationCount(int count) { this.publicationCount = count; }

        public int getSubscriptionCount() { return subscriptionCount; }
        public void setSubscriptionCount(int count) { this.subscriptionCount = count; }

        public int getEnabledSubscriptions() { return enabledSubscriptions; }
        public void setEnabledSubscriptions(int count) { this.enabledSubscriptions = count; }

        public int getOriginCount() { return originCount; }
        public void setOriginCount(int count) { this.originCount = count; }

        public boolean hasLogicalReplication() {
            return publicationCount > 0 || subscriptionCount > 0;
        }
    }
}
