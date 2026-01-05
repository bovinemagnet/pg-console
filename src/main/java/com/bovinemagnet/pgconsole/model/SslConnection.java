package com.bovinemagnet.pgconsole.model;

/**
 * Represents SSL/TLS connection information from the PostgreSQL {@code pg_stat_ssl} system view.
 * <p>
 * This model captures the encryption status and security characteristics of active database
 * connections. It provides visibility into which connections are using SSL/TLS, the strength
 * of their encryption, and certificate details when client certificate authentication is enabled.
 * </p>
 * <p>
 * The data is sourced by joining {@code pg_stat_ssl} with {@code pg_stat_activity} to correlate
 * SSL information with connection metadata such as username, database, and application name.
 * </p>
 * <p>
 * This class includes helper methods for security assessment, such as classifying encryption
 * strength and identifying connection sources (local, internal network, or external).
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/monitoring-stats.html#MONITORING-PG-STAT-SSL-VIEW">PostgreSQL pg_stat_ssl Documentation</a>
 */
public class SslConnection {

    /**
     * The process ID of the backend serving this connection.
     * <p>
     * Corresponds to {@code pg_stat_activity.pid} and can be used to correlate
     * with other monitoring views or to terminate the connection if needed.
     * </p>
     */
    private int pid;

    /**
     * The database user name of this connection.
     * <p>
     * Retrieved from {@code pg_stat_activity.usename}.
     * </p>
     */
    private String username;

    /**
     * The IP address of the client connected to this backend.
     * <p>
     * Retrieved from {@code pg_stat_activity.client_addr}. May be null or empty
     * for local socket connections.
     * </p>
     */
    private String clientAddr;

    /**
     * The name of the application connected to this backend.
     * <p>
     * Retrieved from {@code pg_stat_activity.application_name}. This is typically
     * set by the client application via the connection string or JDBC properties.
     * </p>
     */
    private String applicationName;

    /**
     * Indicates whether SSL is in use for this connection.
     * <p>
     * Corresponds to {@code pg_stat_ssl.ssl}. When {@code false}, all other SSL-related
     * fields will be null or zero.
     * </p>
     */
    private boolean ssl;

    /**
     * The SSL/TLS protocol version in use.
     * <p>
     * Corresponds to {@code pg_stat_ssl.version}. Common values include "TLSv1.2",
     * "TLSv1.3", etc. Null if SSL is not in use.
     * </p>
     */
    private String sslVersion;

    /**
     * The name of the SSL cipher suite in use.
     * <p>
     * Corresponds to {@code pg_stat_ssl.cipher}. Examples include
     * "ECDHE-RSA-AES256-GCM-SHA384" or "TLS_AES_256_GCM_SHA384". Null if SSL is not in use.
     * </p>
     */
    private String cipher;

    /**
     * The number of bits in the encryption key.
     * <p>
     * Corresponds to {@code pg_stat_ssl.bits}. Common values are 128, 192, or 256.
     * Zero if SSL is not in use. Higher values indicate stronger encryption.
     * </p>
     */
    private int bits;

    /**
     * The Distinguished Name (DN) of the client certificate.
     * <p>
     * Corresponds to {@code pg_stat_ssl.client_dn}. Only populated when client
     * certificate authentication is configured and the client presents a certificate.
     * Null otherwise.
     * </p>
     */
    private String clientDn;

    /**
     * The Distinguished Name (DN) of the issuer of the client certificate.
     * <p>
     * Corresponds to {@code pg_stat_ssl.issuer_dn}. Only populated when client
     * certificate authentication is configured and the client presents a certificate.
     * Null otherwise.
     * </p>
     */
    private String issuerDn;

    /**
     * The current state of the backend connection.
     * <p>
     * Retrieved from {@code pg_stat_activity.state}. Common values include "active",
     * "idle", "idle in transaction", etc.
     * </p>
     */
    private String state;

    /**
     * The name of the database this connection is connected to.
     * <p>
     * Retrieved from {@code pg_stat_activity.datname}.
     * </p>
     */
    private String database;

    /**
     * Default constructor.
     * <p>
     * Creates an empty SslConnection instance. All fields will have their default
     * values (zero for numeric types, null for object types, false for boolean).
     * </p>
     */
    public SslConnection() {
    }

    /**
     * Constructs an SslConnection with the specified SSL and connection attributes.
     * <p>
     * This constructor is typically used when mapping result sets from queries
     * that join {@code pg_stat_ssl} and {@code pg_stat_activity}.
     * </p>
     *
     * @param pid             the process ID of the backend serving this connection
     * @param username        the database user name of this connection
     * @param clientAddr      the IP address of the client, may be null for local sockets
     * @param applicationName the name of the application connected to this backend
     * @param ssl             {@code true} if SSL is in use for this connection, {@code false} otherwise
     * @param sslVersion      the SSL/TLS protocol version (e.g., "TLSv1.3"), null if SSL is not in use
     * @param cipher          the name of the SSL cipher suite in use, null if SSL is not in use
     * @param bits            the number of bits in the encryption key, zero if SSL is not in use
     */
    public SslConnection(int pid, String username, String clientAddr,
                         String applicationName, boolean ssl, String sslVersion,
                         String cipher, int bits) {
        this.pid = pid;
        this.username = username;
        this.clientAddr = clientAddr;
        this.applicationName = applicationName;
        this.ssl = ssl;
        this.sslVersion = sslVersion;
        this.cipher = cipher;
        this.bits = bits;
    }

    // Getters and Setters

    /**
     * Returns the process ID of the backend serving this connection.
     *
     * @return the backend process ID
     */
    public int getPid() {
        return pid;
    }

    /**
     * Sets the process ID of the backend serving this connection.
     *
     * @param pid the backend process ID to set
     */
    public void setPid(int pid) {
        this.pid = pid;
    }

    /**
     * Returns the database user name of this connection.
     *
     * @return the username, may be null
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the database user name of this connection.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the IP address of the client connected to this backend.
     *
     * @return the client IP address, may be null or empty for local socket connections
     */
    public String getClientAddr() {
        return clientAddr;
    }

    /**
     * Sets the IP address of the client connected to this backend.
     *
     * @param clientAddr the client IP address to set
     */
    public void setClientAddr(String clientAddr) {
        this.clientAddr = clientAddr;
    }

    /**
     * Returns the name of the application connected to this backend.
     *
     * @return the application name, may be null if not set by the client
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Sets the name of the application connected to this backend.
     *
     * @param applicationName the application name to set
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Indicates whether SSL is in use for this connection.
     *
     * @return {@code true} if SSL is enabled, {@code false} otherwise
     */
    public boolean isSsl() {
        return ssl;
    }

    /**
     * Sets whether SSL is in use for this connection.
     *
     * @param ssl {@code true} to indicate SSL is enabled, {@code false} otherwise
     */
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    /**
     * Returns the SSL/TLS protocol version in use.
     *
     * @return the SSL/TLS version (e.g., "TLSv1.3"), null if SSL is not in use
     */
    public String getSslVersion() {
        return sslVersion;
    }

    /**
     * Sets the SSL/TLS protocol version in use.
     *
     * @param sslVersion the SSL/TLS version to set
     */
    public void setSslVersion(String sslVersion) {
        this.sslVersion = sslVersion;
    }

    /**
     * Returns the name of the SSL cipher suite in use.
     *
     * @return the cipher suite name, null if SSL is not in use
     */
    public String getCipher() {
        return cipher;
    }

    /**
     * Sets the name of the SSL cipher suite in use.
     *
     * @param cipher the cipher suite name to set
     */
    public void setCipher(String cipher) {
        this.cipher = cipher;
    }

    /**
     * Returns the number of bits in the encryption key.
     *
     * @return the key length in bits, zero if SSL is not in use
     */
    public int getBits() {
        return bits;
    }

    /**
     * Sets the number of bits in the encryption key.
     *
     * @param bits the key length in bits to set
     */
    public void setBits(int bits) {
        this.bits = bits;
    }

    /**
     * Returns the Distinguished Name (DN) of the client certificate.
     *
     * @return the client certificate DN, null if not using client certificate authentication
     */
    public String getClientDn() {
        return clientDn;
    }

    /**
     * Sets the Distinguished Name (DN) of the client certificate.
     *
     * @param clientDn the client certificate DN to set
     */
    public void setClientDn(String clientDn) {
        this.clientDn = clientDn;
    }

    /**
     * Returns the Distinguished Name (DN) of the issuer of the client certificate.
     *
     * @return the certificate issuer DN, null if not using client certificate authentication
     */
    public String getIssuerDn() {
        return issuerDn;
    }

    /**
     * Sets the Distinguished Name (DN) of the issuer of the client certificate.
     *
     * @param issuerDn the certificate issuer DN to set
     */
    public void setIssuerDn(String issuerDn) {
        this.issuerDn = issuerDn;
    }

    /**
     * Returns the current state of the backend connection.
     *
     * @return the connection state (e.g., "active", "idle"), may be null
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the current state of the backend connection.
     *
     * @param state the connection state to set
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Returns the name of the database this connection is connected to.
     *
     * @return the database name, may be null
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Sets the name of the database this connection is connected to.
     *
     * @param database the database name to set
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    // Helper Methods

    /**
     * Returns a security level classification based on encryption strength.
     * <p>
     * The security level is determined by the number of bits in the encryption key:
     * </p>
     * <ul>
     *     <li>"None" - SSL is not in use</li>
     *     <li>"Weak" - SSL is enabled but key length is less than 128 bits</li>
     *     <li>"Good" - Key length is 128-255 bits</li>
     *     <li>"Strong" - Key length is 256 bits or more</li>
     * </ul>
     *
     * @return the security level description: "None", "Weak", "Good", or "Strong"
     * @see #getBits()
     * @see #getSecurityLevelCssClass()
     */
    public String getSecurityLevelBadge() {
        if (!ssl) {
            return "None";
        }
        if (bits >= 256) {
            return "Strong";
        }
        if (bits >= 128) {
            return "Good";
        }
        return "Weak";
    }

    /**
     * Returns the Bootstrap CSS class corresponding to the security level.
     * <p>
     * This method provides CSS classes for visual representation of security levels:
     * </p>
     * <ul>
     *     <li>"bg-danger" - No SSL (critical)</li>
     *     <li>"bg-warning text-dark" - Weak encryption (needs attention)</li>
     *     <li>"bg-info" - Good encryption (acceptable)</li>
     *     <li>"bg-success" - Strong encryption (optimal)</li>
     * </ul>
     *
     * @return the Bootstrap background CSS class for the security level badge
     * @see #getSecurityLevelBadge()
     */
    public String getSecurityLevelCssClass() {
        if (!ssl) {
            return "bg-danger";
        }
        if (bits >= 256) {
            return "bg-success";
        }
        if (bits >= 128) {
            return "bg-info";
        }
        return "bg-warning text-dark";
    }

    /**
     * Returns a human-readable SSL status description.
     * <p>
     * Provides a simple text label indicating whether the connection is encrypted.
     * </p>
     *
     * @return "Encrypted" if SSL is in use, "Unencrypted" otherwise
     * @see #isSsl()
     * @see #getSslStatusCssClass()
     */
    public String getSslStatusBadge() {
        return ssl ? "Encrypted" : "Unencrypted";
    }

    /**
     * Returns the Bootstrap CSS class for the SSL status badge.
     * <p>
     * Provides visual styling to highlight encrypted (green) versus unencrypted (red) connections.
     * </p>
     *
     * @return "bg-success" if encrypted, "bg-danger" if unencrypted
     * @see #getSslStatusBadge()
     */
    public String getSslStatusCssClass() {
        return ssl ? "bg-success" : "bg-danger";
    }

    /**
     * Classifies the connection source based on the client IP address.
     * <p>
     * Determines whether the connection originates from a local socket, internal network,
     * or external source by examining the client address:
     * </p>
     * <ul>
     *     <li><strong>Local</strong> - Unix socket connection (null/empty address) or loopback
     *         addresses (127.0.0.0/8, ::1)</li>
     *     <li><strong>Internal</strong> - RFC 1918 private IP ranges (10.0.0.0/8, 172.16.0.0/12,
     *         192.168.0.0/16)</li>
     *     <li><strong>External</strong> - Public routable IP addresses</li>
     * </ul>
     *
     * @return "Local" for socket/loopback, "Internal" for private networks, "External" for public IPs
     * @see #getConnectionSourceCssClass()
     * @see #getClientAddr()
     */
    public String getConnectionSource() {
        if (clientAddr == null || clientAddr.isEmpty()) {
            return "Local";
        }
        if (clientAddr.startsWith("127.") || clientAddr.equals("::1")) {
            return "Local";
        }
        if (clientAddr.startsWith("10.") ||
            clientAddr.startsWith("172.16.") ||
            clientAddr.startsWith("172.17.") ||
            clientAddr.startsWith("172.18.") ||
            clientAddr.startsWith("172.19.") ||
            clientAddr.startsWith("172.2") ||
            clientAddr.startsWith("172.30.") ||
            clientAddr.startsWith("172.31.") ||
            clientAddr.startsWith("192.168.")) {
            return "Internal";
        }
        return "External";
    }

    /**
     * Returns the Bootstrap CSS class for the connection source classification.
     * <p>
     * Provides visual styling to differentiate connection origins:
     * </p>
     * <ul>
     *     <li>"bg-secondary" - Local connections (neutral grey)</li>
     *     <li>"bg-info" - Internal network connections (informational blue)</li>
     *     <li>"bg-warning text-dark" - External connections (warning yellow, may require stronger security)</li>
     * </ul>
     *
     * @return the Bootstrap background CSS class for the connection source badge
     * @see #getConnectionSource()
     */
    public String getConnectionSourceCssClass() {
        String source = getConnectionSource();
        return switch (source) {
            case "Local" -> "bg-secondary";
            case "Internal" -> "bg-info";
            case "External" -> "bg-warning text-dark";
            default -> "bg-secondary";
        };
    }

    /**
     * Returns a display-friendly representation of the client address.
     * <p>
     * Converts null or empty client addresses to a more readable format for UI presentation.
     * </p>
     *
     * @return the client IP address, or "Local Socket" if the address is null or empty
     * @see #getClientAddr()
     */
    public String getClientAddrDisplay() {
        if (clientAddr == null || clientAddr.isEmpty()) {
            return "Local Socket";
        }
        return clientAddr;
    }

    /**
     * Returns a formatted string describing the encryption configuration.
     * <p>
     * Combines the cipher suite name and key length into a single display string
     * suitable for presenting in dashboards and reports.
     * </p>
     *
     * @return a string in the format "cipher (bits bits)", or "None" if SSL is not in use or cipher is null
     * @see #getCipher()
     * @see #getBits()
     */
    public String getEncryptionDetails() {
        if (!ssl || cipher == null) {
            return "None";
        }
        return cipher + " (" + bits + " bits)";
    }
}
