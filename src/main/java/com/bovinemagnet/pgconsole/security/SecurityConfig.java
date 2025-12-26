package com.bovinemagnet.pgconsole.security;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Security configuration helper for conditional security features.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class SecurityConfig {

    @Inject
    InstanceConfig config;

    /**
     * Returns whether security is enabled.
     *
     * @return true if security is enabled
     */
    public boolean isSecurityEnabled() {
        return config.security().enabled();
    }
}
