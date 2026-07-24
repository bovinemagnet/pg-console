# C10 — `active_alert.alert_id` UNIQUE constraint suppresses recurring incidents

- **Severity:** Critical (correctness)
- **Area:** Alerting persistence
- **Locations:** `src/main/resources/db/migration/V1__initial_schema.sql:393`; `repository/ActiveAlertRepository.java:161-228`; `service/notification/AlertManagementService.java:85-99`

## Problem
`alert_id` is declared table-wide `UNIQUE`, but `findByAlertId` dedupes only against `resolved=FALSE` rows, and `fireAlert` then INSERTs. Once a resolved row exists (retained until purge), the same condition recurring violates the unique constraint.

## Impact
`RuntimeException` on every refire → a real recurring incident is permanently suppressed until old rows are purged.

## Recommended fix
- Replace the table-wide unique constraint with a partial unique index `WHERE resolved = FALSE`, or make the insert an upsert.

## Acceptance criteria
- [ ] Fire → resolve → fire of the same alert_id succeeds and creates a new active alert.
