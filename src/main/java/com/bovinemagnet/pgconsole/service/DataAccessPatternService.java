package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.PiiColumnIndicator;
import com.bovinemagnet.pgconsole.model.RlsPolicy;
import com.bovinemagnet.pgconsole.model.SensitiveTable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service for analysing data access patterns and detecting sensitive data.
 * <p>
 * Uses heuristic pattern matching to detect PII columns and analyses
 * row-level security policies.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class DataAccessPatternService {

    private static final Logger LOG = Logger.getLogger(DataAccessPatternService.class);

    @Inject
    DataSourceManager dataSourceManager;

    // PII detection patterns
    private static final Map<Pattern, PiiColumnIndicator.PiiType> PII_PATTERNS = Map.ofEntries(
            Map.entry(Pattern.compile("(?i).*email.*"), PiiColumnIndicator.PiiType.EMAIL),
            Map.entry(Pattern.compile("(?i).*e_mail.*"), PiiColumnIndicator.PiiType.EMAIL),
            Map.entry(Pattern.compile("(?i).*phone.*"), PiiColumnIndicator.PiiType.PHONE),
            Map.entry(Pattern.compile("(?i).*mobile.*"), PiiColumnIndicator.PiiType.PHONE),
            Map.entry(Pattern.compile("(?i).*telephone.*"), PiiColumnIndicator.PiiType.PHONE),
            Map.entry(Pattern.compile("(?i).*ssn.*"), PiiColumnIndicator.PiiType.SSN),
            Map.entry(Pattern.compile("(?i).*social_security.*"), PiiColumnIndicator.PiiType.SSN),
            Map.entry(Pattern.compile("(?i).*national_insurance.*"), PiiColumnIndicator.PiiType.SSN),
            Map.entry(Pattern.compile("(?i).*ni_number.*"), PiiColumnIndicator.PiiType.SSN),
            Map.entry(Pattern.compile("(?i).*password.*"), PiiColumnIndicator.PiiType.PASSWORD),
            Map.entry(Pattern.compile("(?i).*passwd.*"), PiiColumnIndicator.PiiType.PASSWORD),
            Map.entry(Pattern.compile("(?i).*secret.*"), PiiColumnIndicator.PiiType.PASSWORD),
            Map.entry(Pattern.compile("(?i).*birth.*"), PiiColumnIndicator.PiiType.DOB),
            Map.entry(Pattern.compile("(?i).*dob.*"), PiiColumnIndicator.PiiType.DOB),
            Map.entry(Pattern.compile("(?i).*date_of_birth.*"), PiiColumnIndicator.PiiType.DOB),
            Map.entry(Pattern.compile("(?i).*address.*"), PiiColumnIndicator.PiiType.ADDRESS),
            Map.entry(Pattern.compile("(?i).*street.*"), PiiColumnIndicator.PiiType.ADDRESS),
            Map.entry(Pattern.compile("(?i).*postcode.*"), PiiColumnIndicator.PiiType.ADDRESS),
            Map.entry(Pattern.compile("(?i).*zip_code.*"), PiiColumnIndicator.PiiType.ADDRESS),
            Map.entry(Pattern.compile("(?i).*first_name.*"), PiiColumnIndicator.PiiType.NAME),
            Map.entry(Pattern.compile("(?i).*last_name.*"), PiiColumnIndicator.PiiType.NAME),
            Map.entry(Pattern.compile("(?i).*surname.*"), PiiColumnIndicator.PiiType.NAME),
            Map.entry(Pattern.compile("(?i).*full_name.*"), PiiColumnIndicator.PiiType.NAME),
            Map.entry(Pattern.compile("(?i).*credit_card.*"), PiiColumnIndicator.PiiType.FINANCIAL),
            Map.entry(Pattern.compile("(?i).*card_number.*"), PiiColumnIndicator.PiiType.FINANCIAL),
            Map.entry(Pattern.compile("(?i).*account_number.*"), PiiColumnIndicator.PiiType.FINANCIAL),
            Map.entry(Pattern.compile("(?i).*bank_account.*"), PiiColumnIndicator.PiiType.FINANCIAL),
            Map.entry(Pattern.compile("(?i).*iban.*"), PiiColumnIndicator.PiiType.FINANCIAL),
            Map.entry(Pattern.compile("(?i).*passport.*"), PiiColumnIndicator.PiiType.ID_DOCUMENT),
            Map.entry(Pattern.compile("(?i).*driver.*license.*"), PiiColumnIndicator.PiiType.ID_DOCUMENT),
            Map.entry(Pattern.compile("(?i).*driving.*licence.*"), PiiColumnIndicator.PiiType.ID_DOCUMENT),
            Map.entry(Pattern.compile("(?i).*ip_addr.*"), PiiColumnIndicator.PiiType.IP_ADDRESS),
            Map.entry(Pattern.compile("(?i).*client_ip.*"), PiiColumnIndicator.PiiType.IP_ADDRESS),
            Map.entry(Pattern.compile("(?i).*remote_addr.*"), PiiColumnIndicator.PiiType.IP_ADDRESS),
            Map.entry(Pattern.compile("(?i).*medical.*"), PiiColumnIndicator.PiiType.HEALTH),
            Map.entry(Pattern.compile("(?i).*health.*"), PiiColumnIndicator.PiiType.HEALTH),
            Map.entry(Pattern.compile("(?i).*diagnosis.*"), PiiColumnIndicator.PiiType.HEALTH)
    );

    /**
     * Detects columns that may contain PII based on column names.
     *
     * @param instanceName the database instance identifier
     * @return list of PiiColumnIndicator objects
     */
    public List<PiiColumnIndicator> detectPiiColumns(String instanceName) {
        List<PiiColumnIndicator> piiColumns = new ArrayList<>();

        String sql = """
            SELECT
                n.nspname AS schema_name,
                c.relname AS table_name,
                a.attname AS column_name,
                pg_catalog.format_type(a.atttypid, a.atttypmod) AS data_type
            FROM pg_attribute a
            JOIN pg_class c ON a.attrelid = c.oid
            JOIN pg_namespace n ON c.relnamespace = n.oid
            WHERE a.attnum > 0
              AND NOT a.attisdropped
              AND c.relkind IN ('r', 'p')
              AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
            ORDER BY n.nspname, c.relname, a.attnum
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String columnName = rs.getString("column_name");
                PiiColumnIndicator.PiiType piiType = detectPiiType(columnName);

                if (piiType != null) {
                    PiiColumnIndicator indicator = new PiiColumnIndicator();
                    indicator.setSchemaName(rs.getString("schema_name"));
                    indicator.setTableName(rs.getString("table_name"));
                    indicator.setColumnName(columnName);
                    indicator.setDataType(rs.getString("data_type"));
                    indicator.setPiiType(piiType);
                    indicator.setMatchReason("Column name matches pattern for " + piiType.getDisplayName());
                    indicator.setConfidenceScore(calculateConfidence(columnName, piiType));
                    piiColumns.add(indicator);
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Error detecting PII columns for instance %s: %s", instanceName, e.getMessage());
        }

        return piiColumns;
    }

    private PiiColumnIndicator.PiiType detectPiiType(String columnName) {
        for (Map.Entry<Pattern, PiiColumnIndicator.PiiType> entry : PII_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(columnName).matches()) {
                return entry.getValue();
            }
        }
        return null;
    }

    private double calculateConfidence(String columnName, PiiColumnIndicator.PiiType type) {
        // Higher confidence for more specific matches
        String lowerName = columnName.toLowerCase();
        return switch (type) {
            case EMAIL -> lowerName.equals("email") ? 0.95 : 0.8;
            case SSN -> lowerName.contains("ssn") || lowerName.contains("social_security") ? 0.95 : 0.85;
            case PASSWORD -> lowerName.equals("password") ? 0.95 : 0.85;
            case PHONE -> lowerName.equals("phone") || lowerName.equals("mobile") ? 0.9 : 0.75;
            case FINANCIAL -> 0.9;
            case HEALTH -> 0.85;
            case ID_DOCUMENT -> 0.9;
            default -> 0.7;
        };
    }

    /**
     * Detects tables that may contain sensitive data.
     *
     * @param instanceName the database instance identifier
     * @return list of SensitiveTable objects
     */
    public List<SensitiveTable> detectSensitiveTables(String instanceName) {
        List<PiiColumnIndicator> piiColumns = detectPiiColumns(instanceName);

        // Group by table
        Map<String, SensitiveTable> tableMap = new HashMap<>();
        for (PiiColumnIndicator col : piiColumns) {
            String key = col.getSchemaName() + "." + col.getTableName();
            SensitiveTable table = tableMap.computeIfAbsent(key, k -> {
                SensitiveTable st = new SensitiveTable();
                st.setSchemaName(col.getSchemaName());
                st.setTableName(col.getTableName());
                return st;
            });
            table.addPiiColumn(col);
        }

        // Determine sensitivity level and check RLS status
        List<SensitiveTable> tables = new ArrayList<>(tableMap.values());
        Map<String, Integer> rlsStatus = getRlsStatusByTable(instanceName);

        for (SensitiveTable table : tables) {
            // Set sensitivity based on highest sensitivity column
            SensitiveTable.SensitivityLevel maxLevel = SensitiveTable.SensitivityLevel.LOW;
            for (PiiColumnIndicator col : table.getPiiColumns()) {
                SensitiveTable.SensitivityLevel colLevel = col.getPiiType().getSensitivityLevel();
                if (colLevel.ordinal() < maxLevel.ordinal()) {
                    maxLevel = colLevel;
                }
            }
            table.setSensitivityLevel(maxLevel);

            // Set RLS status
            String key = table.getSchemaName() + "." + table.getTableName();
            Integer policyCount = rlsStatus.get(key);
            table.setHasRls(policyCount != null && policyCount > 0);
            table.setPolicyCount(policyCount != null ? policyCount : 0);
        }

        // Sort by sensitivity level
        tables.sort((a, b) -> a.getSensitivityLevel().ordinal() - b.getSensitivityLevel().ordinal());

        return tables;
    }

    private Map<String, Integer> getRlsStatusByTable(String instanceName) {
        Map<String, Integer> status = new HashMap<>();

        String sql = """
            SELECT
                n.nspname || '.' || c.relname AS table_name,
                CASE WHEN c.relrowsecurity THEN COUNT(p.polname) ELSE 0 END AS policy_count
            FROM pg_class c
            JOIN pg_namespace n ON c.relnamespace = n.oid
            LEFT JOIN pg_policy p ON p.polrelid = c.oid
            WHERE c.relkind IN ('r', 'p')
              AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
            GROUP BY n.nspname, c.relname, c.relrowsecurity
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                status.put(rs.getString("table_name"), rs.getInt("policy_count"));
            }

        } catch (SQLException e) {
            LOG.errorf("Error fetching RLS status for instance %s: %s", instanceName, e.getMessage());
        }

        return status;
    }

    /**
     * Retrieves all RLS policies.
     *
     * @param instanceName the database instance identifier
     * @return list of RlsPolicy objects
     */
    public List<RlsPolicy> getRlsPolicies(String instanceName) {
        List<RlsPolicy> policies = new ArrayList<>();

        String sql = """
            SELECT
                n.nspname AS schema_name,
                c.relname AS table_name,
                p.polname AS policy_name,
                p.polcmd AS command,
                p.polpermissive AS permissive,
                pg_get_expr(p.polqual, p.polrelid, true) AS using_expression,
                pg_get_expr(p.polwithcheck, p.polrelid, true) AS with_check_expression,
                ARRAY(
                    SELECT r.rolname FROM pg_roles r
                    WHERE r.oid = ANY(p.polroles)
                ) AS roles
            FROM pg_policy p
            JOIN pg_class c ON p.polrelid = c.oid
            JOIN pg_namespace n ON c.relnamespace = n.oid
            WHERE n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
            ORDER BY n.nspname, c.relname, p.polname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                RlsPolicy policy = new RlsPolicy();
                policy.setSchemaName(rs.getString("schema_name"));
                policy.setTableName(rs.getString("table_name"));
                policy.setPolicyName(rs.getString("policy_name"));
                policy.setCommand(RlsPolicy.Command.fromString(rs.getString("command")));
                policy.setPermissive(rs.getBoolean("permissive"));
                policy.setUsingExpression(rs.getString("using_expression"));
                policy.setWithCheckExpression(rs.getString("with_check_expression"));

                java.sql.Array rolesArray = rs.getArray("roles");
                if (rolesArray != null) {
                    String[] roles = (String[]) rolesArray.getArray();
                    policy.setRoles(java.util.Arrays.asList(roles));
                }

                policies.add(policy);
            }

        } catch (SQLException e) {
            LOG.errorf("Error fetching RLS policies for instance %s: %s", instanceName, e.getMessage());
        }

        return policies;
    }

    /**
     * Retrieves tables that have RLS enabled.
     *
     * @param instanceName the database instance identifier
     * @return list of table names with RLS enabled
     */
    public List<String> getTablesWithRls(String instanceName) {
        List<String> tables = new ArrayList<>();

        String sql = """
            SELECT
                n.nspname || '.' || c.relname AS table_name
            FROM pg_class c
            JOIN pg_namespace n ON c.relnamespace = n.oid
            WHERE c.relkind IN ('r', 'p')
              AND c.relrowsecurity = true
              AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
            ORDER BY n.nspname, c.relname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tables.add(rs.getString("table_name"));
            }

        } catch (SQLException e) {
            LOG.errorf("Error fetching RLS-enabled tables for instance %s: %s", instanceName, e.getMessage());
        }

        return tables;
    }

    /**
     * Retrieves sensitive tables that lack RLS protection.
     *
     * @param instanceName the database instance identifier
     * @return list of SensitiveTable objects without RLS
     */
    public List<SensitiveTable> getTablesWithoutRls(String instanceName) {
        return detectSensitiveTables(instanceName).stream()
                .filter(SensitiveTable::needsRlsProtection)
                .toList();
    }

    /**
     * Generates a summary of data access patterns.
     *
     * @param instanceName the database instance identifier
     * @return map containing summary statistics
     */
    public Map<String, Object> getSummary(String instanceName) {
        Map<String, Object> summary = new HashMap<>();

        List<PiiColumnIndicator> piiColumns = detectPiiColumns(instanceName);
        List<SensitiveTable> sensitiveTables = detectSensitiveTables(instanceName);
        List<RlsPolicy> policies = getRlsPolicies(instanceName);
        List<String> tablesWithRls = getTablesWithRls(instanceName);

        summary.put("piiColumnCount", piiColumns.size());
        summary.put("sensitiveTableCount", sensitiveTables.size());
        summary.put("rlsPolicyCount", policies.size());
        summary.put("tablesWithRlsCount", tablesWithRls.size());

        // Count by sensitivity level
        long highSensitivity = sensitiveTables.stream()
                .filter(t -> t.getSensitivityLevel() == SensitiveTable.SensitivityLevel.HIGH)
                .count();
        long mediumSensitivity = sensitiveTables.stream()
                .filter(t -> t.getSensitivityLevel() == SensitiveTable.SensitivityLevel.MEDIUM)
                .count();
        long lowSensitivity = sensitiveTables.stream()
                .filter(t -> t.getSensitivityLevel() == SensitiveTable.SensitivityLevel.LOW)
                .count();

        summary.put("highSensitivityCount", highSensitivity);
        summary.put("mediumSensitivityCount", mediumSensitivity);
        summary.put("lowSensitivityCount", lowSensitivity);

        // Tables needing RLS
        long needsRls = sensitiveTables.stream()
                .filter(SensitiveTable::needsRlsProtection)
                .count();
        summary.put("tablesNeedingRls", needsRls);

        // Group PII by type
        Map<String, Long> byType = new HashMap<>();
        for (PiiColumnIndicator col : piiColumns) {
            byType.merge(col.getPiiType().getDisplayName(), 1L, Long::sum);
        }
        summary.put("piiByType", byType);

        return summary;
    }

    /**
     * Gets sensitive tables (alias for detectSensitiveTables).
     *
     * @param instanceName the database instance identifier
     * @return list of sensitive tables
     */
    public List<SensitiveTable> getSensitiveTables(String instanceName) {
        return detectSensitiveTables(instanceName);
    }

    /**
     * Gets PII columns (alias for detectPiiColumns).
     *
     * @param instanceName the database instance identifier
     * @return list of PII column indicators
     */
    public List<PiiColumnIndicator> getPiiColumns(String instanceName) {
        return detectPiiColumns(instanceName);
    }
}
