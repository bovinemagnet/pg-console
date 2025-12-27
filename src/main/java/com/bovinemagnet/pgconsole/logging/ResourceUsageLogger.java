package com.bovinemagnet.pgconsole.logging;

import com.bovinemagnet.pgconsole.config.LoggingConfig;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for periodic logging of resource usage metrics.
 * <p>
 * Logs memory usage, thread counts, and other JVM metrics at configurable intervals.
 * Useful for identifying resource leaks and capacity issues.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class ResourceUsageLogger {

    private static final Logger LOG = Logger.getLogger(ResourceUsageLogger.class);

    @Inject
    LoggingConfig loggingConfig;

    @Inject
    StructuredLogger structuredLogger;

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final Runtime runtime = Runtime.getRuntime();

    /**
     * Scheduled job to log resource usage.
     * <p>
     * Runs at the configured interval when resource logging is enabled.
     */
    @Scheduled(every = "${pgconsole-logging.performance.resource-logging-interval-seconds:60}s",
               concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void logResourceUsage() {
        if (!loggingConfig.performanceResourceLoggingEnabled()) {
            return;
        }

        try {
            Map<String, Object> metrics = collectMetrics();
            logMetrics(metrics);
        } catch (Exception e) {
            LOG.debugf(e, "Failed to collect resource metrics");
        }
    }

    /**
     * Collects current resource metrics.
     *
     * @return map of metric name to value
     */
    public Map<String, Object> collectMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Heap memory
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        metrics.put("heap_used_mb", heapUsage.getUsed() / (1024 * 1024));
        metrics.put("heap_max_mb", heapUsage.getMax() / (1024 * 1024));
        metrics.put("heap_committed_mb", heapUsage.getCommitted() / (1024 * 1024));
        metrics.put("heap_usage_percent", calculatePercentage(heapUsage.getUsed(), heapUsage.getMax()));

        // Non-heap memory
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        metrics.put("non_heap_used_mb", nonHeapUsage.getUsed() / (1024 * 1024));
        metrics.put("non_heap_committed_mb", nonHeapUsage.getCommitted() / (1024 * 1024));

        // Runtime memory
        metrics.put("free_memory_mb", runtime.freeMemory() / (1024 * 1024));
        metrics.put("total_memory_mb", runtime.totalMemory() / (1024 * 1024));
        metrics.put("max_memory_mb", runtime.maxMemory() / (1024 * 1024));

        // Threads
        metrics.put("thread_count", threadMXBean.getThreadCount());
        metrics.put("daemon_thread_count", threadMXBean.getDaemonThreadCount());
        metrics.put("peak_thread_count", threadMXBean.getPeakThreadCount());

        // CPU
        metrics.put("available_processors", runtime.availableProcessors());

        // Uptime
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        metrics.put("uptime_seconds", uptimeMs / 1000);
        metrics.put("uptime_formatted", formatUptime(uptimeMs));

        return metrics;
    }

    /**
     * Gets a summary of current resource usage.
     *
     * @return resource usage summary
     */
    public ResourceSummary getResourceSummary() {
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();

        return new ResourceSummary(
            heapUsage.getUsed() / (1024 * 1024),
            heapUsage.getMax() / (1024 * 1024),
            calculatePercentage(heapUsage.getUsed(), heapUsage.getMax()),
            threadMXBean.getThreadCount(),
            threadMXBean.getPeakThreadCount(),
            ManagementFactory.getRuntimeMXBean().getUptime() / 1000
        );
    }

    /**
     * Logs collected metrics.
     */
    private void logMetrics(Map<String, Object> metrics) {
        // Check for concerning metrics
        long heapPercent = (Long) metrics.get("heap_usage_percent");
        if (heapPercent > 90) {
            structuredLogger.warn("RESOURCES",
                String.format("High heap memory usage: %d%% (%d MB / %d MB)",
                    heapPercent, metrics.get("heap_used_mb"), metrics.get("heap_max_mb")),
                metrics);
        } else if (heapPercent > 75) {
            structuredLogger.info("RESOURCES",
                String.format("Elevated heap memory usage: %d%% (%d MB / %d MB)",
                    heapPercent, metrics.get("heap_used_mb"), metrics.get("heap_max_mb")),
                metrics);
        } else {
            structuredLogger.debug("RESOURCES",
                String.format("Resource usage: heap=%d%%, threads=%d, uptime=%s",
                    heapPercent, metrics.get("thread_count"), metrics.get("uptime_formatted")),
                metrics);
        }

        // Log thread count warnings
        int threadCount = (Integer) metrics.get("thread_count");
        if (threadCount > 500) {
            structuredLogger.warn("RESOURCES",
                String.format("High thread count: %d (peak: %d)",
                    threadCount, metrics.get("peak_thread_count")),
                metrics);
        }
    }

    /**
     * Calculates percentage, handling edge cases.
     */
    private long calculatePercentage(long used, long max) {
        if (max <= 0) {
            return 0;
        }
        return (used * 100) / max;
    }

    /**
     * Formats uptime in human-readable form.
     */
    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Resource usage summary.
     */
    public record ResourceSummary(
        long heapUsedMb,
        long heapMaxMb,
        long heapUsagePercent,
        int threadCount,
        int peakThreadCount,
        long uptimeSeconds
    ) {
        public String getHeapUsageFormatted() {
            return String.format("%d MB / %d MB (%d%%)", heapUsedMb, heapMaxMb, heapUsagePercent);
        }

        public String getUptimeFormatted() {
            long minutes = uptimeSeconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 0) {
                return String.format("%dd %dh", days, hours % 24);
            } else if (hours > 0) {
                return String.format("%dh %dm", hours, minutes % 60);
            } else {
                return String.format("%dm", minutes);
            }
        }
    }
}
