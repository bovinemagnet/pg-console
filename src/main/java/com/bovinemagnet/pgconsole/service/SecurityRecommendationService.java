package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.HbaRule;
import com.bovinemagnet.pgconsole.model.RoleInfo;
import com.bovinemagnet.pgconsole.model.SecurityRecommendation;
import com.bovinemagnet.pgconsole.model.SensitiveTable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating security recommendations based on database analysis.
 * <p>
 * Analyses roles, authentication, encryption, extensions, and schema
 * configuration to provide actionable security improvements.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class SecurityRecommendationService {

    private static final Logger LOG = Logger.getLogger(SecurityRecommendationService.class);

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    SecurityAuditService securityAuditService;

    @Inject
    ConnectionSecurityService connectionSecurityService;

    @Inject
    DataAccessPatternService dataAccessPatternService;

    // Extensions considered dangerous or requiring careful review
    private static final List<String> DANGEROUS_EXTENSIONS = List.of(
            "adminpack", "file_fdw", "postgres_fdw", "dblink",
            "plpythonu", "plperlu", "pltclu"
    );

    /**
     * Retrieves all security recommendations across all categories.
     *
     * @param instanceName the database instance identifier
     * @return list of SecurityRecommendation objects sorted by priority
     */
    public List<SecurityRecommendation> getAllRecommendations(String instanceName) {
        List<SecurityRecommendation> recommendations = new ArrayList<>();
        recommendations.addAll(getRoleRecommendations(instanceName));
        recommendations.addAll(getConnectionRecommendations(instanceName));
        recommendations.addAll(getEncryptionRecommendations(instanceName));
        recommendations.addAll(getExtensionRecommendations(instanceName));
        recommendations.addAll(getSchemaRecommendations(instanceName));
        recommendations.addAll(getHbaRecommendations(instanceName));
        recommendations.addAll(getLoggingRecommendations(instanceName));
        recommendations.addAll(getAccessControlRecommendations(instanceName));

        // Sort by priority
        recommendations.sort(Comparator.comparingInt(r ->
                r.getPriority() != null ? -r.getPriority().getLevel() : 0));

        return recommendations;
    }

    /**
     * Generates recommendations related to role configuration.
     *
     * @param instanceName the database instance identifier
     * @return list of role-related recommendations
     */
    public List<SecurityRecommendation> getRoleRecommendations(String instanceName) {
        List<SecurityRecommendation> recommendations = new ArrayList<>();
        List<RoleInfo> roles = securityAuditService.getAllRoles(instanceName);

        // Check for too many superusers
        long superuserCount = roles.stream().filter(RoleInfo::isSuperuser).count();
        if (superuserCount > 2) {
            recommendations.add(SecurityRecommendation.builder()
                    .category(SecurityRecommendation.Category.ROLES)
                    .priority(SecurityRecommendation.Priority.HIGH)
                    .title("Excessive Superuser Accounts")
                    .description("There are " + superuserCount + " superuser accounts configured")
                    .rationale("Each superuser account increases attack surface and risk of privilege escalation")
                    .suggestedAction("Review and remove unnecessary superuser accounts; use least-privilege roles")
                    .currentValue(superuserCount + " superusers")
                    .recommendedValue("2 or fewer superusers")
                    .build());
        }

        // Check for superuser application roles
        for (RoleInfo role : roles) {
            if (role.isSuperuser() && role.isCanLogin() &&
                    (role.getRoleName().contains("app") || role.getRoleName().contains("api"))) {
                recommendations.add(SecurityRecommendation.builder()
                        .category(SecurityRecommendation.Category.ROLES)
                        .priority(SecurityRecommendation.Priority.CRITICAL)
                        .title("Superuser Application Role")
                        .description("Role '" + role.getRoleName() + "' appears to be an application role with superuser privileges")
                        .rationale("Application connections should never use superuser privileges")
                        .suggestedAction("Create a dedicated role with minimal required privileges for the application")
                        .affectedObject(role.getRoleName())
                        .build());
            }

            // Expired passwords
            if (role.isCanLogin() && role.isPasswordExpired()) {
                recommendations.add(SecurityRecommendation.builder()
                        .category(SecurityRecommendation.Category.ROLES)
                        .priority(SecurityRecommendation.Priority.HIGH)
                        .title("Expired Password")
                        .description("Role '" + role.getRoleName() + "' has an expired password")
                        .rationale("Expired passwords may prevent legitimate access or indicate inactive accounts")
                        .suggestedAction("Update the password or disable the role if no longer needed")
                        .affectedObject(role.getRoleName())
                        .currentValue("Expired: " + role.getValidUntil())
                        .build());
            }

            // No password expiry
            if (role.isCanLogin() && role.getValidUntil() == null && !role.isSuperuser()) {
                recommendations.add(SecurityRecommendation.builder()
                        .category(SecurityRecommendation.Category.ROLES)
                        .priority(SecurityRecommendation.Priority.LOW)
                        .title("No Password Expiration")
                        .description("Role '" + role.getRoleName() + "' has no password expiration set")
                        .rationale("Password rotation policies improve security posture")
                        .suggestedAction("Set VALID UNTIL for password expiration")
                        .affectedObject(role.getRoleName())
                        .recommendedValue("VALID UNTIL with 90-day rotation")
                        .build());
            }

            // BYPASSRLS on non-superuser
            if (role.isBypassRls() && !role.isSuperuser()) {
                recommendations.add(SecurityRecommendation.builder()
                        .category(SecurityRecommendation.Category.ROLES)
                        .priority(SecurityRecommendation.Priority.MEDIUM)
                        .title("Unnecessary BYPASSRLS Privilege")
                        .description("Role '" + role.getRoleName() + "' has BYPASSRLS but is not a superuser")
                        .rationale("BYPASSRLS allows circumventing row-level security policies")
                        .suggestedAction("Remove BYPASSRLS unless explicitly required for administrative functions")
                        .affectedObject(role.getRoleName())
                        .build());
            }
        }

        return recommendations;
    }

    /**
     * Generates recommendations related to connection security.
     *
     * @param instanceName the database instance identifier
     * @return list of connection-related recommendations
     */
    public List<SecurityRecommendation> getConnectionRecommendations(String instanceName) {
        List<SecurityRecommendation> recommendations = new ArrayList<>();

        // Check SSL status
        boolean sslEnabled = connectionSecurityService.isSslEnabled(instanceName);
        if (!sslEnabled) {
            recommendations.add(SecurityRecommendation.builder()
                    .category(SecurityRecommendation.Category.ENCRYPTION)
                    .priority(SecurityRecommendation.Priority.CRITICAL)
                    .title("SSL/TLS Not Enabled")
                    .description("SSL encryption is not enabled on the server")
                    .rationale("Without SSL, all data including passwords is transmitted in plain text")
                    .suggestedAction("Enable SSL in postgresql.conf and configure certificates")
                    .currentValue("ssl = off")
                    .recommendedValue("ssl = on")
                    .build());
        }

        // Check SSL usage percentage
        Map<String, Object> sslSummary = connectionSecurityService.getSslSummary(instanceName);
        Double sslPercentage = (Double) sslSummary.get("sslPercentage");
        Long externalConnections = (Long) sslSummary.get("externalConnections");

        if (sslPercentage != null && sslPercentage < 100 && externalConnections != null && externalConnections > 0) {
            recommendations.add(SecurityRecommendation.builder()
                    .category(SecurityRecommendation.Category.ENCRYPTION)
                    .priority(SecurityRecommendation.Priority.HIGH)
                    .title("Non-SSL External Connections")
                    .description("Not all external connections are using SSL encryption")
                    .rationale("External connections without SSL expose data to network sniffing")
                    .suggestedAction("Configure pg_hba.conf to require SSL for external connections (hostssl)")
                    .currentValue(String.format("%.1f%% SSL usage", sslPercentage))
                    .recommendedValue("100% SSL for external connections")
                    .build());
        }

        return recommendations;
    }

    /**
     * Generates recommendations related to encryption settings.
     *
     * @param instanceName the database instance identifier
     * @return list of encryption-related recommendations
     */
    public List<SecurityRecommendation> getEncryptionRecommendations(String instanceName) {
        List<SecurityRecommendation> recommendations = new ArrayList<>();

        String passwordEncryption = connectionSecurityService.getPasswordEncryption(instanceName);
        if ("md5".equalsIgnoreCase(passwordEncryption)) {
            recommendations.add(SecurityRecommendation.builder()
                    .category(SecurityRecommendation.Category.ENCRYPTION)
                    .priority(SecurityRecommendation.Priority.HIGH)
                    .title("Deprecated MD5 Password Encryption")
                    .description("Server is using deprecated MD5 for password hashing")
                    .rationale("MD5 is vulnerable to rainbow table attacks and should not be used")
                    .suggestedAction("Set password_encryption = 'scram-sha-256' and update all passwords")
                    .currentValue("password_encryption = md5")
                    .recommendedValue("password_encryption = scram-sha-256")
                    .reference("https://www.postgresql.org/docs/current/auth-password.html")
                    .build());
        }

        return recommendations;
    }

    /**
     * Generates recommendations related to installed extensions.
     *
     * @param instanceName the database instance identifier
     * @return list of extension-related recommendations
     */
    public List<SecurityRecommendation> getExtensionRecommendations(String instanceName) {
        List<SecurityRecommendation> recommendations = new ArrayList<>();
        List<String> installedExtensions = getInstalledExtensions(instanceName);

        // Check for dangerous extensions
        for (String ext : DANGEROUS_EXTENSIONS) {
            if (installedExtensions.contains(ext)) {
                SecurityRecommendation.Priority priority =
                        ext.startsWith("pl") ? SecurityRecommendation.Priority.HIGH :
                                SecurityRecommendation.Priority.MEDIUM;

                recommendations.add(SecurityRecommendation.builder()
                        .category(SecurityRecommendation.Category.EXTENSIONS)
                        .priority(priority)
                        .title("Potentially Dangerous Extension: " + ext)
                        .description("Extension '" + ext + "' provides elevated capabilities")
                        .rationale(getExtensionRationale(ext))
                        .suggestedAction("Review necessity and restrict access if required")
                        .affectedObject(ext)
                        .build());
            }
        }

        // Check for missing pgaudit
        if (!installedExtensions.contains("pgaudit")) {
            recommendations.add(SecurityRecommendation.builder()
                    .category(SecurityRecommendation.Category.LOGGING)
                    .priority(SecurityRecommendation.Priority.MEDIUM)
                    .title("pgaudit Extension Not Installed")
                    .description("The pgaudit extension is not installed for comprehensive audit logging")
                    .rationale("pgaudit provides detailed audit logging for compliance requirements")
                    .suggestedAction("Install and configure pgaudit for audit trail requirements")
                    .recommendedValue("CREATE EXTENSION pgaudit")
                    .reference("https://www.pgaudit.org/")
                    .build());
        }

        return recommendations;
    }

    private String getExtensionRationale(String ext) {
        return switch (ext) {
            case "adminpack" -> "Provides file system access functions";
            case "file_fdw" -> "Allows reading arbitrary files from the file system";
            case "postgres_fdw", "dblink" -> "Enables connections to external databases";
            case "plpythonu", "plperlu", "pltclu" -> "Untrusted language allows system command execution";
            default -> "Extension provides elevated capabilities that may be exploited";
        };
    }

    private List<String> getInstalledExtensions(String instanceName) {
        List<String> extensions = new ArrayList<>();
        String sql = "SELECT extname FROM pg_extension";

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                extensions.add(rs.getString("extname"));
            }

        } catch (SQLException e) {
            LOG.errorf("Error fetching extensions for instance %s: %s", instanceName, e.getMessage());
        }

        return extensions;
    }

    /**
     * Generates recommendations related to schema configuration.
     *
     * @param instanceName the database instance identifier
     * @return list of schema-related recommendations
     */
    public List<SecurityRecommendation> getSchemaRecommendations(String instanceName) {
        List<SecurityRecommendation> recommendations = new ArrayList<>();

        // Check public schema CREATE privilege
        boolean publicHasCreate = checkPublicSchemaCreate(instanceName);
        if (publicHasCreate) {
            recommendations.add(SecurityRecommendation.builder()
                    .category(SecurityRecommendation.Category.SCHEMA)
                    .priority(SecurityRecommendation.Priority.HIGH)
                    .title("Public Schema CREATE Privilege")
                    .description("The PUBLIC role can create objects in the public schema")
                    .rationale("This allows any user to create objects that may shadow trusted functions")
                    .suggestedAction("REVOKE CREATE ON SCHEMA public FROM PUBLIC")
                    .affectedObject("public schema")
                    .reference("CVE-2018-1058")
                    .build());
        }

        return recommendations;
    }

    private boolean checkPublicSchemaCreate(String instanceName) {
        String sql = """
            SELECT has_schema_privilege('public', 'public', 'CREATE')
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getBoolean(1);
            }

        } catch (SQLException e) {
            LOG.debugf("Error checking public schema privileges: %s", e.getMessage());
        }

        return false;
    }

    /**
     * Generates recommendations related to pg_hba.conf configuration.
     *
     * @param instanceName the database instance identifier
     * @return list of HBA-related recommendations
     */
    public List<SecurityRecommendation> getHbaRecommendations(String instanceName) {
        List<SecurityRecommendation> recommendations = new ArrayList<>();
        List<HbaRule> rules = connectionSecurityService.getHbaRules(instanceName);

        for (HbaRule rule : rules) {
            if (rule.hasError()) continue;

            // Trust authentication
            if ("trust".equalsIgnoreCase(rule.getAuthMethod())) {
                SecurityRecommendation.Priority priority = rule.isWideOpen() ?
                        SecurityRecommendation.Priority.CRITICAL :
                        SecurityRecommendation.Priority.HIGH;

                recommendations.add(SecurityRecommendation.builder()
                        .category(SecurityRecommendation.Category.HBA)
                        .priority(priority)
                        .title("Trust Authentication Configured")
                        .description("pg_hba.conf line " + rule.getLineNumber() + " uses trust authentication")
                        .rationale("Trust authentication allows connections without any password")
                        .suggestedAction("Replace trust with scram-sha-256 or another secure method")
                        .affectedObject("pg_hba.conf line " + rule.getLineNumber())
                        .currentValue("auth_method = trust")
                        .recommendedValue("auth_method = scram-sha-256")
                        .build());
            }

            // Plain password authentication
            if ("password".equalsIgnoreCase(rule.getAuthMethod())) {
                recommendations.add(SecurityRecommendation.builder()
                        .category(SecurityRecommendation.Category.HBA)
                        .priority(SecurityRecommendation.Priority.HIGH)
                        .title("Plain Password Authentication")
                        .description("pg_hba.conf line " + rule.getLineNumber() + " uses plain password authentication")
                        .rationale("Plain password authentication sends passwords in clear text")
                        .suggestedAction("Replace password with scram-sha-256")
                        .affectedObject("pg_hba.conf line " + rule.getLineNumber())
                        .currentValue("auth_method = password")
                        .recommendedValue("auth_method = scram-sha-256")
                        .build());
            }

            // MD5 authentication
            if ("md5".equalsIgnoreCase(rule.getAuthMethod())) {
                recommendations.add(SecurityRecommendation.builder()
                        .category(SecurityRecommendation.Category.HBA)
                        .priority(SecurityRecommendation.Priority.MEDIUM)
                        .title("Deprecated MD5 Authentication")
                        .description("pg_hba.conf line " + rule.getLineNumber() + " uses deprecated MD5 authentication")
                        .rationale("MD5 is cryptographically weak and should be replaced")
                        .suggestedAction("Upgrade to scram-sha-256 authentication")
                        .affectedObject("pg_hba.conf line " + rule.getLineNumber())
                        .currentValue("auth_method = md5")
                        .recommendedValue("auth_method = scram-sha-256")
                        .build());
            }

            // Wide-open access with any method
            if (rule.isWideOpen() && !"reject".equalsIgnoreCase(rule.getAuthMethod())) {
                recommendations.add(SecurityRecommendation.builder()
                        .category(SecurityRecommendation.Category.HBA)
                        .priority(SecurityRecommendation.Priority.MEDIUM)
                        .title("Wide-Open Network Access")
                        .description("pg_hba.conf line " + rule.getLineNumber() + " allows access from all addresses")
                        .rationale("Allowing access from 0.0.0.0/0 exposes the database to the internet")
                        .suggestedAction("Restrict access to specific IP ranges or use VPN")
                        .affectedObject("pg_hba.conf line " + rule.getLineNumber())
                        .currentValue("address = " + rule.getAddress())
                        .recommendedValue("Specific IP range or CIDR block")
                        .build());
            }
        }

        return recommendations;
    }

    /**
     * Generates recommendations related to logging configuration.
     *
     * @param instanceName the database instance identifier
     * @return list of logging-related recommendations
     */
    public List<SecurityRecommendation> getLoggingRecommendations(String instanceName) {
        List<SecurityRecommendation> recommendations = new ArrayList<>();
        Map<String, String> settings = getLoggingSettings(instanceName);

        if (!"on".equalsIgnoreCase(settings.get("log_connections"))) {
            recommendations.add(SecurityRecommendation.builder()
                    .category(SecurityRecommendation.Category.LOGGING)
                    .priority(SecurityRecommendation.Priority.MEDIUM)
                    .title("Connection Logging Disabled")
                    .description("log_connections is not enabled")
                    .rationale("Connection logging is essential for security monitoring and auditing")
                    .suggestedAction("Enable log_connections in postgresql.conf")
                    .currentValue("log_connections = off")
                    .recommendedValue("log_connections = on")
                    .build());
        }

        String logStatement = settings.get("log_statement");
        if (logStatement == null || "none".equalsIgnoreCase(logStatement)) {
            recommendations.add(SecurityRecommendation.builder()
                    .category(SecurityRecommendation.Category.LOGGING)
                    .priority(SecurityRecommendation.Priority.LOW)
                    .title("Statement Logging Minimal")
                    .description("log_statement is set to 'none' or not configured")
                    .rationale("Statement logging helps track database activity for security analysis")
                    .suggestedAction("Consider setting log_statement to 'ddl' or 'mod' for audit purposes")
                    .currentValue("log_statement = " + (logStatement != null ? logStatement : "none"))
                    .recommendedValue("log_statement = ddl (at minimum)")
                    .build());
        }

        return recommendations;
    }

    private Map<String, String> getLoggingSettings(String instanceName) {
        Map<String, String> settings = new HashMap<>();

        String sql = """
            SELECT name, setting
            FROM pg_settings
            WHERE name IN ('log_connections', 'log_disconnections', 'log_statement')
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                settings.put(rs.getString("name"), rs.getString("setting"));
            }

        } catch (SQLException e) {
            LOG.errorf("Error fetching logging settings for instance %s: %s", instanceName, e.getMessage());
        }

        return settings;
    }

    /**
     * Generates recommendations related to data access control.
     *
     * @param instanceName the database instance identifier
     * @return list of access control recommendations
     */
    public List<SecurityRecommendation> getAccessControlRecommendations(String instanceName) {
        List<SecurityRecommendation> recommendations = new ArrayList<>();

        // Check for sensitive tables without RLS
        List<SensitiveTable> tablesNeedingRls = dataAccessPatternService.getTablesWithoutRls(instanceName);
        for (SensitiveTable table : tablesNeedingRls) {
            if (table.getSensitivityLevel() == SensitiveTable.SensitivityLevel.HIGH) {
                recommendations.add(SecurityRecommendation.builder()
                        .category(SecurityRecommendation.Category.ACCESS_CONTROL)
                        .priority(SecurityRecommendation.Priority.HIGH)
                        .title("High-Sensitivity Table Without RLS")
                        .description("Table '" + table.getFullyQualifiedName() + "' contains high-sensitivity data without RLS")
                        .rationale("Row-level security provides fine-grained access control for sensitive data")
                        .suggestedAction("Enable RLS: ALTER TABLE " + table.getFullyQualifiedName() + " ENABLE ROW LEVEL SECURITY")
                        .affectedObject(table.getFullyQualifiedName())
                        .build());
            } else if (table.getSensitivityLevel() == SensitiveTable.SensitivityLevel.MEDIUM) {
                recommendations.add(SecurityRecommendation.builder()
                        .category(SecurityRecommendation.Category.ACCESS_CONTROL)
                        .priority(SecurityRecommendation.Priority.MEDIUM)
                        .title("Sensitive Table Without RLS")
                        .description("Table '" + table.getFullyQualifiedName() + "' may contain sensitive data without RLS")
                        .rationale("Consider row-level security for tables containing PII")
                        .suggestedAction("Review data sensitivity and consider enabling RLS")
                        .affectedObject(table.getFullyQualifiedName())
                        .build());
            }
        }

        return recommendations;
    }

    /**
     * Generates a summary of recommendations by priority and category.
     *
     * @param instanceName the database instance identifier
     * @return map containing summary statistics
     */
    public Map<String, Object> getSummary(String instanceName) {
        Map<String, Object> summary = new HashMap<>();
        List<SecurityRecommendation> recommendations = getAllRecommendations(instanceName);

        summary.put("totalRecommendations", recommendations.size());

        // Count by priority
        Map<String, Long> byPriority = new HashMap<>();
        for (SecurityRecommendation.Priority p : SecurityRecommendation.Priority.values()) {
            long count = recommendations.stream()
                    .filter(r -> r.getPriority() == p)
                    .count();
            byPriority.put(p.getDisplayName(), count);
        }
        summary.put("byPriority", byPriority);

        // Count by category
        Map<String, Long> byCategory = new HashMap<>();
        for (SecurityRecommendation.Category c : SecurityRecommendation.Category.values()) {
            long count = recommendations.stream()
                    .filter(r -> r.getCategory() == c)
                    .count();
            if (count > 0) {
                byCategory.put(c.getDisplayName(), count);
            }
        }
        summary.put("byCategory", byCategory);

        // Individual counts for dashboard cards
        summary.put("criticalCount", byPriority.getOrDefault("Critical", 0L));
        summary.put("highCount", byPriority.getOrDefault("High", 0L));
        summary.put("mediumCount", byPriority.getOrDefault("Medium", 0L));
        summary.put("lowCount", byPriority.getOrDefault("Low", 0L));

        return summary;
    }
}
