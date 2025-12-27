package com.bovinemagnet.pgconsole.repository;

import com.bovinemagnet.pgconsole.model.ComparisonHistory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing schema comparison history records.
 * <p>
 * Handles persistence of comparison audit logs for
 * trend analysis and drift detection.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class ComparisonHistoryRepository {

    private static final Logger LOG = Logger.getLogger(ComparisonHistoryRepository.class);

    @Inject
    DataSource dataSource;

    /**
     * Saves a comparison history record.
     *
     * @param history history record to save
     * @return saved record with generated ID
     */
    public ComparisonHistory save(ComparisonHistory history) {
        String sql = """
            INSERT INTO pgconsole.comparison_history
            (compared_at, source_instance, destination_instance,
             source_schema, destination_schema, performed_by,
             missing_count, extra_count, modified_count, matching_count,
             profile_name, result_snapshot, filter_config)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)
            RETURNING id
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.from(history.getComparedAt()));
            stmt.setString(2, history.getSourceInstance());
            stmt.setString(3, history.getDestinationInstance());
            stmt.setString(4, history.getSourceSchema());
            stmt.setString(5, history.getDestinationSchema());
            stmt.setString(6, history.getPerformedBy());
            stmt.setInt(7, history.getMissingCount());
            stmt.setInt(8, history.getExtraCount());
            stmt.setInt(9, history.getModifiedCount());
            stmt.setInt(10, history.getMatchingCount());
            stmt.setString(11, history.getProfileName());
            stmt.setString(12, history.getResultSnapshotJson());
            stmt.setString(13, history.getFilterConfigJson());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    history.setId(rs.getLong("id"));
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to save comparison history: %s", e.getMessage());
            throw new RuntimeException("Failed to save comparison history", e);
        }

        return history;
    }

    /**
     * Finds a history record by ID.
     *
     * @param id history ID
     * @return history record or empty
     */
    public Optional<ComparisonHistory> findById(long id) {
        String sql = """
            SELECT id, compared_at, source_instance, destination_instance,
                   source_schema, destination_schema, performed_by,
                   missing_count, extra_count, modified_count, matching_count,
                   profile_name, result_snapshot, filter_config
            FROM pgconsole.comparison_history
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapHistory(rs));
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to find comparison history: %s", e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Finds recent history records.
     *
     * @param limit maximum records to return
     * @return list of history records
     */
    public List<ComparisonHistory> findRecent(int limit) {
        String sql = """
            SELECT id, compared_at, source_instance, destination_instance,
                   source_schema, destination_schema, performed_by,
                   missing_count, extra_count, modified_count, matching_count,
                   profile_name, result_snapshot, filter_config
            FROM pgconsole.comparison_history
            ORDER BY compared_at DESC
            LIMIT ?
            """;

        List<ComparisonHistory> history = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    history.add(mapHistory(rs));
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to find recent history: %s", e.getMessage());
        }

        return history;
    }

    /**
     * Finds history records for a specific instance pair.
     *
     * @param sourceInstance source instance
     * @param destInstance destination instance
     * @param days number of days to look back
     * @return matching history records
     */
    public List<ComparisonHistory> findByInstances(String sourceInstance, String destInstance, int days) {
        String sql = """
            SELECT id, compared_at, source_instance, destination_instance,
                   source_schema, destination_schema, performed_by,
                   missing_count, extra_count, modified_count, matching_count,
                   profile_name, result_snapshot, filter_config
            FROM pgconsole.comparison_history
            WHERE source_instance = ? AND destination_instance = ?
              AND compared_at >= ?
            ORDER BY compared_at DESC
            """;

        List<ComparisonHistory> history = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sourceInstance);
            stmt.setString(2, destInstance);
            stmt.setTimestamp(3, Timestamp.from(Instant.now().minus(days, ChronoUnit.DAYS)));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    history.add(mapHistory(rs));
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to find history by instances: %s", e.getMessage());
        }

        return history;
    }

    /**
     * Finds the most recent comparison for an instance pair.
     *
     * @param sourceInstance source instance
     * @param destInstance destination instance
     * @return most recent history or empty
     */
    public Optional<ComparisonHistory> findMostRecent(String sourceInstance, String destInstance) {
        String sql = """
            SELECT id, compared_at, source_instance, destination_instance,
                   source_schema, destination_schema, performed_by,
                   missing_count, extra_count, modified_count, matching_count,
                   profile_name, result_snapshot, filter_config
            FROM pgconsole.comparison_history
            WHERE source_instance = ? AND destination_instance = ?
            ORDER BY compared_at DESC
            LIMIT 1
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sourceInstance);
            stmt.setString(2, destInstance);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapHistory(rs));
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to find most recent history: %s", e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Deletes history records older than specified days.
     *
     * @param days age threshold in days
     * @return number of records deleted
     */
    public int deleteOlderThan(int days) {
        String sql = "DELETE FROM pgconsole.comparison_history WHERE compared_at < ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.from(Instant.now().minus(days, ChronoUnit.DAYS)));
            return stmt.executeUpdate();

        } catch (SQLException e) {
            LOG.errorf("Failed to delete old history: %s", e.getMessage());
            return 0;
        }
    }

    /**
     * Gets count of history records.
     *
     * @return total count
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM pgconsole.comparison_history";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to count history: %s", e.getMessage());
        }

        return 0;
    }

    private ComparisonHistory mapHistory(ResultSet rs) throws SQLException {
        ComparisonHistory history = new ComparisonHistory();
        history.setId(rs.getLong("id"));

        Timestamp comparedAt = rs.getTimestamp("compared_at");
        if (comparedAt != null) {
            history.setComparedAt(comparedAt.toInstant());
        }

        history.setSourceInstance(rs.getString("source_instance"));
        history.setDestinationInstance(rs.getString("destination_instance"));
        history.setSourceSchema(rs.getString("source_schema"));
        history.setDestinationSchema(rs.getString("destination_schema"));
        history.setPerformedBy(rs.getString("performed_by"));
        history.setMissingCount(rs.getInt("missing_count"));
        history.setExtraCount(rs.getInt("extra_count"));
        history.setModifiedCount(rs.getInt("modified_count"));
        history.setMatchingCount(rs.getInt("matching_count"));
        history.setProfileName(rs.getString("profile_name"));
        history.setResultSnapshotJson(rs.getString("result_snapshot"));
        history.setFilterConfigJson(rs.getString("filter_config"));

        return history;
    }
}
