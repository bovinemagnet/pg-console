# m16 — repeatCount is dead / would loop forever

- **Severity:** Minor
- **Area:** Notifications
- **Locations:** `service/notification/EscalationService.java:98-107`; `repository/ActiveAlertRepository.java:96-123`

findDueForEscalation joins on tier_order = current+1, so max-tier alerts are never returned and the repeat-restart branch is unreachable; if invoked directly it resets to tier 0 indefinitely. Implement repeat counting or remove.
