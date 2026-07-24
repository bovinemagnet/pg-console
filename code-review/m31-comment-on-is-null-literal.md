# m31 — Removed comment emits COMMENT ON ... IS 'null'

- **Severity:** Minor
- **Area:** DDL generation
- **Locations:** `service/DdlGeneratorService.java:206-207,236-237`

escapeString(null) returns null and the format string emits the literal string 'null' instead of IS NULL. Emit `IS NULL` when the comment is absent.
