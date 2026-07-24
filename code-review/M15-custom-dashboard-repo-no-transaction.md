# M15 — CustomDashboardRepository widget writes lack a transaction

- **Severity:** Major (data integrity)
- **Area:** Persistence
- **Locations:** `repository/CustomDashboardRepository.java:319-339` (`replaceWidgets`), `:119-158` (`create`)

## Problem
DELETE + N INSERTs run on one connection in autocommit, with no `setAutoCommit(false)`/commit/rollback.

## Impact
An exception on the third widget insert leaves the dashboard with widgets deleted and only two re-created — corrupted with no recovery.

## Recommended fix
- Use an explicit transaction around the delete + inserts.

## Acceptance criteria
- [ ] A failed widget insert leaves the prior widget set intact.
