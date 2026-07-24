# M23 — saveAnomaly has no dedup/auto-resolve → unbounded growth + alert spam

- **Severity:** Major (correctness)
- **Area:** Anomaly detection
- **Locations:** `service/AnomalyDetectionService.java:667-701` (`saveAnomaly`)

## Problem
Plain INSERT with no dedup and no auto-resolve; anomalies close only via manual `resolveAnomaly`.

## Impact
Each `detectAnomalies()` run while a condition persists inserts another open row and re-fires HIGH/CRITICAL alerts — `detected_anomaly` grows without bound; insights fill with duplicates; alert spam.

## Recommended fix
- Dedup against an existing open anomaly for the same metric; auto-resolve when the condition clears.

## Acceptance criteria
- [ ] A persisting condition yields one open anomaly, not one per run.
