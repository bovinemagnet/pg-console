package com.bovinemagnet.pgconsole.repository;

import com.bovinemagnet.pgconsole.config.MetadataDataSource;
import com.bovinemagnet.pgconsole.model.ComparisonFilter;
import com.bovinemagnet.pgconsole.model.ComparisonProfile;
import com.bovinemagnet.pgconsole.model.SchemaComparisonResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing schema comparison profiles.
 * <p>
 * Handles persistence of saved comparison configurations
 * for quick re-runs.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class ComparisonProfileRepository {

    private static final Logger LOG = Logger.getLogger(ComparisonProfileRepository.class);

    @Inject
    @MetadataDataSource
    DataSource dataSource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Saves a new comparison profile.
     *
     * @param profile profile to save
     * @return saved profile with generated ID
     */
    public ComparisonProfile save(ComparisonProfile profile) {
        String sql = """
            INSERT INTO pgconsole.comparison_profile
            (name, description, source_instance, destination_instance,
             source_schema, destination_schema, filter_config, is_default,
             created_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, profile.getName());
            stmt.setString(2, profile.getDescription());
            stmt.setString(3, profile.getSourceInstance());
            stmt.setString(4, profile.getDestinationInstance());
            stmt.setString(5, profile.getSourceSchema());
            stmt.setString(6, profile.getDestinationSchema());
            stmt.setString(7, serialiseFilter(profile.getFilter()));
            stmt.setBoolean(8, profile.isDefault());
            stmt.setString(9, profile.getCreatedBy());
            stmt.setTimestamp(10, Timestamp.from(profile.getCreatedAt()));
            stmt.setTimestamp(11, Timestamp.from(profile.getUpdatedAt()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    profile.setId(rs.getLong("id"));
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to save comparison profile: %s", e.getMessage());
            throw new RuntimeException("Failed to save comparison profile", e);
        }

        return profile;
    }

    /**
     * Updates an existing comparison profile.
     *
     * @param profile profile to update
     * @return updated profile
     */
    public ComparisonProfile update(ComparisonProfile profile) {
        String sql = """
            UPDATE pgconsole.comparison_profile
            SET name = ?, description = ?, source_instance = ?,
                destination_instance = ?, source_schema = ?,
                destination_schema = ?, filter_config = ?::jsonb,
                is_default = ?, updated_at = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, profile.getName());
            stmt.setString(2, profile.getDescription());
            stmt.setString(3, profile.getSourceInstance());
            stmt.setString(4, profile.getDestinationInstance());
            stmt.setString(5, profile.getSourceSchema());
            stmt.setString(6, profile.getDestinationSchema());
            stmt.setString(7, serialiseFilter(profile.getFilter()));
            stmt.setBoolean(8, profile.isDefault());
            stmt.setTimestamp(9, Timestamp.from(Instant.now()));
            stmt.setLong(10, profile.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            LOG.errorf("Failed to update comparison profile: %s", e.getMessage());
            throw new RuntimeException("Failed to update comparison profile", e);
        }

        return profile;
    }

    /**
     * Updates the last run information for a profile.
     *
     * @param profileId profile ID
     * @param summary comparison summary
     */
    public void updateLastRun(long profileId, SchemaComparisonResult.ComparisonSummary summary) {
        String sql = """
            UPDATE pgconsole.comparison_profile
            SET last_run_at = ?, last_run_summary = ?::jsonb, updated_at = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.from(Instant.now()));
            stmt.setString(2, serialiseSummary(summary));
            stmt.setTimestamp(3, Timestamp.from(Instant.now()));
            stmt.setLong(4, profileId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            LOG.errorf("Failed to update profile last run: %s", e.getMessage());
        }
    }

    /**
     * Deletes a comparison profile.
     *
     * @param id profile ID
     */
    public void delete(long id) {
        String sql = "DELETE FROM pgconsole.comparison_profile WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            LOG.errorf("Failed to delete comparison profile: %s", e.getMessage());
            throw new RuntimeException("Failed to delete comparison profile", e);
        }
    }

    /**
     * Finds a profile by ID.
     *
     * @param id profile ID
     * @return profile or empty
     */
    public Optional<ComparisonProfile> findById(long id) {
        String sql = """
            SELECT id, name, description, source_instance, destination_instance,
                   source_schema, destination_schema, filter_config, is_default,
                   created_by, created_at, updated_at, last_run_at, last_run_summary
            FROM pgconsole.comparison_profile
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapProfile(rs));
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to find comparison profile: %s", e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Finds all profiles.
     *
     * @return list of all profiles
     */
    public List<ComparisonProfile> findAll() {
        String sql = """
            SELECT id, name, description, source_instance, destination_instance,
                   source_schema, destination_schema, filter_config, is_default,
                   created_by, created_at, updated_at, last_run_at, last_run_summary
            FROM pgconsole.comparison_profile
            ORDER BY name
            """;

        List<ComparisonProfile> profiles = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                profiles.add(mapProfile(rs));
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to find comparison profiles: %s", e.getMessage());
        }

        return profiles;
    }

    /**
     * Finds profiles for a specific instance pair.
     *
     * @param sourceInstance source instance
     * @param destInstance destination instance
     * @return matching profiles
     */
    public List<ComparisonProfile> findByInstances(String sourceInstance, String destInstance) {
        String sql = """
            SELECT id, name, description, source_instance, destination_instance,
                   source_schema, destination_schema, filter_config, is_default,
                   created_by, created_at, updated_at, last_run_at, last_run_summary
            FROM pgconsole.comparison_profile
            WHERE source_instance = ? AND destination_instance = ?
            ORDER BY name
            """;

        List<ComparisonProfile> profiles = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sourceInstance);
            stmt.setString(2, destInstance);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    profiles.add(mapProfile(rs));
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to find profiles by instances: %s", e.getMessage());
        }

        return profiles;
    }

    /**
     * Finds the default profile for an instance pair.
     *
     * @param sourceInstance source instance
     * @param destInstance destination instance
     * @return default profile or empty
     */
    public Optional<ComparisonProfile> findDefault(String sourceInstance, String destInstance) {
        String sql = """
            SELECT id, name, description, source_instance, destination_instance,
                   source_schema, destination_schema, filter_config, is_default,
                   created_by, created_at, updated_at, last_run_at, last_run_summary
            FROM pgconsole.comparison_profile
            WHERE source_instance = ? AND destination_instance = ? AND is_default = TRUE
            LIMIT 1
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sourceInstance);
            stmt.setString(2, destInstance);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapProfile(rs));
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to find default profile: %s", e.getMessage());
        }

        return Optional.empty();
    }

    private ComparisonProfile mapProfile(ResultSet rs) throws SQLException {
        ComparisonProfile profile = new ComparisonProfile();
        profile.setId(rs.getLong("id"));
        profile.setName(rs.getString("name"));
        profile.setDescription(rs.getString("description"));
        profile.setSourceInstance(rs.getString("source_instance"));
        profile.setDestinationInstance(rs.getString("destination_instance"));
        profile.setSourceSchema(rs.getString("source_schema"));
        profile.setDestinationSchema(rs.getString("destination_schema"));
        profile.setFilter(deserialiseFilter(rs.getString("filter_config")));
        profile.setDefault(rs.getBoolean("is_default"));
        profile.setCreatedBy(rs.getString("created_by"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            profile.setCreatedAt(createdAt.toInstant());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            profile.setUpdatedAt(updatedAt.toInstant());
        }

        Timestamp lastRunAt = rs.getTimestamp("last_run_at");
        if (lastRunAt != null) {
            profile.setLastRunAt(lastRunAt.toInstant());
        }

        profile.setLastRunSummary(deserialiseSummary(rs.getString("last_run_summary")));

        return profile;
    }

    private String serialiseFilter(ComparisonFilter filter) {
        if (filter == null) return null;
        try {
            return objectMapper.writeValueAsString(filter);
        } catch (JsonProcessingException e) {
            LOG.warnf("Failed to serialise filter: %s", e.getMessage());
            return null;
        }
    }

    private ComparisonFilter deserialiseFilter(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, ComparisonFilter.class);
        } catch (JsonProcessingException e) {
            LOG.warnf("Failed to deserialise filter: %s", e.getMessage());
            return null;
        }
    }

    private String serialiseSummary(SchemaComparisonResult.ComparisonSummary summary) {
        if (summary == null) return null;
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            LOG.warnf("Failed to serialise summary: %s", e.getMessage());
            return null;
        }
    }

    private SchemaComparisonResult.ComparisonSummary deserialiseSummary(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, SchemaComparisonResult.ComparisonSummary.class);
        } catch (JsonProcessingException e) {
            LOG.warnf("Failed to deserialise summary: %s", e.getMessage());
            return null;
        }
    }
}
