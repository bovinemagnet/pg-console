package com.bovinemagnet.pgconsole.logging;

import com.bovinemagnet.pgconsole.config.LoggingConfig;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

import java.io.IOException;
import java.security.Principal;
import java.util.UUID;

/**
 * JAX-RS filter that sets up MDC (Mapped Diagnostic Context) for request tracking.
 * <p>
 * Propagates correlation IDs, user context, instance names, and client IP addresses
 * into the logging context for comprehensive request tracing.
 * <p>
 * MDC fields set by this filter:
 * <ul>
 *   <li>correlationId - Unique request identifier (UUID)</li>
 *   <li>user - Authenticated user principal name</li>
 *   <li>instance - PostgreSQL instance being accessed</li>
 *   <li>clientIp - Client IP address</li>
 *   <li>method - HTTP method (GET, POST, etc.)</li>
 *   <li>path - Request URI path</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 100) // Run early in the filter chain
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_USER = "user";
    public static final String MDC_INSTANCE = "instance";
    public static final String MDC_CLIENT_IP = "clientIp";
    public static final String MDC_METHOD = "method";
    public static final String MDC_PATH = "path";
    public static final String MDC_REQUEST_START = "requestStartTime";

    private static final String RESPONSE_HEADER_CORRELATION_ID = "X-Correlation-ID";

    @Inject
    LoggingConfig loggingConfig;

    /**
     * Filters incoming requests to set up MDC context.
     *
     * @param requestContext the request context
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Generate or extract correlation ID
        String correlationId = extractOrGenerateCorrelationId(requestContext);

        // Store request start time for latency calculation
        long startTime = System.currentTimeMillis();
        requestContext.setProperty(MDC_REQUEST_START, startTime);

        // Set MDC values
        if (loggingConfig.mdcCorrelationIdEnabled()) {
            MDC.put(MDC_CORRELATION_ID, correlationId);
        }

        if (loggingConfig.mdcIncludeUser()) {
            String user = extractUser(requestContext);
            if (user != null) {
                MDC.put(MDC_USER, user);
            }
        }

        if (loggingConfig.mdcIncludeInstance()) {
            String instance = extractInstance(requestContext);
            if (instance != null) {
                MDC.put(MDC_INSTANCE, instance);
            }
        }

        if (loggingConfig.mdcIncludeClientIp()) {
            String clientIp = extractClientIp(requestContext);
            if (clientIp != null) {
                MDC.put(MDC_CLIENT_IP, clientIp);
            }
        }

        // Always set method and path for debugging
        MDC.put(MDC_METHOD, requestContext.getMethod());
        MDC.put(MDC_PATH, requestContext.getUriInfo().getPath());
    }

    /**
     * Filters outgoing responses to clean up MDC and add correlation ID header.
     *
     * @param requestContext the request context
     * @param responseContext the response context
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        try {
            // Add correlation ID to response headers
            String correlationId = (String) MDC.get(MDC_CORRELATION_ID);
            if (correlationId != null) {
                responseContext.getHeaders().add(RESPONSE_HEADER_CORRELATION_ID, correlationId);
            }

            // Log slow requests if performance logging is enabled
            if (loggingConfig.performanceLatencyLoggingEnabled()) {
                Long startTime = (Long) requestContext.getProperty(MDC_REQUEST_START);
                if (startTime != null) {
                    long duration = System.currentTimeMillis() - startTime;
                    if (duration > loggingConfig.performanceSlowThresholdMs()) {
                        // Slow request warning will be logged by StructuredLogger
                        MDC.put("duration_ms", String.valueOf(duration));
                    }
                }
            }
        } finally {
            // Clean up MDC to prevent memory leaks
            clearMdc();
        }
    }

    /**
     * Extracts correlation ID from request header or generates a new one.
     */
    private String extractOrGenerateCorrelationId(ContainerRequestContext requestContext) {
        String headerName = loggingConfig.mdcCorrelationIdHeader();
        String correlationId = requestContext.getHeaderString(headerName);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        return correlationId;
    }

    /**
     * Extracts authenticated user from security context.
     */
    private String extractUser(ContainerRequestContext requestContext) {
        SecurityContext securityContext = requestContext.getSecurityContext();
        if (securityContext != null) {
            Principal principal = securityContext.getUserPrincipal();
            if (principal != null) {
                return principal.getName();
            }
        }
        return "anonymous";
    }

    /**
     * Extracts instance name from request query parameter or path.
     */
    private String extractInstance(ContainerRequestContext requestContext) {
        // Try query parameter first
        String instance = requestContext.getUriInfo().getQueryParameters().getFirst("instance");
        if (instance != null && !instance.isBlank()) {
            return instance;
        }

        // Try path parameter (common in API routes)
        String path = requestContext.getUriInfo().getPath();
        if (path.contains("/api/") && path.contains("/instance/")) {
            int start = path.indexOf("/instance/") + 10;
            int end = path.indexOf('/', start);
            if (end == -1) {
                end = path.length();
            }
            return path.substring(start, end);
        }

        return "default";
    }

    /**
     * Extracts client IP address from request headers or remote address.
     */
    private String extractClientIp(ContainerRequestContext requestContext) {
        // Check for forwarded headers (common with proxies/load balancers)
        String forwardedFor = requestContext.getHeaderString("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // Take the first IP in the chain (original client)
            int commaIndex = forwardedFor.indexOf(',');
            if (commaIndex > 0) {
                return forwardedFor.substring(0, commaIndex).trim();
            }
            return forwardedFor.trim();
        }

        String realIp = requestContext.getHeaderString("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        // Fallback to direct connection (may not be available in all contexts)
        return "unknown";
    }

    /**
     * Clears all MDC values set by this filter.
     */
    private void clearMdc() {
        MDC.remove(MDC_CORRELATION_ID);
        MDC.remove(MDC_USER);
        MDC.remove(MDC_INSTANCE);
        MDC.remove(MDC_CLIENT_IP);
        MDC.remove(MDC_METHOD);
        MDC.remove(MDC_PATH);
        MDC.remove("duration_ms");
    }

    /**
     * Gets the current correlation ID from MDC.
     *
     * @return current correlation ID or null
     */
    public static String getCurrentCorrelationId() {
        Object correlationId = MDC.get(MDC_CORRELATION_ID);
        return correlationId != null ? correlationId.toString() : null;
    }

    /**
     * Sets an additional MDC value for the current request.
     *
     * @param key MDC key
     * @param value MDC value
     */
    public static void setMdcValue(String key, String value) {
        if (key != null && value != null) {
            MDC.put(key, value);
        }
    }
}
