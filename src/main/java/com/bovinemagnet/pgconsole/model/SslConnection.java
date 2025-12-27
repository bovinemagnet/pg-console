package com.bovinemagnet.pgconsole.model;

/**
 * Represents SSL/TLS connection information from pg_stat_ssl.
 * <p>
 * Contains details about the SSL status and configuration of
 * active database connections.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class SslConnection {

    private int pid;
    private String username;
    private String clientAddr;
    private String applicationName;
    private boolean ssl;
    private String sslVersion;
    private String cipher;
    private int bits;
    private String clientDn;
    private String issuerDn;
    private String state;
    private String database;

    /**
     * Default constructor.
     */
    public SslConnection() {
    }

    /**
     * Constructs an SslConnection with the specified attributes.
     *
     * @param pid             the process ID
     * @param username        the connected username
     * @param clientAddr      the client IP address
     * @param applicationName the application name
     * @param ssl             whether SSL is enabled
     * @param sslVersion      the SSL/TLS version
     * @param cipher          the cipher suite
     * @param bits            the encryption key length
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

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClientAddr() {
        return clientAddr;
    }

    public void setClientAddr(String clientAddr) {
        this.clientAddr = clientAddr;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getSslVersion() {
        return sslVersion;
    }

    public void setSslVersion(String sslVersion) {
        this.sslVersion = sslVersion;
    }

    public String getCipher() {
        return cipher;
    }

    public void setCipher(String cipher) {
        this.cipher = cipher;
    }

    public int getBits() {
        return bits;
    }

    public void setBits(int bits) {
        this.bits = bits;
    }

    public String getClientDn() {
        return clientDn;
    }

    public void setClientDn(String clientDn) {
        this.clientDn = clientDn;
    }

    public String getIssuerDn() {
        return issuerDn;
    }

    public void setIssuerDn(String issuerDn) {
        this.issuerDn = issuerDn;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    // Helper Methods

    /**
     * Returns a security level badge based on encryption strength.
     * <p>
     * Evaluates SSL version and cipher strength to determine
     * the overall security level.
     *
     * @return security level description
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
     * Returns the CSS class for the security level badge.
     *
     * @return CSS class name
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
     * Returns a badge for the SSL status.
     *
     * @return SSL status description
     */
    public String getSslStatusBadge() {
        return ssl ? "Encrypted" : "Unencrypted";
    }

    /**
     * Returns the CSS class for the SSL status badge.
     *
     * @return CSS class name
     */
    public String getSslStatusCssClass() {
        return ssl ? "bg-success" : "bg-danger";
    }

    /**
     * Returns the connection source classification.
     *
     * @return "Local", "Internal", or "External"
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
     * Returns the CSS class for the connection source.
     *
     * @return CSS class name
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
     * Returns the display string for the client address.
     *
     * @return client address or "Local Socket" if empty
     */
    public String getClientAddrDisplay() {
        if (clientAddr == null || clientAddr.isEmpty()) {
            return "Local Socket";
        }
        return clientAddr;
    }

    /**
     * Returns the encryption details for display.
     *
     * @return cipher and bits, or "None" if not encrypted
     */
    public String getEncryptionDetails() {
        if (!ssl || cipher == null) {
            return "None";
        }
        return cipher + " (" + bits + " bits)";
    }
}
