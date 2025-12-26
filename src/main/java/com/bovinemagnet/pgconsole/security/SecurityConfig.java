package com.bovinemagnet.pgconsole.security;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Security configuration helper for conditional security features.
 * <p>
 * Provides a centralised access point for checking security settings.
 * When security is enabled, HTTP Basic authentication is enforced via
 * Quarkus security configuration.
 * <p>
 * This class delegates to {@link InstanceConfig} for the actual configuration values.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see InstanceConfig.SecurityConfig
 */
@ApplicationScoped
public class SecurityConfig {

    @Inject
    InstanceConfig config;

    /**
     * Determines whether security is enabled for the application.
     * <p>
     * When security is enabled, endpoints require HTTP Basic authentication
     * and role-based access control is enforced for administrative operations.
     *
     * @return true if security is enabled, false otherwise
     */
    public boolean isSecurityEnabled() {
        return config.security().enabled();
    }
}
