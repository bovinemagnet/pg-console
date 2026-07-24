# M31 — ReportService loop-counter reuse empties index recommendations

- **Severity:** Major (correctness)
- **Area:** Reports
- **Locations:** `service/ReportService.java:161-192`

## Problem
`count` is incremented up to 10 in the slow-queries loop, then reused as `if (count++ >= 5) break;` in the index-recs loop — which breaks on the first iteration.

## Impact
The "Index Recommendations" table in the daily summary is always empty.

## Recommended fix
- Use a separate counter per loop.

## Acceptance criteria
- [ ] Index recommendations render when present.
