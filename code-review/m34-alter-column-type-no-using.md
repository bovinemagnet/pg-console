# m34 — ALTER COLUMN ... TYPE generated without USING

- **Severity:** Minor
- **Area:** DDL generation
- **Locations:** `service/DdlGeneratorService.java:220-221`

Common type changes (text→integer) fail at execution without a USING clause and no warning is attached. Add USING or attach a warning.
