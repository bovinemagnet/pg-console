# m09 — metricName interpolated into SQL (latent SQLi)

- **Severity:** Minor
- **Area:** Metrics
- **Locations:** `service/AnomalyDetectionService.java:367-383`, `service/ForecastingService.java:277-283,307-311`

`%s` metric name; callers use static allowlists but `generateForecastForMetric(String,...)` is public — one REST caller from injection. Validate against the allowlist at method entry.
