# M21 — Anomaly detection reads nullable columns with getDouble (NULL→0)

- **Severity:** Major (correctness)
- **Area:** Anomaly detection
- **Locations:** `service/AnomalyDetectionService.java:510-517` (`getCurrentMetricValues`)

## Problem
`rs.getDouble()` on nullable columns without `wasNull()` turns a NULL `cache_hit_ratio` or `longest_query_seconds` into `0.0`.

## Impact
One NULL cache-hit sample reads as 0% against a ~99% baseline → false CRITICAL "below baseline" anomaly, saved to DB and alert fired.

## Recommended fix
- Check `wasNull()` and skip/short-circuit NULL samples.

## Acceptance criteria
- [ ] A NULL metric sample does not create an anomaly.
