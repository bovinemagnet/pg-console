# M39 — "Exclude DROP statements" still emits ALTER ... DROP COLUMN/CONSTRAINT

- **Severity:** Major (data-loss)
- **Area:** Migration script
- **Locations:** `model/MigrationScript.java:411-413` (`isDropStatement`); `service/DdlGeneratorService.java:133-153`

## Problem
`isDropStatement()` only matches DDL starting with `DROP`, but EXTRA columns/constraints generate `ALTER TABLE ... DROP COLUMN/CONSTRAINT`.

## Impact
A user who unticks "include DROP statements" still gets irreversible `ALTER TABLE ... DROP COLUMN` (data loss) in `getFullScript()`.

## Recommended fix
- Detect `DROP COLUMN`/`DROP CONSTRAINT` within ALTER statements as destructive too.

## Acceptance criteria
- [ ] With DROPs excluded, no ALTER ... DROP appears in the script.
