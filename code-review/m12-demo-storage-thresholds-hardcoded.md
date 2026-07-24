# m12 — Hard-coded 100/120GB storage thresholds feed the health score

- **Severity:** Minor
- **Area:** Insights
- **Locations:** `service/InsightsService.java:63-67`

'for demo' thresholds are misleading on any real deployment. Make them configuration.
