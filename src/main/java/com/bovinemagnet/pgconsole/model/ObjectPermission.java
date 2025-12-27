package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents permissions granted on a database object.
 * <p>
 * Stores ACL information from pg_class, pg_namespace, and other
 * system catalogs that track object permissions.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class ObjectPermission {

    /**
     * Types of database objects that can have permissions.
     */
    public enum ObjectType {
        TABLE("Table", "r"),
        VIEW("View", "v"),
        MATERIALIZED_VIEW("Materialized View", "m"),
        SEQUENCE("Sequence", "S"),
        FUNCTION("Function", "f"),
        PROCEDURE("Procedure", "p"),
        SCHEMA("Schema", "n"),
        DATABASE("Database", "d"),
        TABLESPACE("Tablespace", "t"),
        TYPE("Type", "T"),
        FOREIGN_DATA_WRAPPER("Foreign Data Wrapper", "F"),
        FOREIGN_SERVER("Foreign Server", "s"),
        LANGUAGE("Language", "l");

        private final String displayName;
        private final String aclCode;

        ObjectType(String displayName, String aclCode) {
            this.displayName = displayName;
            this.aclCode = aclCode;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getAclCode() {
            return aclCode;
        }

        /**
         * Gets the ObjectType from a PostgreSQL relkind character.
         *
         * @param relkind the relkind character from pg_class
         * @return the corresponding ObjectType, or TABLE as default
         */
        public static ObjectType fromRelkind(char relkind) {
            return switch (relkind) {
                case 'r' -> TABLE;
                case 'v' -> VIEW;
                case 'm' -> MATERIALIZED_VIEW;
                case 'S' -> SEQUENCE;
                case 'f' -> FUNCTION;
                case 'p' -> PROCEDURE;
                default -> TABLE;
            };
        }
    }

    /**
     * Types of privileges that can be granted.
     */
    public enum Privilege {
        SELECT("SELECT", "r"),
        INSERT("INSERT", "a"),
        UPDATE("UPDATE", "w"),
        DELETE("DELETE", "d"),
        TRUNCATE("TRUNCATE", "D"),
        REFERENCES("REFERENCES", "x"),
        TRIGGER("TRIGGER", "t"),
        CREATE("CREATE", "C"),
        CONNECT("CONNECT", "c"),
        TEMPORARY("TEMPORARY", "T"),
        EXECUTE("EXECUTE", "X"),
        USAGE("USAGE", "U"),
        ALL("ALL", "*");

        private final String displayName;
        private final String aclCode;

        Privilege(String displayName, String aclCode) {
            this.displayName = displayName;
            this.aclCode = aclCode;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getAclCode() {
            return aclCode;
        }

        /**
         * Parses privileges from a PostgreSQL ACL string.
         *
         * @param aclString the ACL privilege string (e.g., "arwdDxt")
         * @return list of Privilege enums
         */
        public static List<Privilege> fromAclString(String aclString) {
            List<Privilege> privileges = new ArrayList<>();
            if (aclString == null || aclString.isEmpty()) {
                return privileges;
            }
            for (char c : aclString.toCharArray()) {
                for (Privilege p : values()) {
                    if (p.aclCode.equals(String.valueOf(c))) {
                        privileges.add(p);
                        break;
                    }
                }
            }
            return privileges;
        }
    }

    private ObjectType objectType;
    private String schemaName;
    private String objectName;
    private String grantee;
    private List<Privilege> privileges = new ArrayList<>();
    private String grantor;
    private boolean grantOption;

    /**
     * Default constructor.
     */
    public ObjectPermission() {
    }

    /**
     * Constructs an ObjectPermission with the specified attributes.
     *
     * @param objectType the type of database object
     * @param schemaName the schema containing the object
     * @param objectName the name of the object
     * @param grantee    the role receiving the permission
     * @param privileges the list of privileges granted
     */
    public ObjectPermission(ObjectType objectType, String schemaName, String objectName,
                            String grantee, List<Privilege> privileges) {
        this.objectType = objectType;
        this.schemaName = schemaName;
        this.objectName = objectName;
        this.grantee = grantee;
        this.privileges = privileges;
    }

    // Getters and Setters

    public ObjectType getObjectType() {
        return objectType;
    }

    public void setObjectType(ObjectType objectType) {
        this.objectType = objectType;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getGrantee() {
        return grantee;
    }

    public void setGrantee(String grantee) {
        this.grantee = grantee;
    }

    public List<Privilege> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(List<Privilege> privileges) {
        this.privileges = privileges;
    }

    public String getGrantor() {
        return grantor;
    }

    public void setGrantor(String grantor) {
        this.grantor = grantor;
    }

    public boolean isGrantOption() {
        return grantOption;
    }

    public void setGrantOption(boolean grantOption) {
        this.grantOption = grantOption;
    }

    // Helper Methods

    /**
     * Returns the fully qualified object name.
     *
     * @return schema.object format
     */
    public String getFullyQualifiedName() {
        if (schemaName == null || schemaName.isEmpty()) {
            return objectName;
        }
        return schemaName + "." + objectName;
    }

    /**
     * Returns a comma-separated list of privilege names.
     *
     * @return privilege names for display
     */
    public String getPrivilegesDisplay() {
        if (privileges == null || privileges.isEmpty()) {
            return "NONE";
        }
        return String.join(", ", privileges.stream()
                .map(Privilege::getDisplayName)
                .toList());
    }

    /**
     * Checks if this permission includes a specific privilege.
     *
     * @param privilege the privilege to check
     * @return true if the privilege is included
     */
    public boolean hasPrivilege(Privilege privilege) {
        return privileges != null && privileges.contains(privilege);
    }

    /**
     * Returns the CSS class for the privilege badge.
     *
     * @return CSS class based on privilege level
     */
    public String getPrivilegeLevelCssClass() {
        if (privileges.contains(Privilege.ALL)) {
            return "bg-danger";
        }
        if (privileges.contains(Privilege.DELETE) || privileges.contains(Privilege.TRUNCATE)) {
            return "bg-warning text-dark";
        }
        if (privileges.contains(Privilege.INSERT) || privileges.contains(Privilege.UPDATE)) {
            return "bg-info";
        }
        return "bg-secondary";
    }
}
