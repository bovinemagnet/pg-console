package com.bovinemagnet.pgconsole.model;

/**
 * Represents real-time vacuum operation progress from the PostgreSQL system view
 * {@code pg_stat_progress_vacuum}.
 * <p>
 * This class captures detailed information about ongoing VACUUM and AUTOVACUUM operations,
 * including which phase of the vacuum process is currently executing, how many heap blocks
 * have been processed, and overall progress percentage. It supports both manual VACUUM
 * commands and automatic autovacuum operations.
 * </p>
 * <p>
 * The vacuum process typically proceeds through several phases:
 * </p>
 * <ul>
 * <li>{@link VacuumPhase#INITIALIZING} - Setting up the vacuum operation</li>
 * <li>{@link VacuumPhase#SCANNING_HEAP} - Reading table blocks to identify dead tuples</li>
 * <li>{@link VacuumPhase#VACUUMING_INDEXES} - Removing index entries for dead tuples</li>
 * <li>{@link VacuumPhase#VACUUMING_HEAP} - Removing dead tuples from table blocks</li>
 * <li>{@link VacuumPhase#CLEANING_UP_INDEXES} - Final index cleanup</li>
 * <li>{@link VacuumPhase#TRUNCATING_HEAP} - Returning empty pages at end of table to OS</li>
 * <li>{@link VacuumPhase#PERFORMING_FINAL_CLEANUP} - Final cleanup and statistics update</li>
 * </ul>
 * <p>
 * This class also provides utility methods for formatting progress information for display,
 * including CSS class selection for Bootstrap-based UI rendering.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/progress-reporting.html#VACUUM-PROGRESS-REPORTING">PostgreSQL VACUUM Progress Reporting</a>
 */
public class VacuumProgress {

    /**
     * Represents the current phase of a VACUUM operation.
     * <p>
     * Each phase corresponds to a specific stage in the vacuum process as reported by
     * PostgreSQL's {@code pg_stat_progress_vacuum} view. The phases occur in a generally
     * sequential order, though some phases may be skipped or repeated depending on the
     * table structure and vacuum configuration.
     * </p>
     */
    public enum VacuumPhase {
        /** Initial setup phase before vacuum work begins. */
        INITIALIZING("initializing"),

        /** Scanning table blocks to identify dead tuples. */
        SCANNING_HEAP("scanning heap"),

        /** Removing index entries pointing to dead tuples. */
        VACUUMING_INDEXES("vacuuming indexes"),

        /** Removing dead tuples from table blocks. */
        VACUUMING_HEAP("vacuuming heap"),

        /** Final cleanup of index structures. */
        CLEANING_UP_INDEXES("cleaning up indexes"),

        /** Returning empty pages at the end of the table to the operating system. */
        TRUNCATING_HEAP("truncating heap"),

        /** Final cleanup and statistics update. */
        PERFORMING_FINAL_CLEANUP("performing final cleanup"),

        /** Unknown or unrecognised phase. */
        UNKNOWN("unknown");

        private final String displayName;

        /**
         * Constructs a VacuumPhase with the specified display name.
         *
         * @param displayName the human-readable name of this phase
         */
        VacuumPhase(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Returns the human-readable display name of this vacuum phase.
         *
         * @return the display name suitable for user interfaces
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Converts a string representation of a vacuum phase to its corresponding enum constant.
         * <p>
         * The comparison is case-insensitive and matches the phase strings reported by
         * PostgreSQL's {@code pg_stat_progress_vacuum} view.
         * </p>
         *
         * @param phase the string representation of the phase, may be null
         * @return the corresponding {@link VacuumPhase} constant, or {@link #UNKNOWN} if the
         *         phase string is null or does not match any known phase
         */
        public static VacuumPhase fromString(String phase) {
            if (phase == null) return UNKNOWN;
            return switch (phase.toLowerCase()) {
                case "initializing" -> INITIALIZING;
                case "scanning heap" -> SCANNING_HEAP;
                case "vacuuming indexes" -> VACUUMING_INDEXES;
                case "vacuuming heap" -> VACUUMING_HEAP;
                case "cleaning up indexes" -> CLEANING_UP_INDEXES;
                case "truncating heap" -> TRUNCATING_HEAP;
                case "performing final cleanup" -> PERFORMING_FINAL_CLEANUP;
                default -> UNKNOWN;
            };
        }
    }

    /**
     * Process ID of the backend performing the vacuum operation.
     * <p>
     * This corresponds to the {@code pid} column in {@code pg_stat_progress_vacuum}.
     * </p>
     */
    private int pid;

    /**
     * Name of the database containing the table being vacuumed.
     * <p>
     * This corresponds to the {@code datname} column in {@code pg_stat_progress_vacuum}.
     * </p>
     */
    private String database;

    /**
     * Schema name of the table being vacuumed.
     * <p>
     * This corresponds to the {@code schemaname} column in {@code pg_stat_progress_vacuum}.
     * </p>
     */
    private String schemaName;

    /**
     * Name of the table being vacuumed.
     * <p>
     * This corresponds to the {@code relname} column in {@code pg_stat_progress_vacuum}.
     * </p>
     */
    private String tableName;

    /**
     * Current phase of the vacuum operation.
     * <p>
     * This corresponds to the {@code phase} column in {@code pg_stat_progress_vacuum}.
     * </p>
     *
     * @see VacuumPhase
     */
    private VacuumPhase phase;

    /**
     * Total number of heap blocks in the table.
     * <p>
     * This corresponds to the {@code heap_blks_total} column in {@code pg_stat_progress_vacuum}.
     * PostgreSQL uses 8KB blocks by default.
     * </p>
     */
    private long heapBlksTotal;

    /**
     * Number of heap blocks scanned so far.
     * <p>
     * This corresponds to the {@code heap_blks_scanned} column in {@code pg_stat_progress_vacuum}.
     * During the scanning phase, this indicates how much of the table has been read.
     * </p>
     */
    private long heapBlksScanned;

    /**
     * Number of heap blocks vacuumed so far.
     * <p>
     * This corresponds to the {@code heap_blks_vacuumed} column in {@code pg_stat_progress_vacuum}.
     * During the vacuuming phase, this indicates how many blocks have had dead tuples removed.
     * </p>
     */
    private long heapBlksVacuumed;

    /**
     * Number of times indexes have been vacuumed.
     * <p>
     * This corresponds to the {@code index_vacuum_count} column in {@code pg_stat_progress_vacuum}.
     * For tables with many dead tuples, indexes may need to be vacuumed multiple times.
     * </p>
     */
    private long indexVacuumCount;

    /**
     * Maximum number of dead tuples that can be stored in memory before triggering index vacuum.
     * <p>
     * This corresponds to the {@code max_dead_tuples} column in {@code pg_stat_progress_vacuum}.
     * This limit is determined by the {@code maintenance_work_mem} configuration parameter.
     * </p>
     */
    private long maxDeadTuples;

    /**
     * Number of dead tuples currently collected.
     * <p>
     * This corresponds to the {@code num_dead_tuples} column in {@code pg_stat_progress_vacuum}.
     * When this reaches {@link #maxDeadTuples}, an index vacuum cycle is triggered.
     * </p>
     */
    private long numDeadTuples;

    /**
     * Computed progress percentage based on the current phase and blocks processed.
     * <p>
     * This is calculated by {@link #calculateProgress()} and is not directly from PostgreSQL.
     * </p>
     */
    private double progressPercent;

    /**
     * Indicates whether this vacuum operation is an autovacuum (automatic) or manual vacuum.
     * <p>
     * This is determined by checking if the backend application name contains "autovacuum".
     * </p>
     */
    private boolean isAutovacuum;

    /**
     * Constructs an empty VacuumProgress instance.
     * <p>
     * Fields should be populated using setter methods after construction.
     * </p>
     */
    public VacuumProgress() {
    }

    /**
     * Returns the process ID of the backend performing the vacuum operation.
     *
     * @return the process ID
     */
    public int getPid() {
        return pid;
    }

    /**
     * Sets the process ID of the backend performing the vacuum operation.
     *
     * @param pid the process ID
     */
    public void setPid(int pid) {
        this.pid = pid;
    }

    /**
     * Returns the name of the database containing the table being vacuumed.
     *
     * @return the database name
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Sets the name of the database containing the table being vacuumed.
     *
     * @param database the database name
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * Returns the schema name of the table being vacuumed.
     *
     * @return the schema name
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Sets the schema name of the table being vacuumed.
     *
     * @param schemaName the schema name
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Returns the name of the table being vacuumed.
     *
     * @return the table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets the name of the table being vacuumed.
     *
     * @param tableName the table name
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns the fully qualified table name in the format {@code schema.table}.
     * <p>
     * If the schema name is null or empty, returns only the table name.
     * </p>
     *
     * @return the fully qualified table name, or just the table name if schema is not set
     */
    public String getFullTableName() {
        if (schemaName != null && !schemaName.isEmpty()) {
            return schemaName + "." + tableName;
        }
        return tableName;
    }

    /**
     * Returns the current phase of the vacuum operation.
     *
     * @return the vacuum phase
     */
    public VacuumPhase getPhase() {
        return phase;
    }

    /**
     * Sets the current phase of the vacuum operation.
     *
     * @param phase the vacuum phase
     */
    public void setPhase(VacuumPhase phase) {
        this.phase = phase;
    }

    /**
     * Sets the vacuum phase by converting a string representation to the corresponding enum.
     * <p>
     * This is a convenience method that delegates to {@link VacuumPhase#fromString(String)}.
     * </p>
     *
     * @param phaseStr the string representation of the phase, may be null
     * @see VacuumPhase#fromString(String)
     */
    public void setPhaseFromString(String phaseStr) {
        this.phase = VacuumPhase.fromString(phaseStr);
    }

    /**
     * Returns the total number of heap blocks in the table.
     *
     * @return the total number of heap blocks
     */
    public long getHeapBlksTotal() {
        return heapBlksTotal;
    }

    /**
     * Sets the total number of heap blocks in the table.
     *
     * @param heapBlksTotal the total number of heap blocks
     */
    public void setHeapBlksTotal(long heapBlksTotal) {
        this.heapBlksTotal = heapBlksTotal;
    }

    /**
     * Returns the number of heap blocks scanned so far.
     *
     * @return the number of scanned blocks
     */
    public long getHeapBlksScanned() {
        return heapBlksScanned;
    }

    /**
     * Sets the number of heap blocks scanned so far.
     *
     * @param heapBlksScanned the number of scanned blocks
     */
    public void setHeapBlksScanned(long heapBlksScanned) {
        this.heapBlksScanned = heapBlksScanned;
    }

    /**
     * Returns the number of heap blocks vacuumed so far.
     *
     * @return the number of vacuumed blocks
     */
    public long getHeapBlksVacuumed() {
        return heapBlksVacuumed;
    }

    /**
     * Sets the number of heap blocks vacuumed so far.
     *
     * @param heapBlksVacuumed the number of vacuumed blocks
     */
    public void setHeapBlksVacuumed(long heapBlksVacuumed) {
        this.heapBlksVacuumed = heapBlksVacuumed;
    }

    /**
     * Returns the number of times indexes have been vacuumed.
     *
     * @return the index vacuum count
     */
    public long getIndexVacuumCount() {
        return indexVacuumCount;
    }

    /**
     * Sets the number of times indexes have been vacuumed.
     *
     * @param indexVacuumCount the index vacuum count
     */
    public void setIndexVacuumCount(long indexVacuumCount) {
        this.indexVacuumCount = indexVacuumCount;
    }

    /**
     * Returns the maximum number of dead tuples that can be stored in memory.
     *
     * @return the maximum dead tuples threshold
     */
    public long getMaxDeadTuples() {
        return maxDeadTuples;
    }

    /**
     * Sets the maximum number of dead tuples that can be stored in memory.
     *
     * @param maxDeadTuples the maximum dead tuples threshold
     */
    public void setMaxDeadTuples(long maxDeadTuples) {
        this.maxDeadTuples = maxDeadTuples;
    }

    /**
     * Returns the number of dead tuples currently collected.
     *
     * @return the number of dead tuples
     */
    public long getNumDeadTuples() {
        return numDeadTuples;
    }

    /**
     * Sets the number of dead tuples currently collected.
     *
     * @param numDeadTuples the number of dead tuples
     */
    public void setNumDeadTuples(long numDeadTuples) {
        this.numDeadTuples = numDeadTuples;
    }

    /**
     * Returns the computed progress percentage.
     *
     * @return the progress percentage (0-100)
     * @see #calculateProgress()
     */
    public double getProgressPercent() {
        return progressPercent;
    }

    /**
     * Sets the progress percentage.
     *
     * @param progressPercent the progress percentage (0-100)
     */
    public void setProgressPercent(double progressPercent) {
        this.progressPercent = progressPercent;
    }

    /**
     * Calculates the progress percentage based on the current phase and blocks processed.
     * <p>
     * During the {@link VacuumPhase#SCANNING_HEAP} phase, progress is calculated using
     * {@link #heapBlksScanned}. For all other phases, {@link #heapBlksVacuumed} is used.
     * If {@link #heapBlksTotal} is zero, the progress is set to 0%.
     * </p>
     * <p>
     * This method updates {@link #progressPercent} and should be called after setting
     * the block counts from the PostgreSQL system view.
     * </p>
     */
    public void calculateProgress() {
        if (heapBlksTotal > 0) {
            // Use scanned for scanning phase, vacuumed for vacuum phase
            long completed = phase == VacuumPhase.SCANNING_HEAP ? heapBlksScanned : heapBlksVacuumed;
            this.progressPercent = (completed * 100.0) / heapBlksTotal;
        } else {
            this.progressPercent = 0;
        }
    }

    /**
     * Returns whether this vacuum operation is an autovacuum.
     *
     * @return {@code true} if this is an autovacuum operation, {@code false} for manual vacuum
     */
    public boolean isAutovacuum() {
        return isAutovacuum;
    }

    /**
     * Sets whether this vacuum operation is an autovacuum.
     *
     * @param autovacuum {@code true} if this is an autovacuum operation, {@code false} for manual vacuum
     */
    public void setAutovacuum(boolean autovacuum) {
        isAutovacuum = autovacuum;
    }

    /**
     * Returns the progress percentage formatted as a string with one decimal place.
     * <p>
     * For example, if the progress percentage is 45.67, this method returns "45.7%".
     * </p>
     *
     * @return the formatted progress percentage with a percent sign
     */
    public String getProgressFormatted() {
        return String.format("%.1f%%", progressPercent);
    }

    /**
     * Returns the Bootstrap CSS class appropriate for the current vacuum phase.
     * <p>
     * This method maps vacuum phases to Bootstrap background colour classes for
     * visual differentiation in the user interface:
     * </p>
     * <ul>
     * <li>{@link VacuumPhase#SCANNING_HEAP} - {@code bg-info} (light blue)</li>
     * <li>{@link VacuumPhase#VACUUMING_INDEXES} - {@code bg-warning text-dark} (yellow)</li>
     * <li>{@link VacuumPhase#VACUUMING_HEAP} - {@code bg-primary} (blue)</li>
     * <li>{@link VacuumPhase#CLEANING_UP_INDEXES} - {@code bg-secondary} (grey)</li>
     * <li>{@link VacuumPhase#TRUNCATING_HEAP} - {@code bg-danger} (red)</li>
     * <li>{@link VacuumPhase#PERFORMING_FINAL_CLEANUP} - {@code bg-success} (green)</li>
     * <li>Other phases - {@code bg-secondary} (grey)</li>
     * </ul>
     *
     * @return the Bootstrap CSS class name for the current phase
     */
    public String getPhaseCssClass() {
        return switch (phase) {
            case SCANNING_HEAP -> "bg-info";
            case VACUUMING_INDEXES -> "bg-warning text-dark";
            case VACUUMING_HEAP -> "bg-primary";
            case CLEANING_UP_INDEXES -> "bg-secondary";
            case TRUNCATING_HEAP -> "bg-danger";
            case PERFORMING_FINAL_CLEANUP -> "bg-success";
            default -> "bg-secondary";
        };
    }

    /**
     * Returns the Bootstrap CSS class for the progress bar based on completion percentage.
     * <p>
     * The colour changes based on progress:
     * </p>
     * <ul>
     * <li>90% or higher - {@code bg-success} (green)</li>
     * <li>50% to 89% - {@code bg-info} (light blue)</li>
     * <li>Below 50% - {@code bg-primary} (blue)</li>
     * </ul>
     *
     * @return the Bootstrap CSS class name for the progress bar
     */
    public String getProgressBarCssClass() {
        if (progressPercent >= 90) return "bg-success";
        if (progressPercent >= 50) return "bg-info";
        return "bg-primary";
    }

    /**
     * Returns the number of scanned heap blocks formatted as a human-readable size.
     * <p>
     * Blocks are converted from 8KB units to KB, MB, or GB depending on the size.
     * </p>
     *
     * @return the formatted size (e.g., "1.5 MB", "2.3 GB")
     * @see #formatBlocks(long)
     */
    public String getHeapBlksScannedFormatted() {
        return formatBlocks(heapBlksScanned);
    }

    /**
     * Returns the total number of heap blocks formatted as a human-readable size.
     * <p>
     * Blocks are converted from 8KB units to KB, MB, or GB depending on the size.
     * </p>
     *
     * @return the formatted size (e.g., "1.5 MB", "2.3 GB")
     * @see #formatBlocks(long)
     */
    public String getHeapBlksTotalFormatted() {
        return formatBlocks(heapBlksTotal);
    }

    /**
     * Formats a block count as a human-readable size string.
     * <p>
     * PostgreSQL uses 8KB blocks by default. This method converts the block count
     * to bytes and then formats it as KB, MB, or GB depending on the magnitude:
     * </p>
     * <ul>
     * <li>Less than 1 MB - formatted as KB</li>
     * <li>1 MB to 1 GB - formatted as MB</li>
     * <li>1 GB or more - formatted as GB</li>
     * </ul>
     *
     * @param blocks the number of 8KB blocks
     * @return the formatted size string with one decimal place and unit (KB, MB, or GB)
     */
    private String formatBlocks(long blocks) {
        // Assuming 8KB blocks
        long bytes = blocks * 8192;
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
