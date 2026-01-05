package com.bovinemagnet.pgconsole.model;

/**
 * Represents a role membership relationship from PostgreSQL's {@code pg_auth_members} system catalogue.
 * <p>
 * This class models the hierarchical role membership structure in PostgreSQL, capturing which roles
 * are members of other roles (PostgreSQL's approach to group roles and privilege inheritance). Each
 * instance represents a single membership relationship, including metadata about who granted the
 * membership and whether the member can further delegate the role to others.
 * <p>
 * PostgreSQL roles can be granted to other roles, creating a membership hierarchy. When a role is a
 * member of another role, it inherits the privileges of that role. The {@code WITH ADMIN OPTION}
 * allows the member to further grant the role to other roles.
 * <p>
 * Example hierarchy:
 * <pre>{@code
 * -- Create roles
 * CREATE ROLE app_admin;
 * CREATE ROLE app_user;
 *
 * -- Grant app_admin to app_user with admin option
 * GRANT app_admin TO app_user WITH ADMIN OPTION;
 *
 * -- This would be represented as:
 * RoleMembership membership = new RoleMembership(
 *     "app_admin",    // roleName - the role being granted
 *     "app_user",     // memberName - the role receiving the grant
 *     "postgres",     // grantedBy - who performed the grant
 *     true            // adminOption - app_user can grant app_admin to others
 * );
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/catalog-pg-auth-members.html">PostgreSQL pg_auth_members Documentation</a>
 */
public class RoleMembership {

    /**
     * The name of the role being granted (the parent role in the membership hierarchy).
     * <p>
     * This corresponds to the {@code roleid} in {@code pg_auth_members}, resolved to the role name.
     * Never null for valid membership relationships.
     */
    private String roleName;

    /**
     * The name of the role that is a member of {@code roleName} (the child role in the hierarchy).
     * <p>
     * This corresponds to the {@code member} in {@code pg_auth_members}, resolved to the role name.
     * Never null for valid membership relationships.
     */
    private String memberName;

    /**
     * The name of the role that granted this membership.
     * <p>
     * This corresponds to the {@code grantor} in {@code pg_auth_members}, resolved to the role name.
     * Typically the role that executed the {@code GRANT} statement. May be null if the grantor
     * information is not available.
     */
    private String grantedBy;

    /**
     * Indicates whether the member has the {@code ADMIN OPTION} for this role.
     * <p>
     * When {@code true}, the member can grant the role to other roles and revoke it from roles
     * that were granted by this member. This corresponds to the {@code admin_option} column in
     * {@code pg_auth_members}.
     * <p>
     * If {@code false}, the member can only inherit the privileges but cannot delegate the role.
     */
    private boolean adminOption;

    /**
     * Constructs an empty RoleMembership instance.
     * <p>
     * This constructor is typically used by frameworks for deserialisation or when building
     * the object incrementally using setters.
     */
    public RoleMembership() {
    }

    /**
     * Constructs a RoleMembership with the specified attributes.
     * <p>
     * This constructor initialises all fields of the role membership relationship, representing
     * a complete snapshot of a {@code GRANT role TO member} operation in PostgreSQL.
     *
     * @param roleName    the name of the role being granted (parent role in the hierarchy); must not be null
     * @param memberName  the name of the role receiving the grant (child role in the hierarchy); must not be null
     * @param grantedBy   the name of the role that performed the grant; may be null if unknown
     * @param adminOption {@code true} if the member has {@code WITH ADMIN OPTION}, allowing them to
     *                    further grant this role to others; {@code false} otherwise
     */
    public RoleMembership(String roleName, String memberName, String grantedBy, boolean adminOption) {
        this.roleName = roleName;
        this.memberName = memberName;
        this.grantedBy = grantedBy;
        this.adminOption = adminOption;
    }

    // Getters and Setters

    /**
     * Returns the name of the role being granted.
     * <p>
     * This is the parent role in the membership hierarchy whose privileges are inherited by the member.
     *
     * @return the role name, or null if not set
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * Sets the name of the role being granted.
     *
     * @param roleName the role name to set
     */
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    /**
     * Returns the name of the role that is a member.
     * <p>
     * This is the child role in the membership hierarchy that inherits privileges from the parent role.
     *
     * @return the member role name, or null if not set
     */
    public String getMemberName() {
        return memberName;
    }

    /**
     * Sets the name of the role that is a member.
     *
     * @param memberName the member role name to set
     */
    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    /**
     * Returns the name of the role that granted this membership.
     * <p>
     * This identifies who executed the {@code GRANT} statement that created this membership relationship.
     *
     * @return the grantor role name, or null if not available
     */
    public String getGrantedBy() {
        return grantedBy;
    }

    /**
     * Sets the name of the role that granted this membership.
     *
     * @param grantedBy the grantor role name to set
     */
    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }

    /**
     * Checks whether the member has the {@code ADMIN OPTION} for this role.
     * <p>
     * When this returns {@code true}, the member can grant the role to other roles and revoke it
     * from roles that were granted by this member. This corresponds to the {@code WITH ADMIN OPTION}
     * clause in PostgreSQL's {@code GRANT} statement.
     *
     * @return {@code true} if the member has admin privileges for this role, {@code false} otherwise
     */
    public boolean isAdminOption() {
        return adminOption;
    }

    /**
     * Sets whether the member has the {@code ADMIN OPTION} for this role.
     *
     * @param adminOption {@code true} to grant admin privileges, {@code false} otherwise
     */
    public void setAdminOption(boolean adminOption) {
        this.adminOption = adminOption;
    }

    /**
     * Returns a human-readable formatted display string for this membership relationship.
     * <p>
     * The format is: {@code memberName → roleName} with an optional {@code (WITH ADMIN)} suffix
     * if the member has admin privileges. This provides a concise representation suitable for
     * displaying in user interfaces or logs.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code app_user → app_admin}</li>
     *   <li>{@code developer → superuser (WITH ADMIN)}</li>
     * </ul>
     *
     * @return a formatted string describing the membership relationship, never null
     */
    public String getDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(memberName).append(" → ").append(roleName);
        if (adminOption) {
            sb.append(" (WITH ADMIN)");
        }
        return sb.toString();
    }
}
