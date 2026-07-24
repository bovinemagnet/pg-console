# M22 — Anomaly hour/DOW uses JVM timezone vs DB-timezone baselines

- **Severity:** Major (correctness)
- **Area:** Anomaly detection
- **Locations:** `service/AnomalyDetectionService.java:113-115` (Calendar) vs `:387-390` (`EXTRACT(HOUR/DOW FROM sampled_at)`)

## Problem
Current hour/day-of-week comes from `Calendar.getInstance()` (JVM default TZ) while baselines are bucketed by the DB session timezone.

## Impact
A JVM in Australia/Brisbane against a UTC database compares 14:00 load against the 04:00 baseline → false anomalies every business day.

## Recommended fix
- Derive current hour/DOW in the same timezone the baseline buckets use (compute both in UTC, or bucket both in a configured TZ).

## Acceptance criteria
- [ ] Current-vs-baseline bucket alignment is timezone-consistent.
