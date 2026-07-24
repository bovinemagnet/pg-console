# C04 — Runbook auto-execute runs arbitrary SQL and ignores confirmation flags

- **Severity:** Critical (security)
- **Area:** Runbooks
- **Locations:** `service/RunbookService.java:400-419` (`autoExecuteRunbook`), `:470-509` (`executeStep`); `resource/InsightsResource.java:372-478`; model flags `model/Runbook.java:191-263`

## Problem
`autoExecuteRunbook` loops over every step and calls `executeStep` unconditionally. The per-step `autoExecute` and `requiresConfirmation` flags are never read anywhere in `src/main`. For `SQL_TEMPLATE` steps, `stmt.execute(step.getAction())` runs whatever text the row holds, with no verb allowlist — the only gate is the row-level `auto_executable` boolean.

## Impact
Unauthenticated `POST /insights/runbooks/{id}/auto-execute` runs `VACUUM ANALYZE {table}` (seeded step marked `auto_execute:false`) against live tables, and would run any `DROP`/`pg_terminate_backend` text present in a runbook row.

## Recommended fix
- Honour `autoExecute`/`requiresConfirmation`: skip (or require an explicit confirm token for) steps not marked auto-executable.
- Restrict auto-executable `SQL_TEMPLATE` steps to a verb allowlist (VACUUM/ANALYZE/REINDEX).
- Gate the endpoints behind admin auth (see C01).

## Acceptance criteria
- [ ] A step flagged `auto_execute:false` is not executed by auto-execute.
- [ ] A runbook row containing a non-allowlisted verb is refused.
