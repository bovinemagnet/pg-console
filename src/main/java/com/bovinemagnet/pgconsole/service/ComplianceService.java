package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.ComplianceScore;
import com.bovinemagnet.pgconsole.model.RoleInfo;
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

/**
 * Service for assessing compliance metrics against security frameworks.
 * <p>
 * Evaluates database configuration against SOC 2, GDPR, and general
 * security best practices.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class ComplianceService {

    private static final Logger LOG = Logger.getLogger(ComplianceService.class);

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    SecurityAuditService securityAuditService;

    @Inject
    ConnectionSecurityService connectionSecurityService;

    @Inject
    DataAccessPatternService dataAccessPatternService;

    /**
     * Retrieves all compliance scores across all areas.
     *
     * @param instanceName the database instance identifier
     * @return list of ComplianceScore objects
     */
    public List<ComplianceScore> getAllComplianceScores(String instanceName) {
        List<ComplianceScore> scores = new ArrayList<>();
        scores.add(getAccessControlScore(instanceName));
        scores.add(getEncryptionScore(instanceName));
        scores.add(getAuditLoggingScore(instanceName));
        scores.add(getDataProtectionScore(instanceName));
        scores.add(getAuthenticationScore(instanceName));
        return scores;
    }

    /**
     * Calculates the access control compliance score.
     *
     * @param instanceName the database instance identifier
     * @return ComplianceScore for access control
     */
    public ComplianceScore getAccessControlScore(String instanceName) {
        ComplianceScore score = new ComplianceScore();
        score.setArea(ComplianceScore.ComplianceArea.ACCESS_CONTROL);
        score.setDescription("Evaluation of role-based access control implementation");
        score.setMaxScore(10);

        int points = 0;
        List<RoleInfo> roles = securityAuditService.getAllRoles(instanceName);
        long superuserCount = roles.stream().filter(RoleInfo::isSuperuser).count();
        long loginRoles = roles.stream().filter(RoleInfo::isCanLogin).count();

        // Check: Limited superuser accounts (2 points)
        if (superuserCount <= 2) {
            points += 2;
            score.addPassedCheck("Limited superuser accounts (" + superuserCount + ")");
        } else {
            score.addFailedCheck("Too many superuser accounts (" + superuserCount + ")");
            score.addRecommendation("Reduce number of superuser accounts to 2 or fewer");
        }

        // Check: Separate login roles exist (2 points)
        if (loginRoles > superuserCount) {
            points += 2;
            score.addPassedCheck("Non-superuser login roles configured");
        } else {
            score.addFailedCheck("No non-superuser login roles");
            score.addRecommendation("Create dedicated login roles with minimal privileges");
        }

        // Check: Role hierarchy in use (2 points)
        var memberships = securityAuditService.getRoleHierarchy(instanceName);
        if (!memberships.isEmpty()) {
            points += 2;
            score.addPassedCheck("Role hierarchy implemented");
        } else {
            score.addFailedCheck("No role hierarchy configured");
            score.addRecommendation("Implement role-based access control with group roles");
        }

        // Check: No superuser login roles with no password expiry (2 points)
        long superusersNoExpiry = roles.stream()
                .filter(r -> r.isSuperuser() && r.isCanLogin() && r.getValidUntil() == null)
                .count();
        if (superusersNoExpiry == 0) {
            points += 2;
            score.addPassedCheck("All superuser passwords have expiration");
        } else {
            score.addFailedCheck(superusersNoExpiry + " superuser(s) without password expiration");
            score.addRecommendation("Set password expiration for all superuser accounts");
        }

        // Check: BYPASSRLS not granted unnecessarily (2 points)
        long bypassRlsCount = roles.stream()
                .filter(r -> r.isBypassRls() && !r.isSuperuser())
                .count();
        if (bypassRlsCount == 0) {
            points += 2;
            score.addPassedCheck("BYPASSRLS not granted to non-superusers");
        } else {
            score.addFailedCheck(bypassRlsCount + " non-superuser role(s) with BYPASSRLS");
            score.addRecommendation("Remove BYPASSRLS from non-superuser roles");
        }

        score.setScore(points);
        return score;
    }

    /**
     * Calculates the encryption compliance score.
     *
     * @param instanceName the database instance identifier
     * @return ComplianceScore for encryption
     */
    public ComplianceScore getEncryptionScore(String instanceName) {
        ComplianceScore score = new ComplianceScore();
        score.setArea(ComplianceScore.ComplianceArea.ENCRYPTION);
        score.setDescription("Evaluation of data encryption in transit and at rest");
        score.setMaxScore(10);

        int points = 0;

        // Check: SSL enabled (3 points)
        boolean sslEnabled = connectionSecurityService.isSslEnabled(instanceName);
        if (sslEnabled) {
            points += 3;
            score.addPassedCheck("SSL/TLS enabled on server");
        } else {
            score.addFailedCheck("SSL/TLS not enabled");
            score.addRecommendation("Enable SSL in postgresql.conf");
        }

        // Check: Password encryption method (3 points)
        String passwordEncryption = connectionSecurityService.getPasswordEncryption(instanceName);
        if ("scram-sha-256".equalsIgnoreCase(passwordEncryption)) {
            points += 3;
            score.addPassedCheck("Using scram-sha-256 password encryption");
        } else if ("md5".equalsIgnoreCase(passwordEncryption)) {
            points += 1;
            score.addFailedCheck("Using deprecated MD5 password encryption");
            score.addRecommendation("Upgrade to scram-sha-256 password encryption");
        } else {
            score.addFailedCheck("Unknown password encryption method: " + passwordEncryption);
        }

        // Check: SSL connection percentage (4 points)
        Map<String, Object> sslSummary = connectionSecurityService.getSslSummary(instanceName);
        Double sslPercentage = (Double) sslSummary.get("sslPercentage");
        if (sslPercentage != null) {
            if (sslPercentage >= 100) {
                points += 4;
                score.addPassedCheck("100% of connections use SSL");
            } else if (sslPercentage >= 90) {
                points += 3;
                score.addPassedCheck(String.format("%.1f%% of connections use SSL", sslPercentage));
                score.addRecommendation("Enforce SSL for remaining connections");
            } else if (sslPercentage >= 50) {
                points += 1;
                score.addFailedCheck(String.format("Only %.1f%% of connections use SSL", sslPercentage));
                score.addRecommendation("Configure pg_hba.conf to require SSL");
            } else {
                score.addFailedCheck(String.format("Only %.1f%% of connections use SSL", sslPercentage));
                score.addRecommendation("Enable and enforce SSL for all connections");
            }
        }

        score.setScore(points);
        return score;
    }

    /**
     * Calculates the audit logging compliance score.
     *
     * @param instanceName the database instance identifier
     * @return ComplianceScore for audit logging
     */
    public ComplianceScore getAuditLoggingScore(String instanceName) {
        ComplianceScore score = new ComplianceScore();
        score.setArea(ComplianceScore.ComplianceArea.AUDIT_LOGGING);
        score.setDescription("Evaluation of logging and audit trail capabilities");
        score.setMaxScore(10);

        int points = 0;
        Map<String, String> settings = getAuditSettings(instanceName);

        // Check: pgaudit extension (3 points)
        boolean pgauditInstalled = isExtensionInstalled(instanceName, "pgaudit");
        if (pgauditInstalled) {
            points += 3;
            score.addPassedCheck("pgaudit extension installed");
        } else {
            score.addFailedCheck("pgaudit extension not installed");
            score.addRecommendation("Install pgaudit for comprehensive audit logging");
        }

        // Check: log_connections enabled (2 points)
        if ("on".equalsIgnoreCase(settings.get("log_connections"))) {
            points += 2;
            score.addPassedCheck("Connection logging enabled");
        } else {
            score.addFailedCheck("Connection logging disabled");
            score.addRecommendation("Enable log_connections for connection tracking");
        }

        // Check: log_disconnections enabled (1 point)
        if ("on".equalsIgnoreCase(settings.get("log_disconnections"))) {
            points += 1;
            score.addPassedCheck("Disconnection logging enabled");
        } else {
            score.addFailedCheck("Disconnection logging disabled");
            score.addRecommendation("Enable log_disconnections");
        }

        // Check: log_statement setting (2 points)
        String logStatement = settings.get("log_statement");
        if ("all".equalsIgnoreCase(logStatement)) {
            points += 2;
            score.addPassedCheck("All statements logged");
        } else if ("ddl".equalsIgnoreCase(logStatement) || "mod".equalsIgnoreCase(logStatement)) {
            points += 1;
            score.addPassedCheck("DDL/DML statements logged (log_statement=" + logStatement + ")");
            score.addRecommendation("Consider setting log_statement=all for comprehensive logging");
        } else {
            score.addFailedCheck("Statement logging disabled or minimal");
            score.addRecommendation("Enable log_statement (at least 'ddl' or 'mod')");
        }

        // Check: log_checkpoints enabled (1 point)
        if ("on".equalsIgnoreCase(settings.get("log_checkpoints"))) {
            points += 1;
            score.addPassedCheck("Checkpoint logging enabled");
        } else {
            score.addFailedCheck("Checkpoint logging disabled");
        }

        // Check: log_lock_waits enabled (1 point)
        if ("on".equalsIgnoreCase(settings.get("log_lock_waits"))) {
            points += 1;
            score.addPassedCheck("Lock wait logging enabled");
        } else {
            score.addFailedCheck("Lock wait logging disabled");
        }

        score.setScore(points);
        return score;
    }

    /**
     * Calculates the data protection compliance score.
     *
     * @param instanceName the database instance identifier
     * @return ComplianceScore for data protection
     */
    public ComplianceScore getDataProtectionScore(String instanceName) {
        ComplianceScore score = new ComplianceScore();
        score.setArea(ComplianceScore.ComplianceArea.DATA_PROTECTION);
        score.setDescription("Evaluation of sensitive data protection measures");
        score.setMaxScore(10);

        int points = 0;
        Map<String, Object> accessSummary = dataAccessPatternService.getSummary(instanceName);

        // Check: RLS enabled for sensitive tables (4 points)
        Long tablesNeedingRls = (Long) accessSummary.get("tablesNeedingRls");
        Long sensitiveTableCount = (Long) accessSummary.get("sensitiveTableCount");
        if (tablesNeedingRls != null && sensitiveTableCount != null) {
            if (tablesNeedingRls == 0) {
                points += 4;
                score.addPassedCheck("All sensitive tables have RLS protection");
            } else if (tablesNeedingRls < sensitiveTableCount / 2) {
                points += 2;
                score.addFailedCheck(tablesNeedingRls + " sensitive table(s) without RLS");
                score.addRecommendation("Enable RLS for sensitive tables containing PII");
            } else {
                score.addFailedCheck(tablesNeedingRls + " sensitive table(s) without RLS");
                score.addRecommendation("Implement row-level security for data access control");
            }
        }

        // Check: RLS policies exist (3 points)
        Long policyCount = (Long) accessSummary.get("rlsPolicyCount");
        if (policyCount != null && policyCount > 0) {
            points += 3;
            score.addPassedCheck(policyCount + " RLS policies configured");
        } else {
            score.addFailedCheck("No RLS policies configured");
            score.addRecommendation("Create RLS policies to control data access");
        }

        // Check: PII awareness (3 points)
        Long piiColumnCount = (Long) accessSummary.get("piiColumnCount");
        Long highSensitivity = (Long) accessSummary.get("highSensitivityCount");
        if (piiColumnCount != null && piiColumnCount > 0) {
            points += 2;
            score.addPassedCheck(piiColumnCount + " potential PII columns identified");
            if (highSensitivity != null && highSensitivity > 0) {
                score.addRecommendation("Review " + highSensitivity + " high-sensitivity table(s)");
            }
        } else {
            points += 3;
            score.addPassedCheck("No obvious PII columns detected");
        }

        score.setScore(points);
        return score;
    }

    /**
     * Calculates the authentication compliance score.
     *
     * @param instanceName the database instance identifier
     * @return ComplianceScore for authentication
     */
    public ComplianceScore getAuthenticationScore(String instanceName) {
        ComplianceScore score = new ComplianceScore();
        score.setArea(ComplianceScore.ComplianceArea.AUTHENTICATION);
        score.setDescription("Evaluation of authentication mechanisms and policies");
        score.setMaxScore(10);

        int points = 0;
        var authMethods = connectionSecurityService.getAuthMethodBreakdown(instanceName);

        // Check: No trust authentication (3 points)
        if (!authMethods.containsKey("trust") || authMethods.get("trust") == 0) {
            points += 3;
            score.addPassedCheck("Trust authentication not in use");
        } else {
            score.addFailedCheck("Trust authentication configured (" + authMethods.get("trust") + " rules)");
            score.addRecommendation("Remove trust authentication from pg_hba.conf");
        }

        // Check: No password auth (plain text) (2 points)
        if (!authMethods.containsKey("password") || authMethods.get("password") == 0) {
            points += 2;
            score.addPassedCheck("Plain password authentication not in use");
        } else {
            score.addFailedCheck("Plain password authentication configured");
            score.addRecommendation("Replace password with scram-sha-256");
        }

        // Check: Scram-sha-256 in use (3 points)
        if (authMethods.containsKey("scram-sha-256") && authMethods.get("scram-sha-256") > 0) {
            points += 3;
            score.addPassedCheck("scram-sha-256 authentication configured");
        } else if (authMethods.containsKey("md5") && authMethods.get("md5") > 0) {
            points += 1;
            score.addFailedCheck("Using deprecated MD5 authentication");
            score.addRecommendation("Upgrade to scram-sha-256 authentication");
        } else {
            score.addFailedCheck("No password-based authentication configured");
        }

        // Check: Password expiry policies (2 points)
        List<RoleInfo> loginRoles = securityAuditService.getLoginRoles(instanceName);
        long rolesWithExpiry = loginRoles.stream()
                .filter(r -> r.getValidUntil() != null)
                .count();
        double expiryPercentage = loginRoles.isEmpty() ? 100 :
                (rolesWithExpiry * 100.0 / loginRoles.size());

        if (expiryPercentage >= 80) {
            points += 2;
            score.addPassedCheck(String.format("%.0f%% of login roles have password expiry", expiryPercentage));
        } else if (expiryPercentage >= 50) {
            points += 1;
            score.addFailedCheck(String.format("Only %.0f%% of login roles have password expiry", expiryPercentage));
            score.addRecommendation("Set password expiration for all login roles");
        } else {
            score.addFailedCheck("Most login roles lack password expiration");
            score.addRecommendation("Implement password expiration policy");
        }

        score.setScore(points);
        return score;
    }

    /**
     * Calculates the overall compliance score.
     *
     * @param instanceName the database instance identifier
     * @return overall percentage score
     */
    public double getOverallScore(String instanceName) {
        List<ComplianceScore> scores = getAllComplianceScores(instanceName);
        int totalScore = scores.stream().mapToInt(ComplianceScore::getScore).sum();
        int maxScore = scores.stream().mapToInt(ComplianceScore::getMaxScore).sum();
        return maxScore > 0 ? (totalScore * 100.0 / maxScore) : 0;
    }

    /**
     * Generates a summary of compliance findings.
     *
     * @param instanceName the database instance identifier
     * @return map containing summary statistics
     */
    public Map<String, Object> getSummary(String instanceName) {
        Map<String, Object> summary = new HashMap<>();

        List<ComplianceScore> scores = getAllComplianceScores(instanceName);
        int totalScore = scores.stream().mapToInt(ComplianceScore::getScore).sum();
        int maxScore = scores.stream().mapToInt(ComplianceScore::getMaxScore).sum();

        summary.put("overallScore", totalScore);
        summary.put("maxScore", maxScore);
        summary.put("overallPercentage", maxScore > 0 ? (totalScore * 100.0 / maxScore) : 0);
        summary.put("overallGrade", getOverallGrade(instanceName));
        summary.put("scores", scores);

        // Count passed/failed checks
        int passedChecks = scores.stream().mapToInt(s -> s.getPassedChecks().size()).sum();
        int failedChecks = scores.stream().mapToInt(s -> s.getFailedChecks().size()).sum();
        int totalRecommendations = scores.stream().mapToInt(s -> s.getRecommendations().size()).sum();

        summary.put("passedChecks", passedChecks);
        summary.put("failedChecks", failedChecks);
        summary.put("totalRecommendations", totalRecommendations);

        return summary;
    }

    /**
     * Returns the overall letter grade.
     *
     * @param instanceName the database instance identifier
     * @return letter grade (A-F)
     */
    public String getOverallGrade(String instanceName) {
        double pct = getOverallScore(instanceName);
        if (pct >= 90) return "A";
        if (pct >= 80) return "B";
        if (pct >= 70) return "C";
        if (pct >= 60) return "D";
        return "F";
    }

    private Map<String, String> getAuditSettings(String instanceName) {
        Map<String, String> settings = new HashMap<>();

        String sql = """
            SELECT name, setting
            FROM pg_settings
            WHERE name IN (
                'log_connections', 'log_disconnections', 'log_statement',
                'log_checkpoints', 'log_lock_waits', 'log_duration'
            )
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                settings.put(rs.getString("name"), rs.getString("setting"));
            }

        } catch (SQLException e) {
            LOG.errorf("Error fetching audit settings for instance %s: %s", instanceName, e.getMessage());
        }

        return settings;
    }

    private boolean isExtensionInstalled(String instanceName, String extensionName) {
        String sql = "SELECT COUNT(*) FROM pg_extension WHERE extname = '" + extensionName + "'";

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            LOG.errorf("Error checking extension %s for instance %s: %s",
                    extensionName, instanceName, e.getMessage());
        }

        return false;
    }
}
