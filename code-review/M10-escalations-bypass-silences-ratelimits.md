# M10 — Escalations bypass silences, maintenance windows, rate limits, and channel filters

- **Severity:** Major (correctness)
- **Area:** Alerting
- **Locations:** `service/notification/NotificationDispatcher.java:126-146` (`dispatchToChannels`) vs `:71-96` (`dispatch`); `service/notification/EscalationService.java:120`

## Problem
`dispatchToChannels` (used by escalation) performs none of the silence/maintenance/rate-limit/filter checks that `dispatch()` does.

## Impact
A silenced flapping alert still pages on every escalation interval; rate-limited channels get unlimited escalation traffic.

## Recommended fix
- Route escalation sends through the same gating as `dispatch()` (extract the checks into a shared method).

## Acceptance criteria
- [ ] A silenced alert produces no escalation notifications.
