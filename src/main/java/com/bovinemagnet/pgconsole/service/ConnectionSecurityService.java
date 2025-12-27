package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.HbaRule;
import com.bovinemagnet.pgconsole.model.SecurityWarning;
import com.bovinemagnet.pgconsole.model.SslConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for analysing connection security in PostgreSQL.
 * <p>
 * Queries pg_stat_ssl, pg_stat_activity, and pg_hba_file_rules
 * to analyse SSL/TLS status and authentication configuration.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class ConnectionSecurityService {

    private static final Logger LOG = Logger.getLogger(ConnectionSecurityService.class);

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Retrieves SSL/TLS status for all current connections.
     * <p>
     * Requires PostgreSQL 9.5+ for pg_stat_ssl.
     *
     * @param instanceName the database instance identifier
     * @return list of SslConnection objects
     */
    public List<SslConnection> getSslConnections(String instanceName) {
        List<SslConnection> connections = new ArrayList<>();

        String sql = """
            SELECT
                a.pid,
                a.usename,
                a.client_addr::text,
                a.application_name,
                a.datname,
                a.state,
                COALESCE(s.ssl, false) AS ssl,
                s.version AS ssl_version,
                s.cipher,
                COALESCE(s.bits, 0) AS bits,
                s.client_dn,
                s.issuer_dn
            FROM pg_stat_activity a
            LEFT JOIN pg_stat_ssl s ON a.pid = s.pid
            WHERE a.pid != pg_backend_pid()
              AND a.backend_type = 'client backend'
            ORDER BY a.usename, a.client_addr
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                SslConnection sslConn = new SslConnection();
                sslConn.setPid(rs.getInt("pid"));
                sslConn.setUsername(rs.getString("usename"));
                sslConn.setClientAddr(rs.getString("client_addr"));
                sslConn.setApplicationName(rs.getString("application_name"));
                sslConn.setDatabase(rs.getString("datname"));
                sslConn.setState(rs.getString("state"));
                sslConn.setSsl(rs.getBoolean("ssl"));
                sslConn.setSslVersion(rs.getString("ssl_version"));
                sslConn.setCipher(rs.getString("cipher"));
                sslConn.setBits(rs.getInt("bits"));
                sslConn.setClientDn(rs.getString("client_dn"));
                sslConn.setIssuerDn(rs.getString("issuer_dn"));
                connections.add(sslConn);
            }

        } catch (SQLException e) {
            LOG.errorf("Error fetching SSL connections for instance %s: %s", instanceName, e.getMessage());
        }

        return connections;
    }

    /**
     * Retrieves a summary of SSL connection statistics.
     *
     * @param instanceName the database instance identifier
     * @return map containing SSL summary statistics
     */
    public Map<String, Object> getSslSummary(String instanceName) {
        Map<String, Object> summary = new HashMap<>();

        List<SslConnection> connections = getSslConnections(instanceName);
        long total = connections.size();
        long sslEnabled = connections.stream().filter(SslConnection::isSsl).count();
        long sslDisabled = total - sslEnabled;

        summary.put("totalConnections", total);
        summary.put("sslEnabled", sslEnabled);
        summary.put("sslDisabled", sslDisabled);
        summary.put("sslPercentage", total > 0 ? (sslEnabled * 100.0 / total) : 0.0);

        // Group by SSL version
        Map<String, Long> byVersion = new HashMap<>();
        for (SslConnection conn : connections) {
            if (conn.isSsl() && conn.getSslVersion() != null) {
                byVersion.merge(conn.getSslVersion(), 1L, Long::sum);
            }
        }
        summary.put("byVersion", byVersion);

        // Count by source type
        long localConnections = connections.stream()
                .filter(c -> "Local".equals(c.getConnectionSource()))
                .count();
        long internalConnections = connections.stream()
                .filter(c -> "Internal".equals(c.getConnectionSource()))
                .count();
        long externalConnections = connections.stream()
                .filter(c -> "External".equals(c.getConnectionSource()))
                .count();

        summary.put("localConnections", localConnections);
        summary.put("internalConnections", internalConnections);
        summary.put("externalConnections", externalConnections);

        return summary;
    }

    /**
     * Retrieves HBA rules from pg_hba_file_rules (PostgreSQL 10+).
     *
     * @param instanceName the database instance identifier
     * @return list of HbaRule objects, or empty if not supported
     */
    public List<HbaRule> getHbaRules(String instanceName) {
        List<HbaRule> rules = new ArrayList<>();

        // Check if pg_hba_file_rules exists (PostgreSQL 10+)
        String checkSql = """
            SELECT EXISTS (
                SELECT 1 FROM pg_catalog.pg_class c
                JOIN pg_catalog.pg_namespace n ON c.relnamespace = n.oid
                WHERE n.nspname = 'pg_catalog'
                  AND c.relname = 'pg_hba_file_rules'
            )
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {

            if (rs.next() && !rs.getBoolean(1)) {
                LOG.info("pg_hba_file_rules not available (requires PostgreSQL 10+)");
                return rules;
            }

        } catch (SQLException e) {
            LOG.debugf("Error checking for pg_hba_file_rules: %s", e.getMessage());
            return rules;
        }

        String sql = """
            SELECT
                line_number,
                type,
                database,
                user_name,
                address,
                netmask,
                auth_method,
                options,
                error
            FROM pg_hba_file_rules
            ORDER BY line_number
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                HbaRule rule = new HbaRule();
                rule.setLineNumber(rs.getInt("line_number"));
                rule.setType(rs.getString("type"));

                // Parse database array
                java.sql.Array dbArray = rs.getArray("database");
                if (dbArray != null) {
                    String[] dbs = (String[]) dbArray.getArray();
                    rule.setDatabases(Arrays.asList(dbs));
                }

                // Parse user_name array
                java.sql.Array userArray = rs.getArray("user_name");
                if (userArray != null) {
                    String[] users = (String[]) userArray.getArray();
                    rule.setUsers(Arrays.asList(users));
                }

                rule.setAddress(rs.getString("address"));
                rule.setNetmask(rs.getString("netmask"));
                rule.setAuthMethod(rs.getString("auth_method"));

                // Parse options array
                java.sql.Array optArray = rs.getArray("options");
                if (optArray != null) {
                    String[] opts = (String[]) optArray.getArray();
                    rule.setOptions(Arrays.asList(opts));
                }

                rule.setError(rs.getString("error"));
                rules.add(rule);
            }

        } catch (SQLException e) {
            LOG.errorf("Error fetching HBA rules for instance %s: %s", instanceName, e.getMessage());
        }

        return rules;
    }

    /**
     * Retrieves authentication method breakdown from HBA rules.
     *
     * @param instanceName the database instance identifier
     * @return map of auth method to rule count
     */
    public Map<String, Long> getAuthMethodBreakdown(String instanceName) {
        Map<String, Long> breakdown = new HashMap<>();
        List<HbaRule> rules = getHbaRules(instanceName);

        for (HbaRule rule : rules) {
            if (rule.getAuthMethod() != null && !rule.hasError()) {
                breakdown.merge(rule.getAuthMethod(), 1L, Long::sum);
            }
        }

        return breakdown;
    }

    /**
     * Retrieves connection counts grouped by source IP.
     *
     * @param instanceName the database instance identifier
     * @return map of client address to connection count
     */
    public Map<String, Long> getConnectionsBySourceIp(String instanceName) {
        Map<String, Long> byIp = new HashMap<>();

        String sql = """
            SELECT
                COALESCE(client_addr::text, 'local') AS client_addr,
                COUNT(*) AS connection_count
            FROM pg_stat_activity
            WHERE pid != pg_backend_pid()
              AND backend_type = 'client backend'
            GROUP BY client_addr
            ORDER BY connection_count DESC
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                byIp.put(rs.getString("client_addr"), rs.getLong("connection_count"));
            }

        } catch (SQLException e) {
            LOG.errorf("Error fetching connections by IP for instance %s: %s", instanceName, e.getMessage());
        }

        return byIp;
    }

    /**
     * Retrieves external (non-private) connections.
     *
     * @param instanceName the database instance identifier
     * @return list of external SslConnection objects
     */
    public List<SslConnection> getExternalConnections(String instanceName) {
        return getSslConnections(instanceName).stream()
                .filter(c -> "External".equals(c.getConnectionSource()))
                .toList();
    }

    /**
     * Generates security warnings for connection issues.
     *
     * @param instanceName the database instance identifier
     * @return list of SecurityWarning objects
     */
    public List<SecurityWarning> getConnectionWarnings(String instanceName) {
        List<SecurityWarning> warnings = new ArrayList<>();

        // Check for non-SSL connections
        List<SslConnection> connections = getSslConnections(instanceName);
        for (SslConnection conn : connections) {
            if (!conn.isSsl() && !"Local".equals(conn.getConnectionSource())) {
                warnings.add(SecurityWarning.builder()
                        .type(SecurityWarning.WarningType.NO_SSL_CONNECTION)
                        .severity("External".equals(conn.getConnectionSource())
                                ? SecurityWarning.Severity.HIGH
                                : SecurityWarning.Severity.MEDIUM)
                        .subject(conn.getUsername() + "@" + conn.getClientAddrDisplay())
                        .description("Connection from " + conn.getClientAddrDisplay() +
                                " is not using SSL encryption")
                        .recommendation("Configure SSL for all non-local connections")
                        .detail("Database", conn.getDatabase())
                        .detail("Application", conn.getApplicationName())
                        .build());
            }
        }

        // Check HBA rules for insecure auth methods
        List<HbaRule> rules = getHbaRules(instanceName);
        for (HbaRule rule : rules) {
            if (rule.isInsecure() && !rule.hasError()) {
                SecurityWarning.Severity severity = rule.isWideOpen()
                        ? SecurityWarning.Severity.CRITICAL
                        : SecurityWarning.Severity.HIGH;

                warnings.add(SecurityWarning.builder()
                        .type(rule.getAuthMethod().equalsIgnoreCase("trust")
                                ? SecurityWarning.WarningType.TRUST_AUTHENTICATION
                                : SecurityWarning.WarningType.WEAK_AUTH_METHOD)
                        .severity(severity)
                        .subject("pg_hba.conf line " + rule.getLineNumber())
                        .description("HBA rule uses insecure authentication method: " +
                                rule.getAuthMethod())
                        .recommendation("Replace '" + rule.getAuthMethod() +
                                "' with scram-sha-256 or a more secure method")
                        .detail("Type", rule.getType())
                        .detail("Databases", rule.getDatabasesDisplay())
                        .detail("Users", rule.getUsersDisplay())
                        .detail("Address", rule.getAddressDisplay())
                        .build());
            }

            // Check for MD5 (deprecated)
            if ("md5".equalsIgnoreCase(rule.getAuthMethod()) && !rule.hasError()) {
                warnings.add(SecurityWarning.builder()
                        .type(SecurityWarning.WarningType.MD5_PASSWORD)
                        .severity(SecurityWarning.Severity.MEDIUM)
                        .subject("pg_hba.conf line " + rule.getLineNumber())
                        .description("HBA rule uses deprecated MD5 password authentication")
                        .recommendation("Upgrade to scram-sha-256 for improved security")
                        .detail("Address", rule.getAddressDisplay())
                        .build());
            }
        }

        return warnings;
    }

    /**
     * Generates a summary of connection security findings.
     *
     * @param instanceName the database instance identifier
     * @return map containing summary statistics
     */
    public Map<String, Object> getSummary(String instanceName) {
        Map<String, Object> summary = new HashMap<>();

        Map<String, Object> sslSummary = getSslSummary(instanceName);
        summary.putAll(sslSummary);

        List<HbaRule> rules = getHbaRules(instanceName);
        summary.put("hbaRuleCount", rules.size());
        summary.put("hbaAvailable", !rules.isEmpty());

        long insecureRules = rules.stream()
                .filter(HbaRule::isInsecure)
                .filter(r -> !r.hasError())
                .count();
        summary.put("insecureRules", insecureRules);

        Map<String, Long> authMethods = getAuthMethodBreakdown(instanceName);
        summary.put("authMethods", authMethods);

        List<SecurityWarning> warnings = getConnectionWarnings(instanceName);
        summary.put("warningCount", warnings.size());
        summary.put("criticalWarnings", warnings.stream()
                .filter(w -> w.getSeverity() == SecurityWarning.Severity.CRITICAL).count());

        return summary;
    }

    /**
     * Checks if SSL is enabled on the server.
     *
     * @param instanceName the database instance identifier
     * @return true if SSL is enabled
     */
    public boolean isSslEnabled(String instanceName) {
        String sql = "SHOW ssl";

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return "on".equalsIgnoreCase(rs.getString(1));
            }

        } catch (SQLException e) {
            LOG.errorf("Error checking SSL status for instance %s: %s", instanceName, e.getMessage());
        }

        return false;
    }

    /**
     * Retrieves password encryption method setting.
     *
     * @param instanceName the database instance identifier
     * @return password encryption method (md5, scram-sha-256)
     */
    public String getPasswordEncryption(String instanceName) {
        String sql = "SHOW password_encryption";

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getString(1);
            }

        } catch (SQLException e) {
            LOG.errorf("Error checking password encryption for instance %s: %s", instanceName, e.getMessage());
        }

        return "unknown";
    }

    /**
     * Gets connection security warnings (alias for getConnectionWarnings).
     *
     * @param instanceName the database instance identifier
     * @return list of connection security warnings
     */
    public List<SecurityWarning> getWarnings(String instanceName) {
        return getConnectionWarnings(instanceName);
    }
}
