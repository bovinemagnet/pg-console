# M13 — PagerDuty resolution NPEs and 500s the request

- **Severity:** Major (correctness)
- **Area:** Notifications
- **Locations:** `service/notification/PagerDutyNotificationSender.java:69`; `service/notification/AbstractNotificationSender.java:67`; `service/notification/NotificationDispatcher.java:168-177` (`dispatchResolution`, no try/catch)

## Problem
`createSuccessResult(channel, alert, null)` returns a null response; `AbstractNotificationSender` then calls `response.statusCode()` → NPE. `dispatchResolution` has no try/catch.

## Impact
Resolving an alert with a PagerDuty channel (autoResolve off) throws; remaining channels get no resolution notice and the HTTP request 500s.

## Recommended fix
- Return a non-null result for the no-op resolve, and guard `dispatchResolution` per channel.

## Acceptance criteria
- [ ] Resolving an alert with a PagerDuty channel does not throw.
