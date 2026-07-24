# M05 — Redaction misses userinfo-style connection URLs

- **Severity:** Major (security)
- **Area:** Logging
- **Locations:** `logging/LogRedactionService.java:33-34`; `config/LoggingConfig.java:536-541`

## Problem
`JDBC_PASSWORD_PATTERN` only matches `password=...`; `postgresql://admin:secret@host/db` (the javadoc's own example) is not redacted.

## Impact
Credentials in URI-userinfo form are logged in clear.

## Recommended fix
- Add a pattern for `scheme://user:pass@host` and redact the userinfo segment.

## Acceptance criteria
- [ ] `postgresql://u:p@host` is redacted in logs.
