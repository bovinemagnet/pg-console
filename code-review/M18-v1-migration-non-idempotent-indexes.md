# M18 — V1 migration mixes idempotent tables with non-idempotent indexes

- **Severity:** Major (operational)
- **Area:** Migrations
- **Locations:** `db/migration/V1__initial_schema.sql` (tables use `CREATE TABLE IF NOT EXISTS`; many indexes use plain `CREATE INDEX` — e.g. lines 38,68-71,111-114,144,291-292,315,334,347-348,363,385-388,408-410,426,466-469,504-509,537,605-610,639,694,808-813,854-857,886-889,916,955)

## Problem
The `IF NOT EXISTS` on tables signals intent to survive a pre-existing schema, but the plain `CREATE INDEX`es abort on the first duplicate-index error.

## Impact
Running V1 against a database where the pgconsole tables already exist (baseline mishap / pre-Flyway install) aborts halfway, leaving a partially applied failed migration.

## Recommended fix
- Use `CREATE INDEX IF NOT EXISTS` consistently.

## Acceptance criteria
- [ ] V1 re-applies cleanly against a pre-existing pgconsole schema.
