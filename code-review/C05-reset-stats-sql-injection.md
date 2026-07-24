# C05 — `reset-stats --database` SQL injection

- **Severity:** Critical (security, CLI)
- **Area:** CLI
- **Locations:** `cli/ResetStatsCommand.java:198`

## Problem
`"...WHERE datname = '" + database + "'"` concatenates the `--database` option into a string literal; pgJDBC `Statement.execute` accepts multi-statement strings.

## Impact
`pg-console reset-stats -d "x'); DROP TABLE ...; --"` executes arbitrary SQL with the monitoring account's privileges. This is the exact anti-pattern hardened elsewhere in commit `b327739`.

## Recommended fix
- Use a `PreparedStatement` with `datname = ?`.

## Acceptance criteria
- [ ] The database filter is bound as a parameter; a value with a quote does not alter the statement.
