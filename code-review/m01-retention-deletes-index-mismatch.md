# m01 — Retention DELETEs seq-scan (filter column ≠ leading index column)

- **Severity:** Minor
- **Area:** Persistence
- **Locations:** `repository/HistoryRepository.java:567-591` (sampled_at), `repository/AlertSilenceRepository.java:248` (end_time), `repository/ActiveAlertRepository.java:334` (resolved=TRUE)

Daily cleanup filters on a column that isn't the leading column of any index, so it seq-scans the largest tables. Add an index matching the filter (e.g. a plain `sampled_at` index).
