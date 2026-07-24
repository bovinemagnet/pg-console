# M02 — Raw JDBC URL (with credentials) printed unredacted

- **Severity:** Major (security)
- **Area:** Logging / CLI
- **Locations:** `StartupBanner.java:72`, `cli/HealthCheckCommand.java:164`; `logging/LogRedactionService.sanitiseJdbcUrl()` (exists, never called)

## Problem
A URL like `jdbc:postgresql://host/db?user=x&password=secret` is echoed to stdout at every startup and in `health-check --verbose`. `sanitiseJdbcUrl()` is dead code.

## Impact
Credentials land in container logs / CI output.

## Recommended fix
- Route all JDBC-URL output through `LogRedactionService.sanitiseJdbcUrl()`.

## Acceptance criteria
- [ ] Banner and health-check output mask password/userinfo.
