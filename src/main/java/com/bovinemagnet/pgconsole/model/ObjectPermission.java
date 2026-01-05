package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents permissions granted on a database object in PostgreSQL.
 * <p>
 * This class models the Access Control List (ACL) information from PostgreSQL system catalogs
 * such as {@code pg_class}, {@code pg_namespace}, and related tables. It encapsulates the
 * privileges granted to specific roles on database objects like tables, views, sequences,
 * functions, and schemas.
 * </p>
 * <p>
 * The permission model includes the object type, the grantee (role receiving the permission),
 * the privileges granted, and whether the grantee can further grant these privileges to others
 * (GRANT OPTION).
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * ObjectPermission permission = new ObjectPermission(
 *     ObjectType.TABLE,
 *     "public",
 *     "users",
 *     "app_user",
 *     List.of(Privilege.SELECT, Privilege.INSERT)
 * );
 * String displayPrivileges = permission.getPrivilegesDisplay(); // "SELECT, INSERT"
 * }</pre>
 * </p>
 *
 * @author Paul Snow
 * @since 0.0.0
 * @see ObjectType
 * @see Privilege
 */
public class ObjectPermission {

    /**
     * Enumeration of database object types that can have permissions in PostgreSQL.
     * <p>
     * Each object type corresponds to a specific PostgreSQL catalog entry and has
     * an associated display name and ACL code used in system catalog queries.
     * </p>
     *
     * @see <a href="https://www.postgresql.org/docs/current/catalog-pg-class.html">PostgreSQL pg_class</a>
     */
    public enum ObjectType {
        /** Regular table (relkind 'r'). */
        TABLE("Table", "r"),

        /** View (relkind 'v'). */
        VIEW("View", "v"),

        /** Materialized view (relkind 'm'). */
        MATERIALIZED_VIEW("Materialized View", "m"),

        /** Sequence (relkind 'S'). */
        SEQUENCE("Sequence", "S"),

        /** Function (relkind 'f'). */
        FUNCTION("Function", "f"),

        /** Procedure (relkind 'p'). */
        PROCEDURE("Procedure", "p"),

        /** Schema (namespace). */
        SCHEMA("Schema", "n"),

        /** Database. */
        DATABASE("Database", "d"),

        /** Tablespace. */
        TABLESPACE("Tablespace", "t"),

        /** User-defined type. */
        TYPE("Type", "T"),

        /** Foreign data wrapper. */
        FOREIGN_DATA_WRAPPER("Foreign Data Wrapper", "F"),

        /** Foreign server. */
        FOREIGN_SERVER("Foreign Server", "s"),

        /** Procedural language. */
        LANGUAGE("Language", "l");

        /** Human-readable name for display purposes. */
        private final String displayName;

        /** PostgreSQL ACL code used in system catalog queries. */
        private final String aclCode;

        /**
         * Constructs an ObjectType with the specified display name and ACL code.
         *
         * @param displayName the human-readable name
         * @param aclCode the PostgreSQL ACL code
         */
        ObjectType(String displayName, String aclCode) {
            this.displayName = displayName;
            this.aclCode = aclCode;
        }

        /**
         * Returns the human-readable display name for this object type.
         *
         * @return the display name (e.g., "Table", "Materialized View")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the PostgreSQL ACL code for this object type.
         * <p>
         * This code is used in system catalog queries and ACL string parsing.
         * </p>
         *
         * @return the ACL code (e.g., "r" for table, "v" for view)
         */
        public String getAclCode() {
            return aclCode;
        }

        /**
         * Converts a PostgreSQL {@code relkind} character to the corresponding ObjectType.
         * <p>
         * The {@code relkind} column in {@code pg_class} identifies the type of relation.
         * This method maps those characters to the appropriate ObjectType enum value.
         * </p>
         *
         * @param relkind the relkind character from {@code pg_class.relkind}
         * @return the corresponding ObjectType, or {@link #TABLE} if no match is found
         * @see <a href="https://www.postgresql.org/docs/current/catalog-pg-class.html">PostgreSQL pg_class</a>
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
     * Enumeration of PostgreSQL privileges that can be granted on database objects.
     * <p>
     * Each privilege corresponds to a specific operation allowed on a database object.
     * The privilege is represented by a display name (e.g., "SELECT") and an ACL code
     * character used in PostgreSQL's ACL string format.
     * </p>
     * <p>
     * Not all privileges apply to all object types. For example, SELECT applies to tables
     * and views, whilst EXECUTE applies to functions and procedures.
     * </p>
     *
     * @see <a href="https://www.postgresql.org/docs/current/sql-grant.html">PostgreSQL GRANT</a>
     */
    public enum Privilege {
        /** Permission to read data (ACL code 'r'). */
        SELECT("SELECT", "r"),

        /** Permission to insert new rows (ACL code 'a' for "append"). */
        INSERT("INSERT", "a"),

        /** Permission to modify existing rows (ACL code 'w' for "write"). */
        UPDATE("UPDATE", "w"),

        /** Permission to delete rows (ACL code 'd'). */
        DELETE("DELETE", "d"),

        /** Permission to truncate a table (ACL code 'D'). */
        TRUNCATE("TRUNCATE", "D"),

        /** Permission to create foreign keys referencing this table (ACL code 'x'). */
        REFERENCES("REFERENCES", "x"),

        /** Permission to create triggers on this table (ACL code 't'). */
        TRIGGER("TRIGGER", "t"),

        /** Permission to create objects within a schema or database (ACL code 'C'). */
        CREATE("CREATE", "C"),

        /** Permission to connect to a database (ACL code 'c'). */
        CONNECT("CONNECT", "c"),

        /** Permission to create temporary tables in a database (ACL code 'T'). */
        TEMPORARY("TEMPORARY", "T"),

        /** Permission to execute a function or procedure (ACL code 'X'). */
        EXECUTE("EXECUTE", "X"),

        /** Permission to use a schema, sequence, or other object (ACL code 'U'). */
        USAGE("USAGE", "U"),

        /** Represents all privileges (ACL code '*'). */
        ALL("ALL", "*");

        /** Human-readable privilege name for display purposes. */
        private final String displayName;

        /** PostgreSQL ACL code character used in ACL strings. */
        private final String aclCode;

        /**
         * Constructs a Privilege with the specified display name and ACL code.
         *
         * @param displayName the human-readable privilege name
         * @param aclCode the PostgreSQL ACL code character
         */
        Privilege(String displayName, String aclCode) {
            this.displayName = displayName;
            this.aclCode = aclCode;
        }

        /**
         * Returns the human-readable privilege name.
         *
         * @return the display name (e.g., "SELECT", "INSERT")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the PostgreSQL ACL code character for this privilege.
         * <p>
         * This single-character code is used in PostgreSQL's ACL string format.
         * </p>
         *
         * @return the ACL code (e.g., "r" for SELECT, "w" for UPDATE)
         */
        public String getAclCode() {
            return aclCode;
        }

        /**
         * Parses a PostgreSQL ACL privilege string into a list of Privilege enums.
         * <p>
         * PostgreSQL stores ACL information in strings like "arwdDxt", where each
         * character represents a different privilege. This method converts such a
         * string into a list of Privilege enum values.
         * </p>
         * <p>
         * Example: {@code fromAclString("arw")} returns
         * {@code [INSERT, SELECT, UPDATE]}.
         * </p>
         *
         * @param aclString the ACL privilege string (e.g., "arwdDxt"), may be null
         * @return list of Privilege enums corresponding to the ACL string, empty list if null or empty
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

    /** The type of database object (table, view, function, etc.). */
    private ObjectType objectType;

    /** The schema containing the object, may be null for some object types. */
    private String schemaName;

    /** The name of the database object. */
    private String objectName;

    /** The role (user or group) receiving the permission. */
    private String grantee;

    /** The list of privileges granted to the grantee on this object. */
    private List<Privilege> privileges = new ArrayList<>();

    /** The role that granted the permission, may be null. */
    private String grantor;

    /** Whether the grantee can further grant these privileges to others (WITH GRANT OPTION). */
    private boolean grantOption;

    /**
     * Default constructor.
     * <p>
     * Creates an empty ObjectPermission instance with default values.
     * </p>
     */
    public ObjectPermission() {
    }

    /**
     * Constructs an ObjectPermission with the specified attributes.
     *
     * @param objectType the type of database object
     * @param schemaName the schema containing the object, may be null
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

    /**
     * Returns the type of database object.
     *
     * @return the object type, may be null
     * @see ObjectType
     */
    public ObjectType getObjectType() {
        return objectType;
    }

    /**
     * Sets the type of database object.
     *
     * @param objectType the object type to set
     */
    public void setObjectType(ObjectType objectType) {
        this.objectType = objectType;
    }

    /**
     * Returns the schema name containing the object.
     *
     * @return the schema name, may be null or empty
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Sets the schema name containing the object.
     *
     * @param schemaName the schema name to set
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Returns the name of the database object.
     *
     * @return the object name, may be null
     */
    public String getObjectName() {
        return objectName;
    }

    /**
     * Sets the name of the database object.
     *
     * @param objectName the object name to set
     */
    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    /**
     * Returns the role (user or group) receiving the permission.
     *
     * @return the grantee role name, may be null
     */
    public String getGrantee() {
        return grantee;
    }

    /**
     * Sets the role receiving the permission.
     *
     * @param grantee the grantee role name to set
     */
    public void setGrantee(String grantee) {
        this.grantee = grantee;
    }

    /**
     * Returns the list of privileges granted to the grantee.
     *
     * @return the list of privileges, never null but may be empty
     * @see Privilege
     */
    public List<Privilege> getPrivileges() {
        return privileges;
    }

    /**
     * Sets the list of privileges granted to the grantee.
     *
     * @param privileges the list of privileges to set
     */
    public void setPrivileges(List<Privilege> privileges) {
        this.privileges = privileges;
    }

    /**
     * Returns the role that granted the permission.
     *
     * @return the grantor role name, may be null
     */
    public String getGrantor() {
        return grantor;
    }

    /**
     * Sets the role that granted the permission.
     *
     * @param grantor the grantor role name to set
     */
    public void setGrantor(String grantor) {
        this.grantor = grantor;
    }

    /**
     * Checks whether the grantee can further grant these privileges to others.
     * <p>
     * This corresponds to the {@code WITH GRANT OPTION} clause in PostgreSQL GRANT statements.
     * </p>
     *
     * @return true if the grantee has the grant option, false otherwise
     * @see <a href="https://www.postgresql.org/docs/current/sql-grant.html">PostgreSQL GRANT</a>
     */
    public boolean isGrantOption() {
        return grantOption;
    }

    /**
     * Sets whether the grantee can further grant these privileges to others.
     *
     * @param grantOption true to allow the grantee to grant privileges, false otherwise
     */
    public void setGrantOption(boolean grantOption) {
        this.grantOption = grantOption;
    }

    // Helper Methods

    /**
     * Returns the fully qualified object name in schema.object format.
     * <p>
     * If the schema name is null or empty, returns only the object name.
     * Otherwise, returns the schema-qualified name.
     * </p>
     *
     * @return the fully qualified name (e.g., "public.users") or just the object name
     */
    public String getFullyQualifiedName() {
        if (schemaName == null || schemaName.isEmpty()) {
            return objectName;
        }
        return schemaName + "." + objectName;
    }

    /**
     * Returns a comma-separated list of privilege display names.
     * <p>
     * This method is useful for displaying privileges in user interfaces. If no
     * privileges are granted, returns "NONE".
     * </p>
     *
     * @return privilege names for display (e.g., "SELECT, INSERT, UPDATE"), or "NONE" if empty
     * @see Privilege#getDisplayName()
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
     * Checks whether this permission includes a specific privilege.
     * <p>
     * This is a convenience method to test for the presence of a particular
     * privilege without iterating through the privileges list.
     * </p>
     *
     * @param privilege the privilege to check for
     * @return true if the specified privilege is included, false otherwise
     */
    public boolean hasPrivilege(Privilege privilege) {
        return privileges != null && privileges.contains(privilege);
    }

    /**
     * Returns the Bootstrap CSS class for styling the privilege badge based on risk level.
     * <p>
     * This method categorises privileges by their potential impact:
     * </p>
     * <ul>
     * <li>Red (bg-danger): ALL privileges - highest risk</li>
     * <li>Yellow (bg-warning): DELETE or TRUNCATE - destructive operations</li>
     * <li>Blue (bg-info): INSERT or UPDATE - data modification</li>
     * <li>Grey (bg-secondary): SELECT, USAGE, and other read-only privileges</li>
     * </ul>
     *
     * @return Bootstrap CSS class name for the badge
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
