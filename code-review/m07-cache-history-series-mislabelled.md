# m07 — Persisted cache history duplicates buffer ratio / mislabels active connections

- **Severity:** Minor
- **Area:** Metrics
- **Locations:** `service/MetricsHistoryBridgeService.java:385,127`

For windows >60min the 'Index Cache' series silently shows buffer-cache data, and 'active connections' plots activeQueries — different semantics from the in-memory path, so the chart jumps at the 60-minute boundary. Align the two paths.
