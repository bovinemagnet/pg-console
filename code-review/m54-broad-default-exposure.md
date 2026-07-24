# m54 — Broad-by-default exposure (0.0.0.0 + Swagger in prod)

- **Severity:** Minor
- **Area:** Config
- **Locations:** `src/main/resources/application.properties:22,28,326`

HTTP and the management interface bind 0.0.0.0 with security off by default, and quarkus.swagger-ui.always-include=true ships Swagger UI in production builds. Bind loopback by default; gate Swagger to dev.
