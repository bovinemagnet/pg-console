package com.bovinemagnet.pgconsole.testutil;

import com.bovinemagnet.pgconsole.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * Factory class for creating test data objects.
 * <p>
 * Provides convenient methods to create model objects with sensible
 * defaults for use in unit and integration tests.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public final class TestDataFactory {

    private static final Random RANDOM = new Random();

    private TestDataFactory() {
        // Utility class - prevent instantiation
    }

    // =========================================================================
    // SlowQuery
    // =========================================================================

    /**
     * Creates a SlowQuery with default values.
     */
    public static SlowQuery createSlowQuery() {
        return createSlowQuery("12345", "SELECT * FROM users WHERE id = $1");
    }

    /**
     * Creates a SlowQuery with specified query ID and text.
     */
    public static SlowQuery createSlowQuery(String queryId, String queryText) {
        SlowQuery query = new SlowQuery();
        query.setQueryId(queryId);
        query.setQuery(queryText);
        query.setTotalCalls(100);
        query.setTotalTime(5000.0);
        query.setMeanTime(50.0);
        query.setMaxTime(150.0);
        query.setMinTime(10.0);
        query.setRows(5000);
        query.setSharedBlksHit(1000);
        query.setSharedBlksRead(100);
        query.setUser("test_user");
        query.setDatabase("testdb");
        return query;
    }

    /**
     * Creates a list of SlowQuery objects for testing.
     */
    public static List<SlowQuery> createSlowQueryList(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> createSlowQuery(String.valueOf(i + 1), "SELECT * FROM table_" + i + " WHERE id = $1"))
            .toList();
    }

    // =========================================================================
    // Activity
    // =========================================================================

    /**
     * Creates an Activity with default "active" state.
     */
    public static Activity createActivity() {
        return createActivity("active");
    }

    /**
     * Creates an Activity with specified state.
     */
    public static Activity createActivity(String state) {
        Activity activity = new Activity();
        activity.setPid(1234 + RANDOM.nextInt(10000));
        activity.setUser("test_user");
        activity.setDatabase("testdb");
        activity.setApplicationName("test_app");
        activity.setClientAddr("127.0.0.1");
        activity.setState(state);
        activity.setQuery("SELECT 1");
        activity.setQueryStart(LocalDateTime.now().minusSeconds(10));
        activity.setWaitEventType(null);
        activity.setWaitEvent(null);
        return activity;
    }

    /**
     * Creates an Activity that is blocked.
     */
    public static Activity createBlockedActivity(int blockerPid) {
        Activity activity = createActivity("active");
        activity.setWaitEventType("Lock");
        activity.setWaitEvent("transactionid");
        activity.setQuery("UPDATE users SET name = 'blocked' WHERE id = 1");
        activity.setBlockingPid(blockerPid);
        return activity;
    }

    // =========================================================================
    // OverviewStats
    // =========================================================================

    /**
     * Creates OverviewStats with default healthy values.
     */
    public static OverviewStats createOverviewStats() {
        return createOverviewStats(50, 100, 5, 0, 99.5);
    }

    /**
     * Creates OverviewStats with specified values.
     */
    public static OverviewStats createOverviewStats(
            int connectionsUsed,
            int connectionsMax,
            int activeQueries,
            int blockedQueries,
            double cacheHitRatio) {
        OverviewStats stats = new OverviewStats();
        stats.setConnectionsUsed(connectionsUsed);
        stats.setConnectionsMax(connectionsMax);
        stats.setActiveQueries(activeQueries);
        stats.setBlockedQueries(blockedQueries);
        stats.setCacheHitRatio(cacheHitRatio);
        stats.setDatabaseSize("1 GB");
        stats.setVersion("PostgreSQL 15.0");
        return stats;
    }

    /**
     * Creates OverviewStats indicating high load.
     */
    public static OverviewStats createHighLoadStats() {
        return createOverviewStats(95, 100, 20, 5, 85.0);
    }

    // =========================================================================
    // TableStats
    // =========================================================================

    /**
     * Creates TableStats with default values.
     */
    public static TableStats createTableStats(String tableName) {
        TableStats stats = new TableStats();
        stats.setSchemaName("public");
        stats.setTableName(tableName);
        stats.setSeqScan(100);
        stats.setSeqTupRead(10000);
        stats.setIdxScan(5000);
        stats.setIdxTupFetch(4500);
        stats.setnTupIns(1000);
        stats.setnTupUpd(500);
        stats.setnTupDel(100);
        stats.setnLiveTup(9950);
        stats.setnDeadTup(50);
        return stats;
    }

    // =========================================================================
    // LockInfo
    // =========================================================================

    /**
     * Creates a LockInfo with default values.
     */
    public static LockInfo createLockInfo(String lockType, String mode, boolean granted) {
        LockInfo lock = new LockInfo();
        lock.setLockType(lockType);
        lock.setMode(mode);
        lock.setGranted(granted);
        lock.setPid(1234);
        lock.setRelation("users");
        lock.setDatabase("testdb");
        lock.setUser("test_user");
        lock.setState("active");
        lock.setQuery("SELECT * FROM users FOR UPDATE");
        return lock;
    }

    // =========================================================================
    // DatabaseInfo
    // =========================================================================

    /**
     * Creates a DatabaseInfo with default values.
     */
    public static DatabaseInfo createDatabaseInfo() {
        DatabaseInfo info = new DatabaseInfo();
        info.setPostgresVersion("PostgreSQL 15.0 on x86_64-pc-linux-gnu");
        info.setCurrentDatabase("testdb");
        info.setCurrentUser("test_user");
        info.setServerEncoding("UTF8");
        info.setServerStartTime("2024-01-01 00:00:00");
        info.setPgStatStatementsEnabled(true);
        info.setPgStatStatementsVersion("1.10");
        return info;
    }

    // =========================================================================
    // NotificationChannel
    // =========================================================================

    /**
     * Creates a NotificationChannel for Slack.
     */
    public static NotificationChannel createSlackChannel(String name) {
        NotificationChannel channel = new NotificationChannel();
        channel.setName(name);
        channel.setChannelType(NotificationChannel.ChannelType.SLACK);
        channel.setEnabled(true);
        channel.setConfig("{\"webhookUrl\": \"https://hooks.slack.com/test\", \"channel\": \"#alerts\"}");
        return channel;
    }

    /**
     * Creates a NotificationChannel for email.
     */
    public static NotificationChannel createEmailChannel(String name, String... recipients) {
        NotificationChannel channel = new NotificationChannel();
        channel.setName(name);
        channel.setChannelType(NotificationChannel.ChannelType.EMAIL);
        channel.setEnabled(true);
        channel.setConfig("{\"recipients\": " + java.util.Arrays.toString(recipients) + "}");
        return channel;
    }
}
