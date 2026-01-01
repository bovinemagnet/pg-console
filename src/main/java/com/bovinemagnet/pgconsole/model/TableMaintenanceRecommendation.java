package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Represents a table maintenance recommendation (vacuum, analyse, reindex).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class TableMaintenanceRecommendation {

    public enum MaintenanceType {
        VACUUM,          // Table needs vacuum
        ANALYSE,         // Table needs analyse/statistics update
        VACUUM_FULL,     // Table needs vacuum full (high bloat)
        REINDEX          // Table indexes need rebuilding
    }

    public enum Severity {
        CRITICAL,   // Immediate action needed
        HIGH,       // Should address soon
        MEDIUM,     // Worth investigating
        LOW         // Informational
    }

    private String schemaName;
    private String tableName;
    private MaintenanceType type;
    private Severity severity;
    private String recommendation;
    private String rationale;

    // Table statistics
    private long liveTuples;
    private long deadTuples;
    private double deadTupleRatio;
    private String tableSize;
    private long tableSizeBytes;

    // Vacuum/Analyse timestamps
    private Instant lastVacuum;
    private Instant lastAutoVacuum;
    private Instant lastAnalyse;
    private Instant lastAutoAnalyse;

    // Computed metrics
    private long daysSinceVacuum;
    private long daysSinceAnalyse;
    private boolean neverVacuumed;
    private boolean neverAnalysed;

    // Bloat estimate
    private double estimatedBloatPercent;
    private String estimatedBloatSize;

    public TableMaintenanceRecommendation() {
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

    public MaintenanceType getType() {
        return type;
    }

    public void setType(MaintenanceType type) {
        this.type = type;
    }

    public String getTypeDisplay() {
        return switch (type) {
            case VACUUM -> "Vacuum";
            case ANALYSE -> "Analyse";
            case VACUUM_FULL -> "Vacuum Full";
            case REINDEX -> "Reindex";
        };
    }

    public String getTypeCssClass() {
        return switch (type) {
            case VACUUM -> "bg-warning text-dark";
            case ANALYSE -> "bg-info";
            case VACUUM_FULL -> "bg-danger";
            case REINDEX -> "bg-secondary";
        };
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getSeverityCssClass() {
        return switch (severity) {
            case CRITICAL -> "bg-danger";
            case HIGH -> "bg-warning text-dark";
            case MEDIUM -> "bg-info";
            case LOW -> "bg-secondary";
        };
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public long getLiveTuples() {
        return liveTuples;
    }

    public void setLiveTuples(long liveTuples) {
        this.liveTuples = liveTuples;
    }

    public long getDeadTuples() {
        return deadTuples;
    }

    public void setDeadTuples(long deadTuples) {
        this.deadTuples = deadTuples;
    }

    public double getDeadTupleRatio() {
        return deadTupleRatio;
    }

    public void setDeadTupleRatio(double deadTupleRatio) {
        this.deadTupleRatio = deadTupleRatio;
    }

    public String getDeadTupleRatioFormatted() {
        return String.format("%.1f%%", deadTupleRatio * 100);
    }

    public String getTableSize() {
        return tableSize;
    }

    public void setTableSize(String tableSize) {
        this.tableSize = tableSize;
    }

    public long getTableSizeBytes() {
        return tableSizeBytes;
    }

    public void setTableSizeBytes(long tableSizeBytes) {
        this.tableSizeBytes = tableSizeBytes;
    }

    public Instant getLastVacuum() {
        return lastVacuum;
    }

    public void setLastVacuum(Instant lastVacuum) {
        this.lastVacuum = lastVacuum;
        if (lastVacuum != null) {
            this.daysSinceVacuum = ChronoUnit.DAYS.between(lastVacuum, Instant.now());
            this.neverVacuumed = false;
        } else {
            this.neverVacuumed = true;
        }
    }

    public Instant getLastAutoVacuum() {
        return lastAutoVacuum;
    }

    public void setLastAutoVacuum(Instant lastAutoVacuum) {
        this.lastAutoVacuum = lastAutoVacuum;
    }

    public Instant getLastAnalyse() {
        return lastAnalyse;
    }

    public void setLastAnalyse(Instant lastAnalyse) {
        this.lastAnalyse = lastAnalyse;
        if (lastAnalyse != null) {
            this.daysSinceAnalyse = ChronoUnit.DAYS.between(lastAnalyse, Instant.now());
            this.neverAnalysed = false;
        } else {
            this.neverAnalysed = true;
        }
    }

    public Instant getLastAutoAnalyse() {
        return lastAutoAnalyse;
    }

    public void setLastAutoAnalyse(Instant lastAutoAnalyse) {
        this.lastAutoAnalyse = lastAutoAnalyse;
    }

    public long getDaysSinceVacuum() {
        return daysSinceVacuum;
    }

    public long getDaysSinceAnalyse() {
        return daysSinceAnalyse;
    }

    public boolean isNeverVacuumed() {
        return neverVacuumed;
    }

    public boolean isNeverAnalysed() {
        return neverAnalysed;
    }

    public String getLastVacuumDisplay() {
        if (neverVacuumed) {
            return "Never";
        }
        if (daysSinceVacuum == 0) {
            return "Today";
        } else if (daysSinceVacuum == 1) {
            return "Yesterday";
        } else {
            return daysSinceVacuum + " days ago";
        }
    }

    public String getLastAnalyseDisplay() {
        if (neverAnalysed) {
            return "Never";
        }
        if (daysSinceAnalyse == 0) {
            return "Today";
        } else if (daysSinceAnalyse == 1) {
            return "Yesterday";
        } else {
            return daysSinceAnalyse + " days ago";
        }
    }

    public double getEstimatedBloatPercent() {
        return estimatedBloatPercent;
    }

    public void setEstimatedBloatPercent(double estimatedBloatPercent) {
        this.estimatedBloatPercent = estimatedBloatPercent;
    }

    public String getEstimatedBloatPercentFormatted() {
        return String.format("%.1f%%", estimatedBloatPercent);
    }

    public String getEstimatedBloatSize() {
        return estimatedBloatSize;
    }

    public void setEstimatedBloatSize(String estimatedBloatSize) {
        this.estimatedBloatSize = estimatedBloatSize;
    }

    /**
     * Gets the most recent vacuum timestamp (manual or auto).
     */
    public Instant getEffectiveLastVacuum() {
        if (lastVacuum == null) return lastAutoVacuum;
        if (lastAutoVacuum == null) return lastVacuum;
        return lastVacuum.isAfter(lastAutoVacuum) ? lastVacuum : lastAutoVacuum;
    }

    /**
     * Gets the most recent analyse timestamp (manual or auto).
     */
    public Instant getEffectiveLastAnalyse() {
        if (lastAnalyse == null) return lastAutoAnalyse;
        if (lastAutoAnalyse == null) return lastAnalyse;
        return lastAnalyse.isAfter(lastAutoAnalyse) ? lastAnalyse : lastAutoAnalyse;
    }
}
