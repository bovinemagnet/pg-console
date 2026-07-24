# M30 — Dead `sql` variable in getSummary calls pg_stat_statements_reset()

- **Severity:** Major (loaded gun)
- **Area:** Statements management
- **Locations:** `service/StatementsManagementService.java:207-217`

## Problem
`String sql = """ ... pg_stat_statements_reset() IS NOT NULL as can_reset ... """` is assigned but never executed (the method runs `checkSql`/`statsSql`). Harmless today, but the field is named `sql` and executing it on a read path would wipe all query stats on every dashboard view.

## Recommended fix
- Delete the dead variable.

## Acceptance criteria
- [ ] The reset SQL no longer exists in the read path.
