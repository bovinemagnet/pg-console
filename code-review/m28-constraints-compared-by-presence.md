# m28 — Unique/check constraints compared by presence only

- **Severity:** Minor
- **Area:** Schema comparison
- **Locations:** `service/SchemaComparisonService.java:401-468`, `service/DatabaseDiffService.java:464-528`

A constraint with the same name but a different column list/expression is reported identical, so real drift (e.g. a relaxed CHECK) goes undetected. Compare definitions.
