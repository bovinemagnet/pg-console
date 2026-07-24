# m43 — Custom-SQL validation is denylist regex (currently inert)

- **Severity:** Minor
- **Area:** Custom dashboards
- **Locations:** `service/CustomDashboardService.java:282-298,270-276`

isValidSelectQuery relies on a DANGEROUS_PATTERN denylist + 'starts with SELECT'; denylists miss WITH...SELECT, pg_sleep, dblink, lo_import, set_config. executeCustomSql currently returns a placeholder (not exploitable yet). If execution is ever enabled, use an allowlist / read-only least-privileged role, not this gate.
