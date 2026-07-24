# m39 — /api/sparkline 500s on non-numeric input

- **Severity:** Minor
- **Area:** Web
- **Locations:** `resource/DashboardResource.java:435`

Double.parseDouble on each token with no try/catch → NumberFormatException → 500 for `?values=abc`. Validate/skip non-numeric tokens and return 400.
