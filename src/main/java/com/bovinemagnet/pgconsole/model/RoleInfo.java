package com.bovinemagnet.pgconsole.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents PostgreSQL role information from pg_roles.
 * <p>
 * Contains all role attributes including privileges, membership information,
 * and password policy details.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class RoleInfo {

    private String roleName;
    private boolean superuser;
    private boolean inherit;
    private boolean createRole;
    private boolean createDb;
    private boolean canLogin;
    private boolean replication;
    private boolean bypassRls;
    private int connectionLimit;
    private OffsetDateTime validUntil;
    private String comment;
    private List<String> memberOf = new ArrayList<>();
    private List<String> members = new ArrayList<>();

    /**
     * Default constructor.
     */
    public RoleInfo() {
    }

    /**
     * Constructs a RoleInfo with the specified attributes.
     *
     * @param roleName        the role name
     * @param superuser       whether the role is a superuser
     * @param inherit         whether the role inherits privileges
     * @param createRole      whether the role can create other roles
     * @param createDb        whether the role can create databases
     * @param canLogin        whether the role can login
     * @param replication     whether the role has replication privileges
     * @param bypassRls       whether the role bypasses row-level security
     * @param connectionLimit the connection limit (-1 for unlimited)
     * @param validUntil      password expiration date (null for no expiry)
     */
    public RoleInfo(String roleName, boolean superuser, boolean inherit,
                    boolean createRole, boolean createDb, boolean canLogin,
                    boolean replication, boolean bypassRls, int connectionLimit,
                    OffsetDateTime validUntil) {
        this.roleName = roleName;
        this.superuser = superuser;
        this.inherit = inherit;
        this.createRole = createRole;
        this.createDb = createDb;
        this.canLogin = canLogin;
        this.replication = replication;
        this.bypassRls = bypassRls;
        this.connectionLimit = connectionLimit;
        this.validUntil = validUntil;
    }

    // Getters and Setters

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public boolean isSuperuser() {
        return superuser;
    }

    public void setSuperuser(boolean superuser) {
        this.superuser = superuser;
    }

    public boolean isInherit() {
        return inherit;
    }

    public void setInherit(boolean inherit) {
        this.inherit = inherit;
    }

    public boolean isCreateRole() {
        return createRole;
    }

    public void setCreateRole(boolean createRole) {
        this.createRole = createRole;
    }

    public boolean isCreateDb() {
        return createDb;
    }

    public void setCreateDb(boolean createDb) {
        this.createDb = createDb;
    }

    public boolean isCanLogin() {
        return canLogin;
    }

    public void setCanLogin(boolean canLogin) {
        this.canLogin = canLogin;
    }

    public boolean isReplication() {
        return replication;
    }

    public void setReplication(boolean replication) {
        this.replication = replication;
    }

    public boolean isBypassRls() {
        return bypassRls;
    }

    public void setBypassRls(boolean bypassRls) {
        this.bypassRls = bypassRls;
    }

    public int getConnectionLimit() {
        return connectionLimit;
    }

    public void setConnectionLimit(int connectionLimit) {
        this.connectionLimit = connectionLimit;
    }

    public OffsetDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(OffsetDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getMemberOf() {
        return memberOf;
    }

    public void setMemberOf(List<String> memberOf) {
        this.memberOf = memberOf;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    // Helper Methods

    /**
     * Returns a list of privilege badges for display.
     * <p>
     * Each badge represents a special privilege held by this role.
     *
     * @return list of privilege names for badge display
     */
    public List<String> getPrivilegeBadges() {
        List<String> badges = new ArrayList<>();
        if (superuser) badges.add("SUPERUSER");
        if (createRole) badges.add("CREATEROLE");
        if (createDb) badges.add("CREATEDB");
        if (replication) badges.add("REPLICATION");
        if (bypassRls) badges.add("BYPASSRLS");
        if (canLogin) badges.add("LOGIN");
        return badges;
    }

    /**
     * Checks if this role has any elevated privileges.
     * <p>
     * Elevated privileges include superuser, create role, create database,
     * replication, or bypass RLS capabilities.
     *
     * @return true if the role has elevated privileges
     */
    public boolean hasElevatedPrivileges() {
        return superuser || createRole || createDb || replication || bypassRls;
    }

    /**
     * Checks if the password has expired.
     *
     * @return true if validUntil is set and has passed
     */
    public boolean isPasswordExpired() {
        if (validUntil == null) {
            return false;
        }
        return validUntil.isBefore(OffsetDateTime.now());
    }

    /**
     * Checks if the password is expiring soon (within 30 days).
     *
     * @return true if validUntil is set and is within 30 days
     */
    public boolean isPasswordExpiringSoon() {
        if (validUntil == null) {
            return false;
        }
        OffsetDateTime thirtyDaysFromNow = OffsetDateTime.now().plusDays(30);
        return validUntil.isAfter(OffsetDateTime.now()) && validUntil.isBefore(thirtyDaysFromNow);
    }

    /**
     * Returns the connection limit as a display string.
     *
     * @return "Unlimited" if -1, otherwise the numeric limit
     */
    public String getConnectionLimitDisplay() {
        return connectionLimit == -1 ? "Unlimited" : String.valueOf(connectionLimit);
    }

    /**
     * Returns the CSS class for the role type badge.
     *
     * @return CSS class name based on role privileges
     */
    public String getRoleTypeCssClass() {
        if (superuser) return "bg-danger";
        if (hasElevatedPrivileges()) return "bg-warning text-dark";
        if (canLogin) return "bg-primary";
        return "bg-secondary";
    }

    /**
     * Returns a display label for the role type.
     *
     * @return role type description
     */
    public String getRoleTypeLabel() {
        if (superuser) return "Superuser";
        if (hasElevatedPrivileges()) return "Privileged";
        if (canLogin) return "Login Role";
        return "Group Role";
    }
}
