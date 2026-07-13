package com.bovinemagnet.pgconsole.repository;

import com.bovinemagnet.pgconsole.config.MetadataDataSource;
import com.bovinemagnet.pgconsole.model.EscalationPolicy;
import com.bovinemagnet.pgconsole.model.EscalationPolicy.EscalationTier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing escalation policies.
 * <p>
 * Provides CRUD operations for escalation policies and their tiers.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class EscalationPolicyRepository {

    @Inject
    @MetadataDataSource
    DataSource dataSource;

    @Inject
    NotificationChannelRepository channelRepository;

    /**
     * Finds all escalation policies.
     *
     * @return list of all policies
     */
    public List<EscalationPolicy> findAll() {
        String sql = """
            SELECT id, name, description, enabled, repeat_count, created_at, updated_at
            FROM pgconsole.escalation_policy
            ORDER BY name
            """;

        List<EscalationPolicy> policies = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                EscalationPolicy policy = mapRow(rs);
                policy.setTiers(findTiersByPolicyId(policy.getId()));
                policies.add(policy);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch escalation policies", e);
        }
        return policies;
    }

    /**
     * Finds an escalation policy by ID.
     *
     * @param id policy ID
     * @return optional containing policy if found
     */
    public Optional<EscalationPolicy> findById(Long id) {
        String sql = """
            SELECT id, name, description, enabled, repeat_count, created_at, updated_at
            FROM pgconsole.escalation_policy
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    EscalationPolicy policy = mapRow(rs);
                    policy.setTiers(findTiersByPolicyId(policy.getId()));
                    return Optional.of(policy);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch escalation policy by ID", e);
        }
        return Optional.empty();
    }

    /**
     * Finds all enabled escalation policies.
     *
     * @return list of enabled policies
     */
    public List<EscalationPolicy> findEnabled() {
        String sql = """
            SELECT id, name, description, enabled, repeat_count, created_at, updated_at
            FROM pgconsole.escalation_policy
            WHERE enabled = TRUE
            ORDER BY name
            """;

        List<EscalationPolicy> policies = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                EscalationPolicy policy = mapRow(rs);
                policy.setTiers(findTiersByPolicyId(policy.getId()));
                policies.add(policy);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch enabled escalation policies", e);
        }
        return policies;
    }

    /**
     * Saves a new escalation policy.
     *
     * @param policy policy to save
     * @return saved policy with ID
     */
    public EscalationPolicy save(EscalationPolicy policy) {
        String sql = """
            INSERT INTO pgconsole.escalation_policy (name, description, enabled, repeat_count)
            VALUES (?, ?, ?, ?)
            RETURNING id, created_at, updated_at
            """;

        try (Connection conn = dataSource.getConnection()) {
            // Policy insert + tier inserts must be atomic (M14).
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, policy.getName());
                    stmt.setString(2, policy.getDescription());
                    stmt.setBoolean(3, policy.isEnabled());
                    stmt.setInt(4, policy.getRepeatCount());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            policy.setId(rs.getLong("id"));
                            policy.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                            policy.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
                        }
                    }
                }

                for (EscalationTier tier : policy.getTiers()) {
                    saveTier(conn, policy.getId(), tier);
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save escalation policy", e);
        }
        return policy;
    }

    /**
     * Updates an existing escalation policy.
     *
     * @param policy policy to update
     * @return updated policy
     */
    public EscalationPolicy update(EscalationPolicy policy) {
        String sql = """
            UPDATE pgconsole.escalation_policy
            SET name = ?, description = ?, enabled = ?, repeat_count = ?, updated_at = NOW()
            WHERE id = ?
            RETURNING updated_at
            """;

        try (Connection conn = dataSource.getConnection()) {
            // Policy update + tier delete/re-insert must be atomic; a failure after
            // the delete would otherwise leave the policy with zero tiers so it
            // silently never escalates (M14).
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, policy.getName());
                    stmt.setString(2, policy.getDescription());
                    stmt.setBoolean(3, policy.isEnabled());
                    stmt.setInt(4, policy.getRepeatCount());
                    stmt.setLong(5, policy.getId());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            policy.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
                        }
                    }
                }

                deleteTiersByPolicyId(conn, policy.getId());
                for (EscalationTier tier : policy.getTiers()) {
                    saveTier(conn, policy.getId(), tier);
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update escalation policy", e);
        }
        return policy;
    }

    /**
     * Deletes an escalation policy.
     *
     * @param id policy ID
     */
    public void delete(Long id) {
        String sql = "DELETE FROM pgconsole.escalation_policy WHERE id = ?";
        try (Connection conn = dataSource.getConnection()) {
            // Delete tiers and the policy atomically (M14).
            conn.setAutoCommit(false);
            try {
                deleteTiersByPolicyId(conn, id);
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setLong(1, id);
                    stmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete escalation policy", e);
        }
    }

    private List<EscalationTier> findTiersByPolicyId(Long policyId) {
        String sql = """
            SELECT id, policy_id, tier_order, delay_minutes, channel_ids
            FROM pgconsole.escalation_tier
            WHERE policy_id = ?
            ORDER BY tier_order
            """;

        List<EscalationTier> tiers = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, policyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tiers.add(mapTierRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch escalation tiers", e);
        }
        return tiers;
    }

    /**
     * Inserts a tier on the given connection so it participates in the caller's
     * transaction (M14).
     */
    private void saveTier(Connection conn, Long policyId, EscalationTier tier) throws SQLException {
        String sql = """
            INSERT INTO pgconsole.escalation_tier (policy_id, tier_order, delay_minutes, channel_ids)
            VALUES (?, ?, ?, ?)
            RETURNING id
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, policyId);
            stmt.setInt(2, tier.getTierOrder());
            stmt.setInt(3, tier.getDelayMinutes());
            stmt.setArray(4, conn.createArrayOf("bigint",
                tier.getChannelIds() != null ? tier.getChannelIds().toArray() : new Long[0]));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    tier.setId(rs.getLong("id"));
                    tier.setPolicyId(policyId);
                }
            }
        }
    }

    /**
     * Deletes a policy's tiers on the given connection so it participates in the
     * caller's transaction (M14).
     */
    private void deleteTiersByPolicyId(Connection conn, Long policyId) throws SQLException {
        String sql = "DELETE FROM pgconsole.escalation_tier WHERE policy_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, policyId);
            stmt.executeUpdate();
        }
    }

    private EscalationPolicy mapRow(ResultSet rs) throws SQLException {
        EscalationPolicy policy = new EscalationPolicy();
        policy.setId(rs.getLong("id"));
        policy.setName(rs.getString("name"));
        policy.setDescription(rs.getString("description"));
        policy.setEnabled(rs.getBoolean("enabled"));
        policy.setRepeatCount(rs.getInt("repeat_count"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            policy.setCreatedAt(createdAt.toInstant());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            policy.setUpdatedAt(updatedAt.toInstant());
        }

        return policy;
    }

    private EscalationTier mapTierRow(ResultSet rs) throws SQLException {
        EscalationTier tier = new EscalationTier();
        tier.setId(rs.getLong("id"));
        tier.setPolicyId(rs.getLong("policy_id"));
        tier.setTierOrder(rs.getInt("tier_order"));
        tier.setDelayMinutes(rs.getInt("delay_minutes"));

        Array channelIdsArray = rs.getArray("channel_ids");
        if (channelIdsArray != null) {
            Long[] ids = (Long[]) channelIdsArray.getArray();
            tier.setChannelIds(new ArrayList<>(List.of(ids)));
        }

        return tier;
    }
}
