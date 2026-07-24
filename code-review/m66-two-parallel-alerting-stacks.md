# m66 — Two parallel alerting stacks

- **Severity:** Minor
- **Area:** Architecture
- **Locations:** `service/AlertingService.java:264-269` vs `service/notification/AlertManagementService.java` + `NotificationDispatcher`

Legacy config-driven AlertingService (email is a TODO log line) coexists with the AlertManagementService/NotificationDispatcher stack, each with its own cooldown/dedup/HttpClient/escapeJson. Route AlertingService.checkAndAlert through fireAlert to remove the duplication (fix M09 first).
