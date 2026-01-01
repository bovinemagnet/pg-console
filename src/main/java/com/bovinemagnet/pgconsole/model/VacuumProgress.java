package com.bovinemagnet.pgconsole.model;

/**
 * Represents vacuum progress from pg_stat_progress_vacuum.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class VacuumProgress {

    public enum VacuumPhase {
        INITIALIZING("initializing"),
        SCANNING_HEAP("scanning heap"),
        VACUUMING_INDEXES("vacuuming indexes"),
        VACUUMING_HEAP("vacuuming heap"),
        CLEANING_UP_INDEXES("cleaning up indexes"),
        TRUNCATING_HEAP("truncating heap"),
        PERFORMING_FINAL_CLEANUP("performing final cleanup"),
        UNKNOWN("unknown");

        private final String displayName;

        VacuumPhase(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

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

    private int pid;
    private String database;
    private String schemaName;
    private String tableName;
    private VacuumPhase phase;
    private long heapBlksTotal;
    private long heapBlksScanned;
    private long heapBlksVacuumed;
    private long indexVacuumCount;
    private long maxDeadTuples;
    private long numDeadTuples;

    // Computed fields
    private double progressPercent;
    private boolean isAutovacuum;

    public VacuumProgress() {
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getFullTableName() {
        if (schemaName != null && !schemaName.isEmpty()) {
            return schemaName + "." + tableName;
        }
        return tableName;
    }

    public VacuumPhase getPhase() {
        return phase;
    }

    public void setPhase(VacuumPhase phase) {
        this.phase = phase;
    }

    public void setPhaseFromString(String phaseStr) {
        this.phase = VacuumPhase.fromString(phaseStr);
    }

    public long getHeapBlksTotal() {
        return heapBlksTotal;
    }

    public void setHeapBlksTotal(long heapBlksTotal) {
        this.heapBlksTotal = heapBlksTotal;
    }

    public long getHeapBlksScanned() {
        return heapBlksScanned;
    }

    public void setHeapBlksScanned(long heapBlksScanned) {
        this.heapBlksScanned = heapBlksScanned;
    }

    public long getHeapBlksVacuumed() {
        return heapBlksVacuumed;
    }

    public void setHeapBlksVacuumed(long heapBlksVacuumed) {
        this.heapBlksVacuumed = heapBlksVacuumed;
    }

    public long getIndexVacuumCount() {
        return indexVacuumCount;
    }

    public void setIndexVacuumCount(long indexVacuumCount) {
        this.indexVacuumCount = indexVacuumCount;
    }

    public long getMaxDeadTuples() {
        return maxDeadTuples;
    }

    public void setMaxDeadTuples(long maxDeadTuples) {
        this.maxDeadTuples = maxDeadTuples;
    }

    public long getNumDeadTuples() {
        return numDeadTuples;
    }

    public void setNumDeadTuples(long numDeadTuples) {
        this.numDeadTuples = numDeadTuples;
    }

    public double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(double progressPercent) {
        this.progressPercent = progressPercent;
    }

    public void calculateProgress() {
        if (heapBlksTotal > 0) {
            // Use scanned for scanning phase, vacuumed for vacuum phase
            long completed = phase == VacuumPhase.SCANNING_HEAP ? heapBlksScanned : heapBlksVacuumed;
            this.progressPercent = (completed * 100.0) / heapBlksTotal;
        } else {
            this.progressPercent = 0;
        }
    }

    public boolean isAutovacuum() {
        return isAutovacuum;
    }

    public void setAutovacuum(boolean autovacuum) {
        isAutovacuum = autovacuum;
    }

    public String getProgressFormatted() {
        return String.format("%.1f%%", progressPercent);
    }

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

    public String getProgressBarCssClass() {
        if (progressPercent >= 90) return "bg-success";
        if (progressPercent >= 50) return "bg-info";
        return "bg-primary";
    }

    public String getHeapBlksScannedFormatted() {
        return formatBlocks(heapBlksScanned);
    }

    public String getHeapBlksTotalFormatted() {
        return formatBlocks(heapBlksTotal);
    }

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
