package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.ObjectPermission;
import com.bovinemagnet.pgconsole.model.RoleInfo;
import com.bovinemagnet.pgconsole.model.RoleMembership;
import com.bovinemagnet.pgconsole.model.SecurityWarning;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for auditing PostgreSQL roles and permissions.
 * <p>
 * Queries pg_roles, pg_auth_members, and related system catalogs
 * to analyse role configurations, privilege assignments, and
 * detect potential security issues.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class SecurityAuditService {

    private static final Logger LOG = Logger.getLogger(SecurityAuditService.class);

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Retrieves all database roles and their attributes.
     *
     * @param instanceName the database instance identifier
     * @return list of RoleInfo objects for all non-system roles
     */
    public List<RoleInfo> getAllRoles(String instanceName) {
        List<RoleInfo> roles = new ArrayList<>();

        String sql = """
            SELECT
                r.rolname,
                r.rolsuper,
                r.rolinherit,
                r.rolcreaterole,
                r.rolcreatedb,
                r.rolcanlogin,
                r.rolreplication,
                r.rolbypassrls,
                r.rolconnlimit,
                r.rolvaliduntil,
                pg_catalog.shobj_description(r.oid, 'pg_authid') AS comment
            FROM pg_roles r
            WHERE r.rolname NOT LIKE 'pg_%'
            ORDER BY r.rolsuper DESC, r.rolcanlogin DESC, r.rolname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                RoleInfo role = new RoleInfo();
                role.setRoleName(rs.getString("rolname"));
                role.setSuperuser(rs.getBoolean("rolsuper"));
                role.setInherit(rs.getBoolean("rolinherit"));
                role.setCreateRole(rs.getBoolean("rolcreaterole"));
                role.setCreateDb(rs.getBoolean("rolcreatedb"));
                role.setCanLogin(rs.getBoolean("rolcanlogin"));
                role.setReplication(rs.getBoolean("rolreplication"));
                role.setBypassRls(rs.getBoolean("rolbypassrls"));
                role.setConnectionLimit(rs.getInt("rolconnlimit"));

                Timestamp validUntil = rs.getTimestamp("rolvaliduntil");
                if (validUntil != null) {
                    role.setValidUntil(validUntil.toInstant().atOffset(ZoneOffset.UTC));
                }

                role.setComment(rs.getString("comment"));
                roles.add(role);
            }

            // Populate membership information
            populateRoleMemberships(conn, roles);

        } catch (SQLException e) {
            LOG.errorf("Error fetching roles for instance %s: %s", instanceName, e.getMessage());
        }

        return roles;
    }

    private void populateRoleMemberships(Connection conn, List<RoleInfo> roles) throws SQLException {
        Map<String, RoleInfo> roleMap = new HashMap<>();
        for (RoleInfo role : roles) {
            roleMap.put(role.getRoleName(), role);
        }

        String sql = """
            SELECT
                r1.rolname AS role_name,
                r2.rolname AS member_name
            FROM pg_auth_members am
            JOIN pg_roles r1 ON am.roleid = r1.oid
            JOIN pg_roles r2 ON am.member = r2.oid
            WHERE r1.rolname NOT LIKE 'pg_%'
              AND r2.rolname NOT LIKE 'pg_%'
            """;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String roleName = rs.getString("role_name");
                String memberName = rs.getString("member_name");

                RoleInfo role = roleMap.get(roleName);
                if (role != null) {
                    role.getMembers().add(memberName);
                }

                RoleInfo member = roleMap.get(memberName);
                if (member != null) {
                    member.getMemberOf().add(roleName);
                }
            }
        }
    }

    /**
     * Retrieves the role membership hierarchy.
     *
     * @param instanceName the database instance identifier
     * @return list of RoleMembership relationships
     */
    public List<RoleMembership> getRoleHierarchy(String instanceName) {
        List<RoleMembership> memberships = new ArrayList<>();

        String sql = """
            SELECT
                r1.rolname AS role_name,
                r2.rolname AS member_name,
                g.rolname AS granted_by,
                am.admin_option
            FROM pg_auth_members am
            JOIN pg_roles r1 ON am.roleid = r1.oid
            JOIN pg_roles r2 ON am.member = r2.oid
            LEFT JOIN pg_roles g ON am.grantor = g.oid
            WHERE r1.rolname NOT LIKE 'pg_%'
              AND r2.rolname NOT LIKE 'pg_%'
            ORDER BY r1.rolname, r2.rolname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                RoleMembership membership = new RoleMembership();
                membership.setRoleName(rs.getString("role_name"));
                membership.setMemberName(rs.getString("member_name"));
                membership.setGrantedBy(rs.getString("granted_by"));
                membership.setAdminOption(rs.getBoolean("admin_option"));
                memberships.add(membership);
            }

        } catch (SQLException e) {
            LOG.errorf("Error fetching role hierarchy for instance %s: %s", instanceName, e.getMessage());
        }

        return memberships;
    }

    /**
     * Retrieves all superuser roles.
     *
     * @param instanceName the database instance identifier
     * @return list of superuser RoleInfo objects
     */
    public List<RoleInfo> getSuperusers(String instanceName) {
        return getAllRoles(instanceName).stream()
                .filter(RoleInfo::isSuperuser)
                .toList();
    }

    /**
     * Retrieves roles with elevated privileges (CREATEDB, CREATEROLE, REPLICATION, BYPASSRLS).
     *
     * @param instanceName the database instance identifier
     * @return list of privileged RoleInfo objects
     */
    public List<RoleInfo> getPrivilegedRoles(String instanceName) {
        return getAllRoles(instanceName).stream()
                .filter(RoleInfo::hasElevatedPrivileges)
                .toList();
    }

    /**
     * Retrieves login roles (roles that can connect).
     *
     * @param instanceName the database instance identifier
     * @return list of login RoleInfo objects
     */
    public List<RoleInfo> getLoginRoles(String instanceName) {
        return getAllRoles(instanceName).stream()
                .filter(RoleInfo::isCanLogin)
                .toList();
    }

    /**
     * Retrieves table permissions from pg_class ACL.
     *
     * @param instanceName the database instance identifier
     * @return list of ObjectPermission for tables
     */
    public List<ObjectPermission> getTablePermissions(String instanceName) {
        List<ObjectPermission> permissions = new ArrayList<>();

        String sql = """
            SELECT
                n.nspname AS schema_name,
                c.relname AS table_name,
                c.relkind,
                unnest(c.relacl)::text AS acl_entry
            FROM pg_class c
            JOIN pg_namespace n ON c.relnamespace = n.oid
            WHERE c.relkind IN ('r', 'v', 'm', 'p')
              AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
              AND c.relacl IS NOT NULL
            ORDER BY n.nspname, c.relname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String aclEntry = rs.getString("acl_entry");
                ObjectPermission perm = parseAclEntry(aclEntry);
                if (perm != null) {
                    perm.setSchemaName(rs.getString("schema_name"));
                    perm.setObjectName(rs.getString("table_name"));
                    char relkind = rs.getString("relkind").charAt(0);
                    perm.setObjectType(ObjectPermission.ObjectType.fromRelkind(relkind));
                    permissions.add(perm);
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Error fetching table permissions for instance %s: %s", instanceName, e.getMessage());
        }

        return permissions;
    }

    /**
     * Retrieves schema permissions from pg_namespace ACL.
     *
     * @param instanceName the database instance identifier
     * @return list of ObjectPermission for schemas
     */
    public List<ObjectPermission> getSchemaPermissions(String instanceName) {
        List<ObjectPermission> permissions = new ArrayList<>();

        String sql = """
            SELECT
                n.nspname AS schema_name,
                unnest(n.nspacl)::text AS acl_entry
            FROM pg_namespace n
            WHERE n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
              AND n.nspacl IS NOT NULL
            ORDER BY n.nspname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String aclEntry = rs.getString("acl_entry");
                ObjectPermission perm = parseAclEntry(aclEntry);
                if (perm != null) {
                    perm.setSchemaName(rs.getString("schema_name"));
                    perm.setObjectName(rs.getString("schema_name"));
                    perm.setObjectType(ObjectPermission.ObjectType.SCHEMA);
                    permissions.add(perm);
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Error fetching schema permissions for instance %s: %s", instanceName, e.getMessage());
        }

        return permissions;
    }

    private ObjectPermission parseAclEntry(String aclEntry) {
        if (aclEntry == null || aclEntry.isEmpty()) {
            return null;
        }

        // ACL format: grantee=privileges/grantor
        // e.g., "user1=arwdDxt/postgres" or "=r/postgres" (empty grantee means PUBLIC)
        try {
            int equalsPos = aclEntry.indexOf('=');
            int slashPos = aclEntry.indexOf('/');

            if (equalsPos < 0 || slashPos < 0) {
                return null;
            }

            String grantee = aclEntry.substring(0, equalsPos);
            if (grantee.isEmpty()) {
                grantee = "PUBLIC";
            }

            String privileges = aclEntry.substring(equalsPos + 1, slashPos);
            String grantor = aclEntry.substring(slashPos + 1);

            ObjectPermission perm = new ObjectPermission();
            perm.setGrantee(grantee);
            perm.setGrantor(grantor);
            perm.setPrivileges(ObjectPermission.Privilege.fromAclString(privileges));
            perm.setGrantOption(privileges.contains("*"));

            return perm;
        } catch (Exception e) {
            LOG.debugf("Failed to parse ACL entry: %s", aclEntry);
            return null;
        }
    }

    /**
     * Generates security warnings for elevated privilege issues.
     *
     * @param instanceName the database instance identifier
     * @return list of SecurityWarning objects
     */
    public List<SecurityWarning> getElevatedPrivilegeWarnings(String instanceName) {
        List<SecurityWarning> warnings = new ArrayList<>();
        List<RoleInfo> roles = getAllRoles(instanceName);

        for (RoleInfo role : roles) {
            // Superuser warning
            if (role.isSuperuser() && role.isCanLogin()) {
                warnings.add(SecurityWarning.builder()
                        .type(SecurityWarning.WarningType.SUPERUSER_ROLE)
                        .severity(SecurityWarning.Severity.HIGH)
                        .subject(role.getRoleName())
                        .description("Role '" + role.getRoleName() + "' is a superuser with login capability")
                        .recommendation("Consider using a non-superuser role for application access")
                        .detail("Privileges", String.join(", ", role.getPrivilegeBadges()))
                        .build());
            }

            // Bypass RLS warning
            if (role.isBypassRls() && !role.isSuperuser() && role.isCanLogin()) {
                warnings.add(SecurityWarning.builder()
                        .type(SecurityWarning.WarningType.BYPASS_RLS_ROLE)
                        .severity(SecurityWarning.Severity.MEDIUM)
                        .subject(role.getRoleName())
                        .description("Role '" + role.getRoleName() + "' can bypass row-level security")
                        .recommendation("Remove BYPASSRLS unless explicitly required")
                        .build());
            }
        }

        return warnings;
    }

    /**
     * Generates security warnings for password policy issues.
     *
     * @param instanceName the database instance identifier
     * @return list of SecurityWarning objects
     */
    public List<SecurityWarning> getPasswordPolicyWarnings(String instanceName) {
        List<SecurityWarning> warnings = new ArrayList<>();
        List<RoleInfo> loginRoles = getLoginRoles(instanceName);

        for (RoleInfo role : loginRoles) {
            // Expired password
            if (role.isPasswordExpired()) {
                warnings.add(SecurityWarning.builder()
                        .type(SecurityWarning.WarningType.EXPIRED_PASSWORD)
                        .severity(SecurityWarning.Severity.HIGH)
                        .subject(role.getRoleName())
                        .description("Role '" + role.getRoleName() + "' has an expired password")
                        .recommendation("Update the password or set a new expiration date")
                        .detail("Expired", role.getValidUntil().toString())
                        .build());
            }
            // Password expiring soon
            else if (role.isPasswordExpiringSoon()) {
                warnings.add(SecurityWarning.builder()
                        .type(SecurityWarning.WarningType.EXPIRING_PASSWORD)
                        .severity(SecurityWarning.Severity.MEDIUM)
                        .subject(role.getRoleName())
                        .description("Role '" + role.getRoleName() + "' password expires soon")
                        .recommendation("Plan password rotation before expiration")
                        .detail("Expires", role.getValidUntil().toString())
                        .build());
            }
            // No password expiry set
            else if (role.getValidUntil() == null) {
                warnings.add(SecurityWarning.builder()
                        .type(SecurityWarning.WarningType.NO_PASSWORD_EXPIRY)
                        .severity(SecurityWarning.Severity.LOW)
                        .subject(role.getRoleName())
                        .description("Role '" + role.getRoleName() + "' has no password expiration set")
                        .recommendation("Consider setting VALID UNTIL for password rotation")
                        .build());
            }
        }

        return warnings;
    }

    /**
     * Generates a summary of security audit findings.
     *
     * @param instanceName the database instance identifier
     * @return map containing summary statistics
     */
    public Map<String, Object> getSummary(String instanceName) {
        Map<String, Object> summary = new HashMap<>();

        List<RoleInfo> roles = getAllRoles(instanceName);
        List<SecurityWarning> privilegeWarnings = getElevatedPrivilegeWarnings(instanceName);
        List<SecurityWarning> passwordWarnings = getPasswordPolicyWarnings(instanceName);

        summary.put("totalRoles", roles.size());
        summary.put("superuserCount", roles.stream().filter(RoleInfo::isSuperuser).count());
        summary.put("loginRoleCount", roles.stream().filter(RoleInfo::isCanLogin).count());
        summary.put("privilegedRoleCount", roles.stream().filter(RoleInfo::hasElevatedPrivileges).count());
        summary.put("createRoleCount", roles.stream().filter(RoleInfo::isCreateRole).count());
        summary.put("createDbCount", roles.stream().filter(RoleInfo::isCreateDb).count());
        summary.put("replicationCount", roles.stream().filter(RoleInfo::isReplication).count());
        summary.put("bypassRlsCount", roles.stream().filter(RoleInfo::isBypassRls).count());

        List<SecurityWarning> allWarnings = new ArrayList<>();
        allWarnings.addAll(privilegeWarnings);
        allWarnings.addAll(passwordWarnings);

        summary.put("totalWarnings", allWarnings.size());
        summary.put("criticalWarnings", allWarnings.stream()
                .filter(w -> w.getSeverity() == SecurityWarning.Severity.CRITICAL).count());
        summary.put("highWarnings", allWarnings.stream()
                .filter(w -> w.getSeverity() == SecurityWarning.Severity.HIGH).count());
        summary.put("mediumWarnings", allWarnings.stream()
                .filter(w -> w.getSeverity() == SecurityWarning.Severity.MEDIUM).count());

        return summary;
    }

    /**
     * Gets all security warnings (privilege + password warnings combined).
     *
     * @param instanceName the database instance identifier
     * @return list of all security warnings
     */
    public List<SecurityWarning> getAllWarnings(String instanceName) {
        List<SecurityWarning> allWarnings = new ArrayList<>();
        allWarnings.addAll(getElevatedPrivilegeWarnings(instanceName));
        allWarnings.addAll(getPasswordPolicyWarnings(instanceName));
        return allWarnings;
    }

    /**
     * Gets role membership information (alias for getRoleHierarchy).
     *
     * @param instanceName the database instance identifier
     * @return list of role memberships
     */
    public List<RoleMembership> getRoleMemberships(String instanceName) {
        return getRoleHierarchy(instanceName);
    }
}
