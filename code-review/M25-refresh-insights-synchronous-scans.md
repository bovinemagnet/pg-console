# M25 — refreshInsights runs ~192 percentile scans synchronously on the HTTP thread

- **Severity:** Major (performance)
- **Area:** Insights
- **Locations:** `service/InsightsService.java:244-262` (`refreshInsights`), called from `resource/InsightsResource.java:535`

## Problem
`calculateBaselines` runs 6 metrics × 32 buckets = 192 `PERCENTILE_CONT` scans of `system_metrics_history`, each on its own pooled connection, plus forecasting and detection, all on the request thread with no in-flight guard.

## Impact
One "refresh insights" click (or two concurrent) monopolises the connection pool for minutes and stalls the whole console.

## Recommended fix
- Move refresh to a background job with a single in-flight guard; cache results; batch the percentile queries.

## Acceptance criteria
- [ ] A refresh does not exhaust the pool; concurrent clicks coalesce.
