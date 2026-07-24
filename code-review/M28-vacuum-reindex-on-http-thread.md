# M28 — VACUUM/REINDEX/CLUSTER run on the HTTP thread with no statement timeout

- **Severity:** Major (availability)
- **Area:** Maintenance execution
- **Locations:** `service/RunbookService.java:400-419`; `service/ScheduledMaintenanceService.java:351-356`

## Problem
`stmt.execute(...)` for VACUUM/VACUUM FULL/REINDEX/CLUSTER has no `setQueryTimeout(...)`; `autoExecuteRunbook` is invoked directly from a JAX-RS POST handler, and `max_duration_minutes` is never applied.

## Impact
A VACUUM FULL / REINDEX on a large table blocks the worker thread and HTTP connection indefinitely and holds locks.

## Recommended fix
- Run maintenance off-request (async worker), set `setQueryTimeout` from `max_duration_minutes`.

## Acceptance criteria
- [ ] Long maintenance does not block request threads and honours a timeout.
