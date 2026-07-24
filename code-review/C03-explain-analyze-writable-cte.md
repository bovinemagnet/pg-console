# C03 — EXPLAIN ANALYZE executes writable CTEs (arbitrary DML)

- **Severity:** Critical (security)
- **Area:** Query explain
- **Locations:** `service/PostgresService.java:1479` (`isExplainSafe`), `:1422` (`explainQuery`); `resource/DashboardResource.java:2035` (`/api/explain`)

## Problem
`isExplainSafe` returns `true` for any query starting with `WITH`. A writable CTE passes the guard, and with `analyse=true` the plan is actually executed. (Verified.)

## Impact
`POST /api/explain?analyse=true&query=WITH d AS (DELETE FROM orders RETURNING 1) SELECT * FROM d` deletes rows despite the "SELECT/WITH/VALUES only" guard. Reachable anonymously (see C01).

## Recommended fix
- Run explains inside a `SET TRANSACTION READ ONLY` transaction with a statement timeout (defence in depth), and
- Reject `WITH` bodies that contain `INSERT`/`UPDATE`/`DELETE`/`MERGE` (or parse to confirm data-modifying statements are absent).

## Acceptance criteria
- [ ] Writable-CTE query is rejected or fails harmlessly under a read-only transaction.
- [ ] Test asserts a `WITH ... DELETE ... RETURNING` explain does not modify data.
