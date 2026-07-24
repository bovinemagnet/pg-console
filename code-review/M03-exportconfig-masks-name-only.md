# M03 — Config export masks by key name only, leaking credentials in values

- **Severity:** Major (security)
- **Area:** CLI
- **Locations:** `cli/ExportConfigCommand.java:322-325`

## Problem
Masking matches on key name ("password", "secret"), so `quarkus.datasource.jdbc.url` or `DATABASE_URL=postgres://user:pass@host/db` is written in clear even without `--include-sensitive`. Default export also dumps the whole process environment.

## Impact
Credentials embedded in URL values are exported unmasked.

## Recommended fix
- Pass every exported value through `LogRedactionService.redact()`; scope which config sources are exported by default.

## Acceptance criteria
- [ ] Exported values containing `password=`/userinfo are redacted.
