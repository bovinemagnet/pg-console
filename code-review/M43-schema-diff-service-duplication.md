# M43 — DatabaseDiffService and SchemaComparisonService duplicate ~700 lines

- **Severity:** Major (maintainability)
- **Area:** Schema comparison
- **Locations:** `service/DatabaseDiffService.java:197-986` vs `service/SchemaComparisonService.java:111-976`; extractors `SchemaExtractorService.java:1023-1472` vs `:36-1021`

## Problem
The comparison logic and every extractor are duplicated near-verbatim between the instance-based and connection-based paths, and the two copies have already drifted (e.g. `compareForeignKeys`/`compareColumns` signatures).

## Impact
Fixes (e.g. M37/M38, C07) applied to one copy leave the other broken.

## Recommended fix
- Collapse `DatabaseDiffService` onto `SchemaComparisonService` using the `Connection` overloads as the single implementation.

## Acceptance criteria
- [ ] One comparison implementation; both entry points delegate to it.
