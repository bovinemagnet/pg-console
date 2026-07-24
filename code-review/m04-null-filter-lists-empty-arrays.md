# m04 — Null filter lists persisted as empty arrays vs documented 'NULL = all'

- **Severity:** Minor
- **Area:** Persistence
- **Locations:** `repository/NotificationChannelRepository.java:176-181,221-226`, `repository/MaintenanceWindowRepository.java:171-174,215-218`

Stored data contradicts the schema's 'NULL = all' semantics; rescued only because `matchesAlert` treats empty as match-all. Persist NULL when the filter is absent.
