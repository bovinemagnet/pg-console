# M20 — Five aggregated rate metrics are never populated

- **Severity:** Major (correctness)
- **Area:** Metrics / window comparison
- **Locations:** `service/MetricsHistoryBridgeService.java:450-499`; `service/WindowComparisonService.java:228-246`

## Problem
`getAggregatedMetrics()` never calls `setAvgCommitRate/RollbackRate/InsertRate/UpdateRate/DeleteRate` (setters exist but are never invoked anywhere).

## Impact
The comparison page's Commit/Rollback/Insert/Update/Delete rate deltas always compare 0 vs 0 — five of ten metric rows are permanently meaningless.

## Recommended fix
- Populate the five rate fields from the aggregate query.

## Acceptance criteria
- [ ] The five rate rows show real values in a window comparison.
