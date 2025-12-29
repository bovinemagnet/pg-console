package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.SystemMetricsHistory;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory storage for metrics when schema is disabled.
 * <p>
 * This service stores recent metrics in memory for short-term trend display
 * when the pgconsole schema is not available. Metrics are automatically
 * evicted after the configured retention period.
 * <p>
 * Thread-safe for concurrent sampling and reading.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class InMemoryMetricsStore {

    private static final Logger LOG = Logger.getLogger("pgconsole.InMemoryMetricsStore");

    /**
     * System metrics storage keyed by instance ID.
     */
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<SystemMetricsHistory>> systemMetrics =
            new ConcurrentHashMap<>();

    @Inject
    InstanceConfig config;

    /**
     * Adds a system metrics snapshot to the in-memory store.
     *
     * @param instanceId the instance identifier
     * @param metrics    the metrics snapshot to store
     */
    public void addSystemMetrics(String instanceId, SystemMetricsHistory metrics) {
        if (instanceId == null || metrics == null) {
            return;
        }

        ConcurrentLinkedDeque<SystemMetricsHistory> deque = systemMetrics.computeIfAbsent(
                instanceId, k -> new ConcurrentLinkedDeque<>());

        // Ensure sampledAt is set
        if (metrics.getSampledAt() == null) {
            metrics.setSampledAt(Instant.now());
        }

        deque.addLast(metrics);
        LOG.debugf("Added system metrics for instance '%s', store size: %d", instanceId, deque.size());
    }

    /**
     * Retrieves system metrics history for a given instance within a time window.
     *
     * @param instanceId the instance identifier
     * @param hours      number of hours to look back
     * @return list of metrics snapshots ordered by time (oldest first)
     */
    public List<SystemMetricsHistory> getSystemMetricsHistory(String instanceId, int hours) {
        if (instanceId == null) {
            return new ArrayList<>();
        }

        ConcurrentLinkedDeque<SystemMetricsHistory> deque = systemMetrics.get(instanceId);
        if (deque == null || deque.isEmpty()) {
            return new ArrayList<>();
        }

        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<SystemMetricsHistory> result = new ArrayList<>();

        for (SystemMetricsHistory m : deque) {
            if (m.getSampledAt() != null && m.getSampledAt().isAfter(cutoff)) {
                result.add(m);
            }
        }

        LOG.debugf("Retrieved %d system metrics for instance '%s' (last %d hours)",
                Integer.valueOf(result.size()), instanceId, Integer.valueOf(hours));
        return result;
    }

    /**
     * Gets the current number of stored metrics for an instance.
     *
     * @param instanceId the instance identifier
     * @return number of stored metrics
     */
    public int getMetricsCount(String instanceId) {
        ConcurrentLinkedDeque<SystemMetricsHistory> deque = systemMetrics.get(instanceId);
        return deque != null ? deque.size() : 0;
    }

    /**
     * Gets the configured retention period in minutes.
     *
     * @return retention period in minutes
     */
    public int getRetentionMinutes() {
        return config.schema().inMemoryMinutes();
    }

    /**
     * Clears all stored metrics (useful for testing).
     */
    public void clear() {
        systemMetrics.clear();
        LOG.debug("Cleared all in-memory metrics");
    }

    /**
     * Clears stored metrics for a specific instance.
     *
     * @param instanceId the instance identifier
     */
    public void clear(String instanceId) {
        systemMetrics.remove(instanceId);
        LOG.debugf("Cleared in-memory metrics for instance '%s'", instanceId);
    }

    /**
     * Scheduled job to evict old metrics entries.
     * <p>
     * Runs every 60 seconds and removes entries older than the configured
     * retention period (in-memory-minutes).
     */
    @Scheduled(every = "60s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void evictOldEntries() {
        // Only run eviction when schema is disabled (in-memory mode active)
        if (config.schema().enabled()) {
            return;
        }

        int retentionMinutes = config.schema().inMemoryMinutes();
        Instant cutoff = Instant.now().minus(retentionMinutes, ChronoUnit.MINUTES);

        int totalEvicted = 0;

        for (var entry : systemMetrics.entrySet()) {
            String instanceId = entry.getKey();
            ConcurrentLinkedDeque<SystemMetricsHistory> deque = entry.getValue();
            int evictedFromInstance = 0;

            // Remove entries from the front of the deque that are older than cutoff
            while (!deque.isEmpty()) {
                SystemMetricsHistory oldest = deque.peekFirst();
                if (oldest != null && oldest.getSampledAt() != null
                        && oldest.getSampledAt().isBefore(cutoff)) {
                    deque.pollFirst();
                    evictedFromInstance++;
                } else {
                    // Entries are ordered by time, so stop when we hit a recent one
                    break;
                }
            }

            if (evictedFromInstance > 0) {
                LOG.debugf("Evicted %d old entries from instance '%s'", evictedFromInstance, instanceId);
            }
            totalEvicted += evictedFromInstance;
        }

        if (totalEvicted > 0) {
            LOG.debugf("In-memory eviction complete: removed %d entries older than %d minutes",
                    totalEvicted, retentionMinutes);
        }
    }

    /**
     * Gets a summary of current storage state for logging.
     *
     * @return summary string
     */
    public String getStorageSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("In-memory metrics store: ");
        if (systemMetrics.isEmpty()) {
            sb.append("empty");
        } else {
            sb.append("[");
            boolean first = true;
            for (var entry : systemMetrics.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue().size());
                first = false;
            }
            sb.append("]");
        }
        sb.append(" (retention: ").append(config.schema().inMemoryMinutes()).append(" min)");
        return sb.toString();
    }
}
