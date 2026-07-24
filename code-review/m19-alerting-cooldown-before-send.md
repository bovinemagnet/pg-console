# m19 — Cooldown recorded before send + check-then-put race

- **Severity:** Minor
- **Area:** Notifications
- **Locations:** `service/AlertingService.java:167-186`

A failed webhook still suppresses re-alerting for the full cooldown; two concurrent callers can both pass the check. Record cooldown on success; use atomic putIfAbsent.
