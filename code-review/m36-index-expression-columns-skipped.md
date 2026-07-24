# m36 — Index column aggregation skips expression columns

- **Severity:** Minor
- **Area:** Schema extraction
- **Locations:** `service/SchemaExtractorService.java:316-333`

indkey entries of 0 (expression columns) are skipped, so expression indexes show a truncated/empty column list in docs/diffs (comparison still works via definition). Render the expression.
