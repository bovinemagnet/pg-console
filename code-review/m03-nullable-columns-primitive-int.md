# m03 — Nullable columns mapped to primitive int

- **Severity:** Minor
- **Area:** Persistence
- **Locations:** `repository/NotificationHistoryRepository.java:60,320`, `repository/NotificationChannelRepository.java:182,305`

`response_code`/`rate_limit_per_hour` are nullable but read into `int`: NULL reads as 0 and can never be written, so documented NULL states are unreachable (survives by coincidence). Use Integer + wasNull.
