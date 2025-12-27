package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a pg_hba.conf rule from pg_hba_file_rules (PostgreSQL 10+).
 * <p>
 * Contains the parsed configuration for host-based authentication rules
 * that control client access to the database.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class HbaRule {

    private int lineNumber;
    private String type;
    private List<String> databases = new ArrayList<>();
    private List<String> users = new ArrayList<>();
    private String address;
    private String netmask;
    private String authMethod;
    private List<String> options = new ArrayList<>();
    private String error;

    /**
     * Default constructor.
     */
    public HbaRule() {
    }

    /**
     * Constructs an HbaRule with the specified attributes.
     *
     * @param lineNumber the line number in pg_hba.conf
     * @param type       the connection type (local, host, hostssl, etc.)
     * @param databases  the list of databases this rule applies to
     * @param users      the list of users this rule applies to
     * @param address    the client address pattern
     * @param authMethod the authentication method
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

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getDatabases() {
        return databases;
    }

    public void setDatabases(List<String> databases) {
        this.databases = databases;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    // Helper Methods

    /**
     * Returns the CSS class for the authentication method badge.
     *
     * @return CSS class based on auth method security
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
     * Checks if this rule uses an insecure authentication method.
     *
     * @return true if the auth method is considered insecure
     */
    public boolean isInsecure() {
        if (authMethod == null) {
            return false;
        }
        String method = authMethod.toLowerCase();
        return method.equals("trust") || method.equals("password");
    }

    /**
     * Checks if this rule allows access from all addresses.
     *
     * @return true if address is all or 0.0.0.0/0
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
     * Checks if this rule has a configuration error.
     *
     * @return true if there is an error
     */
    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    /**
     * Returns the databases as a comma-separated string.
     *
     * @return database list for display
     */
    public String getDatabasesDisplay() {
        if (databases == null || databases.isEmpty()) {
            return "all";
        }
        return String.join(", ", databases);
    }

    /**
     * Returns the users as a comma-separated string.
     *
     * @return user list for display
     */
    public String getUsersDisplay() {
        if (users == null || users.isEmpty()) {
            return "all";
        }
        return String.join(", ", users);
    }

    /**
     * Returns the options as a comma-separated string.
     *
     * @return options for display
     */
    public String getOptionsDisplay() {
        if (options == null || options.isEmpty()) {
            return "-";
        }
        return String.join(", ", options);
    }

    /**
     * Returns the address with netmask for display.
     *
     * @return formatted address
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
     * Returns the security level assessment for this rule.
     *
     * @return security level description
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
     * Returns the CSS class for the security level.
     *
     * @return CSS class name
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
