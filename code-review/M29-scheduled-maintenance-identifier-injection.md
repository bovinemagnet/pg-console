# M29 — Identifier injection in ScheduledMaintenanceService.buildMaintenanceSQL (latent)

- **Severity:** Major (security, latent)
- **Area:** Maintenance
- **Locations:** `service/ScheduledMaintenanceService.java:636-654`

## Problem
Target is built as `String.format("\"%s\".\"%s\"", schema, table)` and concatenated into VACUUM/REINDEX/CLUSTER. Double-quoting is not injection-safe: a table name containing `"` breaks out (e.g. `x"; DROP TABLE y; --`). Currently no endpoint wires `createTask`/`executeTask`, so it is latent.

## Impact
Live injection sink the moment a create-task UI is added.

## Recommended fix
- Use `quote_ident` semantics (double embedded quotes) via a shared `quoteIdent()` helper (see C09).

## Acceptance criteria
- [ ] A table name containing `"` is safely quoted.
