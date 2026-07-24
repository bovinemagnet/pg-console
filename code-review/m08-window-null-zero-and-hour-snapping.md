# m08 — Window comparison coerces null→0 and snaps boundaries to whole hours

- **Severity:** Minor
- **Area:** Metrics
- **Locations:** `service/WindowComparisonService.java:224-226,273-276`

Null `avgCacheHitRatio`→0 shows a huge fake delta instead of n/a; `toHours()` truncation +1 padding silently includes up to ~2h extra in sub-hour windows. Preserve null and use exact bounds.
