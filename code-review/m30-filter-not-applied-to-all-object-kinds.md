# m30 — Include/exclude filter only applied to tables and views

- **Severity:** Minor
- **Area:** Schema comparison
- **Locations:** `service/SchemaComparisonService.java:705-709,781-785,842-846` (mirrored in DatabaseDiffService)

matchesTable is not applied to functions, sequences, or types, so a filter silently doesn't filter half the object kinds. Apply consistently.
