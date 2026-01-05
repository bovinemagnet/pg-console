package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single rule from PostgreSQL's pg_hba.conf (Host-Based Authentication) configuration file.
 * <p>
 * This class models the data retrieved from the {@code pg_hba_file_rules} system view (available in PostgreSQL 10+),
 * which exposes the parsed contents of the pg_hba.conf file. Each rule defines how PostgreSQL authenticates
 * client connections based on connection type, database, user, source address, and authentication method.
 * </p>
 * <p>
 * The pg_hba.conf file controls which hosts are allowed to connect, which databases and users they can access,
 * and what authentication methods must be used. Rules are processed in order, and the first matching rule
 * determines the authentication behaviour.
 * </p>
 * <p>
 * This model includes helper methods for security assessment, identifying insecure authentication methods
 * (e.g., {@code trust}, {@code password}), overly permissive network rules, and configuration errors.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/auth-pg-hba-conf.html">PostgreSQL pg_hba.conf Documentation</a>
 * @see <a href="https://www.postgresql.org/docs/current/view-pg-hba-file-rules.html">pg_hba_file_rules View</a>
 * @since 0.0.0
 */
public class HbaRule {

    /**
     * The line number in the pg_hba.conf file where this rule appears.
     * <p>
     * Useful for correlating rules with the actual configuration file when troubleshooting
     * authentication issues or configuration errors.
     * </p>
     */
    private int lineNumber;

    /**
     * The connection type that this rule applies to.
     * <p>
     * Common values include:
     * <ul>
     * <li>{@code local} - Unix-domain socket connections</li>
     * <li>{@code host} - TCP/IP connections (both SSL and non-SSL)</li>
     * <li>{@code hostssl} - TCP/IP connections using SSL encryption</li>
     * <li>{@code hostnossl} - TCP/IP connections not using SSL</li>
     * <li>{@code hostgssenc} - TCP/IP connections using GSSAPI encryption</li>
     * <li>{@code hostnogssenc} - TCP/IP connections not using GSSAPI encryption</li>
     * </ul>
     * </p>
     */
    private String type;

    /**
     * List of database names this rule applies to.
     * <p>
     * Special values include {@code all} (all databases), {@code sameuser} (database with same name as user),
     * {@code samerole} (database name matches role name), and {@code replication} (replication connections).
     * Empty list typically represents "all" in display contexts.
     * </p>
     */
    private List<String> databases = new ArrayList<>();

    /**
     * List of user/role names this rule applies to.
     * <p>
     * Special values include {@code all} (all users) and role group names prefixed with {@code +}.
     * Empty list typically represents "all" in display contexts.
     * </p>
     */
    private List<String> users = new ArrayList<>();

    /**
     * The client IP address or hostname pattern this rule applies to.
     * <p>
     * Can be a specific IP (e.g., {@code 192.168.1.100}), CIDR notation (e.g., {@code 192.168.1.0/24}),
     * hostname, or the special value {@code all}. Not applicable for {@code local} connection types.
     * IPv6 addresses are also supported (e.g., {@code ::1} for localhost).
     * </p>
     */
    private String address;

    /**
     * The network mask for the address (when specified separately).
     * <p>
     * In older PostgreSQL configurations or specific cases, the netmask may be specified separately
     * from the address rather than using CIDR notation. May be null if CIDR notation is used or
     * for exact IP matches.
     * </p>
     */
    private String netmask;

    /**
     * The authentication method required by this rule.
     * <p>
     * Common authentication methods include:
     * <ul>
     * <li>{@code trust} - Allow connection unconditionally (insecure)</li>
     * <li>{@code reject} - Reject connection unconditionally</li>
     * <li>{@code scram-sha-256} - Challenge-response authentication with SCRAM-SHA-256 (recommended)</li>
     * <li>{@code md5} - Challenge-response authentication with MD5 (deprecated, less secure)</li>
     * <li>{@code password} - Clear-text password authentication (insecure)</li>
     * <li>{@code peer} - Obtain OS username and use as database username (Unix only)</li>
     * <li>{@code ident} - Obtain client's OS username from ident server</li>
     * <li>{@code cert} - SSL client certificate authentication</li>
     * <li>{@code ldap} - LDAP authentication</li>
     * <li>{@code radius} - RADIUS authentication</li>
     * <li>{@code pam} - PAM (Pluggable Authentication Modules)</li>
     * <li>{@code gss} - GSSAPI authentication (e.g., Kerberos)</li>
     * <li>{@code sspi} - SSPI authentication (Windows)</li>
     * </ul>
     * </p>
     */
    private String authMethod;

    /**
     * Additional authentication options specific to the authentication method.
     * <p>
     * These are method-specific parameters in the form {@code name=value}. For example,
     * LDAP authentication might include options like {@code ldapserver=ldap.example.com}
     * or {@code ldapprefix=cn=} and {@code ldapsuffix=,dc=example,dc=com}.
     * </p>
     */
    private List<String> options = new ArrayList<>();

    /**
     * Configuration error message if the rule is invalid or malformed.
     * <p>
     * When PostgreSQL detects a syntax error or invalid configuration in a pg_hba.conf rule,
     * the error message is stored here. Null or empty indicates a valid rule.
     * Rules with errors are typically not applied and should be corrected.
     * </p>
     */
    private String error;

    /**
     * Constructs a new HbaRule with default values.
     * <p>
     * All list fields are initialised to empty ArrayLists. Other fields are set to their default values
     * (0 for lineNumber, null for String fields).
     * </p>
     */
    public HbaRule() {
    }

    /**
     * Constructs an HbaRule with the specified core attributes.
     * <p>
     * This constructor sets the primary fields that define a pg_hba.conf rule. The {@code netmask},
     * {@code options}, and {@code error} fields are left at their default values and can be set
     * separately if needed.
     * </p>
     *
     * @param lineNumber the line number in pg_hba.conf where this rule appears
     * @param type       the connection type (e.g., "local", "host", "hostssl", "hostnossl")
     * @param databases  the list of database names this rule applies to (empty list typically means "all")
     * @param users      the list of user/role names this rule applies to (empty list typically means "all")
     * @param address    the client IP address or hostname pattern (not used for "local" type)
     * @param authMethod the authentication method (e.g., "trust", "md5", "scram-sha-256", "peer")
     */
    public HbaRule(int lineNumber, String type, List<String> databases,
                   List<String> users, String address, String authMethod) {
        this.lineNumber = lineNumber;
        this.type = type;
        this.databases = databases;
        this.users = users;
        this.address = address;
        this.authMethod = authMethod;
    }

    // Getters and Setters

    /**
     * Returns the line number in pg_hba.conf where this rule appears.
     *
     * @return the line number (1-based)
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Sets the line number in pg_hba.conf where this rule appears.
     *
     * @param lineNumber the line number (1-based)
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Returns the connection type that this rule applies to.
     *
     * @return the connection type (e.g., "local", "host", "hostssl", "hostnossl")
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the connection type that this rule applies to.
     *
     * @param type the connection type (e.g., "local", "host", "hostssl", "hostnossl")
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the list of database names this rule applies to.
     * <p>
     * May contain special values like "all", "sameuser", "samerole", or "replication".
     * An empty list typically represents "all" in display contexts.
     * </p>
     *
     * @return the list of database names, never null
     */
    public List<String> getDatabases() {
        return databases;
    }

    /**
     * Sets the list of database names this rule applies to.
     *
     * @param databases the list of database names
     */
    public void setDatabases(List<String> databases) {
        this.databases = databases;
    }

    /**
     * Returns the list of user/role names this rule applies to.
     * <p>
     * May contain special values like "all" or role groups prefixed with "+".
     * An empty list typically represents "all" in display contexts.
     * </p>
     *
     * @return the list of user/role names, never null
     */
    public List<String> getUsers() {
        return users;
    }

    /**
     * Sets the list of user/role names this rule applies to.
     *
     * @param users the list of user/role names
     */
    public void setUsers(List<String> users) {
        this.users = users;
    }

    /**
     * Returns the client IP address or hostname pattern this rule applies to.
     * <p>
     * Not applicable for "local" connection types. May be null.
     * </p>
     *
     * @return the address pattern, or null if not applicable
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the client IP address or hostname pattern this rule applies to.
     *
     * @param address the address pattern
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Returns the network mask for the address, if specified separately.
     * <p>
     * May be null if CIDR notation is used or for exact IP matches.
     * </p>
     *
     * @return the netmask, or null if not specified
     */
    public String getNetmask() {
        return netmask;
    }

    /**
     * Sets the network mask for the address.
     *
     * @param netmask the netmask
     */
    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    /**
     * Returns the authentication method required by this rule.
     *
     * @return the authentication method (e.g., "trust", "md5", "scram-sha-256"), or null if not set
     */
    public String getAuthMethod() {
        return authMethod;
    }

    /**
     * Sets the authentication method required by this rule.
     *
     * @param authMethod the authentication method
     */
    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    /**
     * Returns the additional authentication options specific to the authentication method.
     * <p>
     * Options are in the form "name=value" and vary by authentication method.
     * </p>
     *
     * @return the list of options, never null
     */
    public List<String> getOptions() {
        return options;
    }

    /**
     * Sets the additional authentication options.
     *
     * @param options the list of options
     */
    public void setOptions(List<String> options) {
        this.options = options;
    }

    /**
     * Returns the configuration error message if the rule is invalid.
     *
     * @return the error message, or null if the rule is valid
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the configuration error message.
     *
     * @param error the error message
     */
    public void setError(String error) {
        this.error = error;
    }

    // Helper Methods

    /**
     * Returns the Bootstrap CSS class for styling the authentication method badge.
     * <p>
     * This method maps authentication methods to appropriate visual indicators based on their
     * security characteristics:
     * </p>
     * <ul>
     * <li>{@code bg-danger} (red) - Highly insecure methods like "trust"</li>
     * <li>{@code bg-warning} (yellow) - Weak or deprecated methods like "password" and "md5"</li>
     * <li>{@code bg-success} (green) - Strong methods like "scram-sha-256" and "cert"</li>
     * <li>{@code bg-info} (blue) - External authentication methods like "peer", "ldap", "gss"</li>
     * <li>{@code bg-dark} (dark) - Rejection rules</li>
     * <li>{@code bg-secondary} (grey) - Unknown or null methods</li>
     * </ul>
     *
     * @return the Bootstrap CSS class name for the authentication method
     */
    public String getAuthMethodCssClass() {
        if (authMethod == null) {
            return "bg-secondary";
        }
        return switch (authMethod.toLowerCase()) {
            case "trust" -> "bg-danger";
            case "password" -> "bg-warning text-dark";
            case "md5" -> "bg-warning text-dark";
            case "scram-sha-256" -> "bg-success";
            case "peer", "ident" -> "bg-info";
            case "cert" -> "bg-success";
            case "gss", "sspi" -> "bg-info";
            case "ldap" -> "bg-info";
            case "radius" -> "bg-info";
            case "pam" -> "bg-info";
            case "reject" -> "bg-dark";
            default -> "bg-secondary";
        };
    }

    /**
     * Determines whether this rule uses an insecure authentication method.
     * <p>
     * A rule is considered insecure if it uses:
     * </p>
     * <ul>
     * <li>{@code trust} - Allows connection without any authentication</li>
     * <li>{@code password} - Sends passwords in clear text over the network</li>
     * </ul>
     * <p>
     * These methods should generally be avoided in production environments, especially
     * when combined with permissive network rules.
     * </p>
     *
     * @return {@code true} if the authentication method is "trust" or "password", {@code false} otherwise
     * @see #isWideOpen()
     * @see #getSecurityLevel()
     */
    public boolean isInsecure() {
        if (authMethod == null) {
            return false;
        }
        String method = authMethod.toLowerCase();
        return method.equals("trust") || method.equals("password");
    }

    /**
     * Determines whether this rule allows access from any network address.
     * <p>
     * A rule is considered "wide open" if it accepts connections from:
     * </p>
     * <ul>
     * <li>{@code all} - Special keyword allowing any address</li>
     * <li>{@code 0.0.0.0/0} - IPv4 wildcard representing any IPv4 address</li>
     * <li>{@code ::/0} - IPv6 wildcard representing any IPv6 address</li>
     * </ul>
     * <p>
     * Wide open rules combined with weak authentication methods represent a significant security risk.
     * </p>
     *
     * @return {@code true} if the rule accepts connections from any address, {@code false} otherwise
     * @see #isInsecure()
     * @see #getSecurityLevel()
     */
    public boolean isWideOpen() {
        if (address == null) {
            return false;
        }
        return address.equals("all") ||
               address.equals("0.0.0.0/0") ||
               address.equals("::/0");
    }

    /**
     * Determines whether this rule has a configuration error.
     * <p>
     * When PostgreSQL parses pg_hba.conf, syntax errors or invalid configurations are recorded
     * in the error field. Rules with errors are typically ignored and will not be applied.
     * </p>
     *
     * @return {@code true} if an error message is present, {@code false} if the rule is valid
     * @see #getError()
     */
    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    /**
     * Returns a formatted, human-readable string of database names this rule applies to.
     * <p>
     * If the databases list is null or empty, returns "all" to indicate the rule applies
     * to all databases. Otherwise, returns the database names joined with commas and spaces.
     * </p>
     *
     * @return comma-separated database names, or "all" if empty
     */
    public String getDatabasesDisplay() {
        if (databases == null || databases.isEmpty()) {
            return "all";
        }
        return String.join(", ", databases);
    }

    /**
     * Returns a formatted, human-readable string of user/role names this rule applies to.
     * <p>
     * If the users list is null or empty, returns "all" to indicate the rule applies
     * to all users. Otherwise, returns the user names joined with commas and spaces.
     * </p>
     *
     * @return comma-separated user/role names, or "all" if empty
     */
    public String getUsersDisplay() {
        if (users == null || users.isEmpty()) {
            return "all";
        }
        return String.join(", ", users);
    }

    /**
     * Returns a formatted, human-readable string of authentication options.
     * <p>
     * If the options list is null or empty, returns "-" to indicate no options are configured.
     * Otherwise, returns the options joined with commas and spaces.
     * </p>
     *
     * @return comma-separated authentication options, or "-" if empty
     */
    public String getOptionsDisplay() {
        if (options == null || options.isEmpty()) {
            return "-";
        }
        return String.join(", ", options);
    }

    /**
     * Returns a formatted, human-readable address string for display.
     * <p>
     * The formatting varies based on the connection type and available data:
     * </p>
     * <ul>
     * <li>For "local" type connections: returns "local socket"</li>
     * <li>If address is null or empty: returns "-"</li>
     * <li>If both address and netmask are present: returns "address/netmask"</li>
     * <li>Otherwise: returns the address as-is</li>
     * </ul>
     *
     * @return formatted address string suitable for display in UI
     */
    public String getAddressDisplay() {
        if (type != null && type.equals("local")) {
            return "local socket";
        }
        if (address == null || address.isEmpty()) {
            return "-";
        }
        if (netmask != null && !netmask.isEmpty()) {
            return address + "/" + netmask;
        }
        return address;
    }

    /**
     * Assesses the overall security risk level of this rule.
     * <p>
     * The security level is determined by combining the authentication method strength
     * with the network scope of the rule:
     * </p>
     * <ul>
     * <li>{@code Critical} - Insecure authentication (trust/password) combined with wide-open network access</li>
     * <li>{@code High Risk} - Insecure authentication with limited network scope</li>
     * <li>{@code Medium Risk} - MD5 authentication (deprecated but better than clear-text)</li>
     * <li>{@code Secure} - Strong authentication methods (scram-sha-256, cert, etc.)</li>
     * </ul>
     *
     * @return security level description ("Critical", "High Risk", "Medium Risk", or "Secure")
     * @see #isInsecure()
     * @see #isWideOpen()
     * @see #getSecurityLevelCssClass()
     */
    public String getSecurityLevel() {
        if (isInsecure()) {
            if (isWideOpen()) {
                return "Critical";
            }
            return "High Risk";
        }
        if (authMethod != null && authMethod.equalsIgnoreCase("md5")) {
            return "Medium Risk";
        }
        return "Secure";
    }

    /**
     * Returns the Bootstrap CSS class for styling the security level indicator.
     * <p>
     * Maps the security level assessment to appropriate Bootstrap background colours:
     * </p>
     * <ul>
     * <li>{@code bg-danger} (red) - Critical security issues</li>
     * <li>{@code bg-warning} (yellow) - High risk configurations</li>
     * <li>{@code bg-info} (blue) - Medium risk configurations</li>
     * <li>{@code bg-success} (green) - Secure configurations</li>
     * </ul>
     *
     * @return the Bootstrap CSS class name for the security level
     * @see #getSecurityLevel()
     */
    public String getSecurityLevelCssClass() {
        String level = getSecurityLevel();
        return switch (level) {
            case "Critical" -> "bg-danger";
            case "High Risk" -> "bg-warning text-dark";
            case "Medium Risk" -> "bg-info";
            default -> "bg-success";
        };
    }
}
