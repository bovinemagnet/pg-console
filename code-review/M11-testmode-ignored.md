# M11 — Channel `testMode` is never honoured

- **Severity:** Major (correctness)
- **Area:** Notifications
- **Locations:** `model/NotificationChannel.java:144-151`; persisted at `repository/NotificationChannelRepository.java:183,228`; no reader anywhere

## Problem
`testMode` (documented "notifications are logged but not sent") is persisted but no sender or dispatcher checks `isTestMode()`.

## Impact
Test-mode channels send real pages/messages.

## Recommended fix
- Short-circuit sends for `isTestMode()` channels (log only).

## Acceptance criteria
- [ ] A test-mode channel logs but does not POST.
