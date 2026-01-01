package com.bovinemagnet.pgconsole.model;

/**
 * Represents a role membership relationship from pg_auth_members.
 * <p>
 * Maps the relationship between roles and their members, including
 * who granted the membership and whether admin privileges were granted.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class RoleMembership {

    private String roleName;
    private String memberName;
    private String grantedBy;
    private boolean adminOption;

    /**
     * Default constructor.
     */
    public RoleMembership() {
    }

    /**
     * Constructs a RoleMembership with the specified attributes.
     *
     * @param roleName    the role being granted
     * @param memberName  the role receiving the grant
     * @param grantedBy   the role that performed the grant
     * @param adminOption whether the member can grant this role to others
     */
    public RoleMembership(String roleName, String memberName, String grantedBy, boolean adminOption) {
        this.roleName = roleName;
        this.memberName = memberName;
        this.grantedBy = grantedBy;
        this.adminOption = adminOption;
    }

    // Getters and Setters

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }

    public boolean isAdminOption() {
        return adminOption;
    }

    public void setAdminOption(boolean adminOption) {
        this.adminOption = adminOption;
    }

    /**
     * Returns a formatted display string for this membership.
     *
     * @return membership description
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
