# m13 — MetricsSamplerService and InMemoryMetricsSampler duplicate ~200 lines

- **Severity:** Minor
- **Area:** Metrics
- **Locations:** `service/MetricsSamplerService.java`, `service/InMemoryMetricsSampler.java:206-223`

System/database/infrastructure sampling SQL and null helpers are duplicated verbatim; `sampleQueryMetrics` also stamps a fresh Instant.now() per row. Extract a shared sampler taking a sink; batch one sample timestamp.
