# m69 — ~4,300 lines of repeated JDBC boilerplate across repositories

- **Severity:** Minor
- **Area:** Persistence
- **Locations:** `repository/*`

Connection/prepare/map boilerplate and repeated column lists per repo would shrink with a small row-mapper helper and shared getLongOrNull/getDoubleOrNull (currently private to HistoryRepository while others hand-roll null checks). StopwatchRepository returns null where siblings return Optional.
