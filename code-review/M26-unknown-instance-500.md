# M26 — Unknown `instance` param throws uncaught → 500 reflecting input

- **Severity:** Major (robustness)
- **Area:** Web
- **Locations:** `service/DataSourceManager.java:107`; only `resource/ApiResource.java:448` wraps calls

## Problem
`getDataSource` throws `IllegalArgumentException("No datasource configured for instance: " + name)`. Service methods catch only `SQLException`, so an unknown instance propagates.

## Impact
`GET /api/v1/overview?instance=bogus` (and most endpoints) returns a 500 with the reflected instance name instead of a clean 400/404.

## Recommended fix
- Validate `instance` centrally (filter or shared helper) and return 400/404; don't reflect the raw value.

## Acceptance criteria
- [ ] Unknown instance yields 400/404 consistently across endpoints.
