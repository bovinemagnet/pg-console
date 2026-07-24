# m02 — Comparison repos swallow SQLException (return empty)

- **Severity:** Minor
- **Area:** Persistence
- **Locations:** `repository/ComparisonHistoryRepository.java:110-151,189-253`, `repository/ComparisonProfileRepository.java:199-308`

Read methods and `deleteOlderThan` log + return empty/Optional.empty()/0 unlike every other repo which throws. A metadata-DB outage renders comparison pages as 'no data' and silently stops retention. Throw like the others.
