# M12 — Escalation scheduler can overlap itself → duplicate notifications

- **Severity:** Major (correctness/concurrency)
- **Area:** Alerting
- **Locations:** `service/notification/EscalationService.java:53` (`@Scheduled(every="60s")`, default PROCEED), `:129-131`; blocking sends `service/notification/AbstractNotificationSender.java:48,54` (30s timeout); `repository/ActiveAlertRepository.java:96-123`

## Problem
The 60s scheduled escalation uses the default `PROCEED` policy while sends are blocking (30s each) on the scheduler thread. `last_notification_at` is only updated after sending, so a slow run overlaps the next and re-selects the same alerts.

## Impact
Alerts escalate twice.

## Recommended fix
- Use `concurrentExecution = SKIP` and/or dispatch sends asynchronously; update `last_notification_at` before/atomically with sending.

## Acceptance criteria
- [ ] Overlapping scheduler runs do not double-escalate.
