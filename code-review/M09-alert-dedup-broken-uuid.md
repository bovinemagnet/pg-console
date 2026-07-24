# M09 — Alert deduplication can never match (random UUID)

- **Severity:** Major (correctness)
- **Area:** Alerting
- **Locations:** `service/notification/AlertManagementService.java:82-89`, `:395-398` (`generateAlertId`)

## Problem
`fireAlert` dedupes by `alertId`, but `generateAlertId` appends a random UUID suffix, so `findByAlertId` never finds an existing alert.

## Impact
Every `fireAlert` for the same condition creates a new alert, new notifications, and a new independent escalation chain — an alert storm by design.

## Recommended fix
- Derive the dedup key deterministically from the alert condition (instance + metric + threshold), not a random UUID.

## Acceptance criteria
- [ ] Re-firing the same condition updates the existing active alert instead of creating a duplicate.
