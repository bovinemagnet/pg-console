# C08 — `pg_get_functiondef` on aggregates aborts function extraction

- **Severity:** Critical (data-loss, compounds C07)
- **Area:** Schema extraction
- **Locations:** `service/SchemaExtractorService.java:522-540` (duplicate at `:1148-1166`)

## Problem
`pg_get_functiondef(p.oid)` is selected for `prokind IN ('f','p','a','w')`, but PostgreSQL raises an ERROR for aggregates ("... is an aggregate function"), aborting the whole query.

## Impact
Any schema containing one custom aggregate makes function extraction fail for the entire schema. Combined with C07, all functions are then reported missing/extra and the migration drops every destination function.

## Recommended fix
- Guard the call: `CASE WHEN p.prokind IN ('f','p') THEN pg_get_functiondef(p.oid) END`, and handle aggregates/windows separately if needed.

## Acceptance criteria
- [ ] Extraction succeeds against a schema containing a custom aggregate.
