# C02 — JDBC URL injection via unvalidated database name (RCE/SSRF/file-write)

- **Severity:** Critical (security)
- **Area:** Cross-database connection
- **Locations:** `service/CrossDatabaseConnectionService.java:127-137` (`replaceDatabase`), `:109` (`DriverManager.getConnection`); callers `resource/DatabaseDiffResource.java` (form params `sourceDatabase`/`destDatabase`), `resource/SchemaDocResource.java` (`@QueryParam database`)

## Problem
The user-supplied database name is spliced verbatim into the JDBC URL and passed to `DriverManager.getConnection`. When the base URL has no `?params`, an attacker-supplied name such as `postgres?socketFactory=<class>&loggerFile=/path&sslfactory=...` injects arbitrary Postgres JDBC connection properties. The value is never checked against `listDatabases()`. (Independently found by two reviewers; verified.)

## Impact
Arbitrary JDBC connection properties → potential arbitrary class instantiation (RCE), local file write via `loggerFile`, or connections to attacker-chosen hosts (SSRF).

## Recommended fix
- In `getConnectionToDatabase`, validate `databaseName` against `listDatabases(instance)` and reject if absent.
- The database name cannot be a bind parameter (it selects the connection target), so allowlist validation is the correct control.

## Acceptance criteria
- [ ] Requests with a database name not in `listDatabases()` are rejected (400/404) before any connection attempt.
- [ ] Test with a payload containing `?`/`&`/`socketFactory` is rejected.
