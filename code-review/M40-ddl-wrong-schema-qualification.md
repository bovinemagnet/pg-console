# M40 — Generated index/trigger/FK DDL keeps the source schema qualification

- **Severity:** Major (correctness)
- **Area:** DDL generation
- **Locations:** `service/DdlGeneratorService.java:325-332,608-612,411-416,592-596`

## Problem
Index/trigger DDL is taken verbatim from `pg_get_indexdef`/`pg_get_triggerdef`, which embed the *source* schema; `targetSchema` is ignored. Same for FK `REFERENCES`.

## Impact
Comparing `app_v1` (source) with `app_v2` (dest) generates `CREATE INDEX ... ON app_v1.orders` — created/failing in the wrong schema.

## Recommended fix
- Rewrite the schema qualifier to `targetSchema` when generating cross-schema DDL.

## Acceptance criteria
- [ ] Generated DDL targets the destination schema.
