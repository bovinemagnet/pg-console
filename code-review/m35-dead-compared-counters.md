# m35 — Dead *Compared counters and contradictory hasBreakingChanges

- **Severity:** Minor
- **Area:** Schema comparison
- **Locations:** `model/SchemaComparisonResult.java:392-407,635-681`

All *Compared counters are never incremented (always 0); ComparisonSummary.hasBreakingChanges (missing||modified) contradicts the severity-based SchemaComparisonResult.hasBreakingChanges, so the summary badge can show danger with zero BREAKING diffs. Reconcile or remove.
