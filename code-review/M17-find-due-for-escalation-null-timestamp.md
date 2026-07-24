# M17 — Alerts with NULL `last_notification_at` are excluded from escalation

- **Severity:** Major (correctness)
- **Area:** Alerting persistence
- **Locations:** `repository/ActiveAlertRepository.java:96-123` (`findDueForEscalation`)

## Problem
The predicate `last_notification_at + interval <= NOW()` is NULL when `last_notification_at` is NULL (nullable column; `fireAlert` saves alerts without setting it).

## Impact
If the initial notification dispatch fails before `last_notification_at` is set, the alert is silently excluded from escalation forever — exactly the case escalation exists for.

## Recommended fix
- Treat NULL `last_notification_at` as immediately due: `COALESCE(last_notification_at, created_at)` or `last_notification_at IS NULL OR ...`.

## Acceptance criteria
- [ ] An alert that never got an initial notification is picked up for escalation.
