# C01 — Enabling security does not actually protect the dashboards

- **Severity:** Critical (security)
- **Area:** Auth / configuration
- **Locations:** `src/main/resources/application.properties:184-195`, `config/InstanceConfig.java:437-453`; all `resource/*` classes except `LoggingResource`

## Problem
`PG_CONSOLE_SECURITY_ENABLED=true` only sets `quarkus.http.auth.basic=true`, which makes Basic auth *available*. The single permission policy covers `POST /api/activity/*/cancel|terminate` only. There is no catch-all `quarkus.http.auth.permission.../paths=/*`, and no `@Authenticated`/`@RolesAllowed` on any resource except `LoggingResource`.

## Impact
An operator who enables security (javadoc promises "all dashboard endpoints require valid credentials") still exposes every dashboard, export, and API endpoint anonymously. This is the master finding: it makes C02–C08 and the unauthenticated-endpoint majors exploitable in the intended-secure configuration.

## Recommended fix
- Add a deny-by-default permission policy covering `/*`, then explicitly allow health/static assets.
- Or annotate resources with `@RolesAllowed`. Add an integration test asserting an unauthenticated request to a representative dashboard + API + export endpoint returns 401 when security is enabled.

## Acceptance criteria
- [ ] With security enabled, anonymous GET/POST to dashboards, `/api/*`, exports return 401.
- [ ] Test covers at least one page, one API, one export, and the activity cancel/terminate path.
