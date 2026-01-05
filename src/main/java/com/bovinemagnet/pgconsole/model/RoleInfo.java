package com.bovinemagnet.pgconsole.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents PostgreSQL role information retrieved from the pg_roles system view.
 * <p>
 * This data transfer object encapsulates all attributes of a PostgreSQL role,
 * including privileges (superuser, create role, create database), connection
 * policies (login capability, connection limits), security settings (row-level
 * security bypass), and role membership hierarchy. It provides helper methods
 * for UI display purposes such as badge generation and password expiry checks.
 * </p>
 * <p>
 * PostgreSQL roles serve as both users (when they can login) and groups (when
 * they cannot). This class represents both types and includes membership tracking
 * to show role hierarchies.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/view-pg-roles.html">PostgreSQL pg_roles Documentation</a>
 * @since 0.0.0
 */
public class RoleInfo {

    /**
     * The name of the role. This is the unique identifier for the role in PostgreSQL.
     */
    private String roleName;

    /**
     * Whether the role has superuser privileges. Superusers bypass all permission checks
     * except the right to log in.
     */
    private boolean superuser;

    /**
     * Whether the role inherits privileges from roles it is a member of. If false,
     * the role must explicitly SET ROLE to use privileges of member roles.
     */
    private boolean inherit;

    /**
     * Whether the role can create other roles (equivalent to CREATEROLE privilege).
     */
    private boolean createRole;

    /**
     * Whether the role can create databases (equivalent to CREATEDB privilege).
     */
    private boolean createDb;

    /**
     * Whether the role can log in to the database. Roles without this privilege
     * are typically used as group roles for privilege management.
     */
    private boolean canLogin;

    /**
     * Whether the role has replication privileges, allowing it to initiate
     * streaming replication connections and create/drop replication slots.
     */
    private boolean replication;

    /**
     * Whether the role bypasses row-level security (RLS) policies. Only superusers
     * and roles with this attribute can bypass RLS.
     */
    private boolean bypassRls;

    /**
     * Maximum number of concurrent connections allowed for this role. A value of -1
     * indicates no limit (unlimited connections).
     */
    private int connectionLimit;

    /**
     * Password expiration timestamp. If set, the role's password becomes invalid
     * after this date and time. Null indicates the password never expires.
     */
    private OffsetDateTime validUntil;

    /**
     * User-defined comment or description for this role, retrieved from pg_description.
     * May be null if no comment has been set.
     */
    private String comment;

    /**
     * List of role names that this role is a member of (parent roles in the hierarchy).
     * Represents the roles from which this role inherits privileges.
     */
    private List<String> memberOf = new ArrayList<>();

    /**
     * List of role names that are members of this role (child roles in the hierarchy).
     * Represents roles that inherit privileges from this role.
     */
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

    /**
     * Returns the role name.
     *
     * @return the unique role identifier, never null for valid role objects
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * Sets the role name.
     *
     * @param roleName the unique role identifier to set
     */
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    /**
     * Checks whether this role has superuser privileges.
     * <p>
     * Superusers bypass all permission checks except the right to log in.
     * </p>
     *
     * @return true if the role is a superuser, false otherwise
     */
    public boolean isSuperuser() {
        return superuser;
    }

    /**
     * Sets the superuser privilege flag.
     *
     * @param superuser true if the role should be a superuser, false otherwise
     */
    public void setSuperuser(boolean superuser) {
        this.superuser = superuser;
    }

    /**
     * Checks whether this role inherits privileges from parent roles.
     * <p>
     * When false, the role must explicitly SET ROLE to use privileges of roles
     * it is a member of.
     * </p>
     *
     * @return true if the role inherits privileges, false otherwise
     */
    public boolean isInherit() {
        return inherit;
    }

    /**
     * Sets the inherit privilege flag.
     *
     * @param inherit true if the role should inherit privileges from parent roles
     */
    public void setInherit(boolean inherit) {
        this.inherit = inherit;
    }

    /**
     * Checks whether this role can create other roles.
     *
     * @return true if the role has CREATEROLE privilege, false otherwise
     */
    public boolean isCreateRole() {
        return createRole;
    }

    /**
     * Sets the create role privilege flag.
     *
     * @param createRole true if the role should be able to create other roles
     */
    public void setCreateRole(boolean createRole) {
        this.createRole = createRole;
    }

    /**
     * Checks whether this role can create databases.
     *
     * @return true if the role has CREATEDB privilege, false otherwise
     */
    public boolean isCreateDb() {
        return createDb;
    }

    /**
     * Sets the create database privilege flag.
     *
     * @param createDb true if the role should be able to create databases
     */
    public void setCreateDb(boolean createDb) {
        this.createDb = createDb;
    }

    /**
     * Checks whether this role can log in to the database.
     * <p>
     * Roles without login capability are typically used as group roles for
     * privilege management rather than as user accounts.
     * </p>
     *
     * @return true if the role has LOGIN privilege, false otherwise
     */
    public boolean isCanLogin() {
        return canLogin;
    }

    /**
     * Sets the login capability flag.
     *
     * @param canLogin true if the role should be able to log in
     */
    public void setCanLogin(boolean canLogin) {
        this.canLogin = canLogin;
    }

    /**
     * Checks whether this role has replication privileges.
     * <p>
     * Replication privileges allow the role to initiate streaming replication
     * connections and create or drop replication slots.
     * </p>
     *
     * @return true if the role has REPLICATION privilege, false otherwise
     */
    public boolean isReplication() {
        return replication;
    }

    /**
     * Sets the replication privilege flag.
     *
     * @param replication true if the role should have replication privileges
     */
    public void setReplication(boolean replication) {
        this.replication = replication;
    }

    /**
     * Checks whether this role bypasses row-level security policies.
     * <p>
     * Only superusers and roles with this attribute can bypass row-level
     * security (RLS) policies.
     * </p>
     *
     * @return true if the role has BYPASSRLS privilege, false otherwise
     */
    public boolean isBypassRls() {
        return bypassRls;
    }

    /**
     * Sets the bypass row-level security flag.
     *
     * @param bypassRls true if the role should bypass RLS policies
     */
    public void setBypassRls(boolean bypassRls) {
        this.bypassRls = bypassRls;
    }

    /**
     * Returns the maximum number of concurrent connections allowed for this role.
     *
     * @return the connection limit, or -1 if unlimited
     */
    public int getConnectionLimit() {
        return connectionLimit;
    }

    /**
     * Sets the maximum number of concurrent connections for this role.
     *
     * @param connectionLimit the connection limit, or -1 for unlimited connections
     */
    public void setConnectionLimit(int connectionLimit) {
        this.connectionLimit = connectionLimit;
    }

    /**
     * Returns the password expiration timestamp.
     *
     * @return the date and time when the password expires, or null if it never expires
     */
    public OffsetDateTime getValidUntil() {
        return validUntil;
    }

    /**
     * Sets the password expiration timestamp.
     *
     * @param validUntil the date and time when the password should expire, or null for no expiry
     */
    public void setValidUntil(OffsetDateTime validUntil) {
        this.validUntil = validUntil;
    }

    /**
     * Returns the user-defined comment or description for this role.
     *
     * @return the role comment, or null if no comment has been set
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the user-defined comment or description for this role.
     *
     * @param comment the comment to set, or null to clear the comment
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Returns the list of parent roles that this role is a member of.
     * <p>
     * These are the roles from which this role inherits privileges (if the
     * inherit flag is true).
     * </p>
     *
     * @return list of parent role names, never null but may be empty
     */
    public List<String> getMemberOf() {
        return memberOf;
    }

    /**
     * Sets the list of parent roles that this role is a member of.
     *
     * @param memberOf list of parent role names, must not be null
     */
    public void setMemberOf(List<String> memberOf) {
        this.memberOf = memberOf;
    }

    /**
     * Returns the list of child roles that are members of this role.
     * <p>
     * These are the roles that inherit privileges from this role.
     * </p>
     *
     * @return list of child role names, never null but may be empty
     */
    public List<String> getMembers() {
        return members;
    }

    /**
     * Sets the list of child roles that are members of this role.
     *
     * @param members list of child role names, must not be null
     */
    public void setMembers(List<String> members) {
        this.members = members;
    }

    // Helper Methods

    /**
     * Returns a list of privilege badges for UI display.
     * <p>
     * Each badge represents a special privilege held by this role. The badges
     * are returned in a consistent order: SUPERUSER, CREATEROLE, CREATEDB,
     * REPLICATION, BYPASSRLS, LOGIN. Only privileges that the role actually
     * has are included in the returned list.
     * </p>
     * <p>
     * This method is primarily used by Qute templates to render privilege
     * badges in the roles dashboard.
     * </p>
     *
     * @return list of privilege names suitable for badge display, never null but may be empty
     * @see #hasElevatedPrivileges()
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
     * Checks whether this role has any elevated privileges beyond basic login.
     * <p>
     * Elevated privileges include:
     * </p>
     * <ul>
     * <li>Superuser - bypasses all permission checks</li>
     * <li>Create role - can create and manage other roles</li>
     * <li>Create database - can create new databases</li>
     * <li>Replication - can initiate streaming replication</li>
     * <li>Bypass RLS - can bypass row-level security policies</li>
     * </ul>
     * <p>
     * This method is useful for security auditing and UI highlighting of
     * privileged accounts.
     * </p>
     *
     * @return true if the role has any elevated privileges, false otherwise
     * @see #getRoleTypeCssClass()
     * @see #getRoleTypeLabel()
     */
    public boolean hasElevatedPrivileges() {
        return superuser || createRole || createDb || replication || bypassRls;
    }

    /**
     * Checks whether the role's password has expired.
     * <p>
     * A password is considered expired if the validUntil timestamp is set and
     * represents a date/time in the past. If validUntil is null, the password
     * never expires and this method returns false.
     * </p>
     *
     * @return true if the password has expired, false if it is still valid or never expires
     * @see #isPasswordExpiringSoon()
     * @see #getValidUntil()
     */
    public boolean isPasswordExpired() {
        if (validUntil == null) {
            return false;
        }
        return validUntil.isBefore(OffsetDateTime.now());
    }

    /**
     * Checks whether the role's password is expiring soon (within 30 days).
     * <p>
     * This method is useful for proactive password expiry warnings. It returns
     * true only if:
     * </p>
     * <ul>
     * <li>The validUntil timestamp is set (not null)</li>
     * <li>The password has not already expired</li>
     * <li>The password will expire within the next 30 days</li>
     * </ul>
     *
     * @return true if the password expires within 30 days, false otherwise
     * @see #isPasswordExpired()
     */
    public boolean isPasswordExpiringSoon() {
        if (validUntil == null) {
            return false;
        }
        OffsetDateTime thirtyDaysFromNow = OffsetDateTime.now().plusDays(30);
        return validUntil.isAfter(OffsetDateTime.now()) && validUntil.isBefore(thirtyDaysFromNow);
    }

    /**
     * Returns the connection limit as a human-readable display string.
     * <p>
     * Converts the numeric connection limit to a string suitable for UI display.
     * A limit of -1 (which represents unlimited connections in PostgreSQL) is
     * displayed as "Unlimited", whilst other values are displayed as their
     * numeric representation.
     * </p>
     *
     * @return "Unlimited" if the connection limit is -1, otherwise the numeric limit as a string
     * @see #getConnectionLimit()
     */
    public String getConnectionLimitDisplay() {
        return connectionLimit == -1 ? "Unlimited" : String.valueOf(connectionLimit);
    }

    /**
     * Returns the Bootstrap CSS class for styling the role type badge.
     * <p>
     * Returns different CSS classes based on the role's privilege level, suitable
     * for use with Bootstrap badge components:
     * </p>
     * <ul>
     * <li><code>bg-danger</code> - For superuser roles (highest risk)</li>
     * <li><code>bg-warning text-dark</code> - For roles with elevated privileges</li>
     * <li><code>bg-primary</code> - For standard login roles</li>
     * <li><code>bg-secondary</code> - For group roles (cannot login)</li>
     * </ul>
     *
     * @return Bootstrap CSS class name for badge styling, never null
     * @see #getRoleTypeLabel()
     * @see #hasElevatedPrivileges()
     */
    public String getRoleTypeCssClass() {
        if (superuser) return "bg-danger";
        if (hasElevatedPrivileges()) return "bg-warning text-dark";
        if (canLogin) return "bg-primary";
        return "bg-secondary";
    }

    /**
     * Returns a human-readable label describing the role type.
     * <p>
     * Categorises the role based on its privileges into one of four types:
     * </p>
     * <ul>
     * <li>Superuser - Has unrestricted access to the database</li>
     * <li>Privileged - Has elevated privileges but is not a superuser</li>
     * <li>Login Role - Can log in but has no special privileges</li>
     * <li>Group Role - Cannot log in; used for privilege grouping</li>
     * </ul>
     *
     * @return role type description, never null
     * @see #getRoleTypeCssClass()
     * @see #hasElevatedPrivileges()
     */
    public String getRoleTypeLabel() {
        if (superuser) return "Superuser";
        if (hasElevatedPrivileges()) return "Privileged";
        if (canLogin) return "Login Role";
        return "Group Role";
    }
}
