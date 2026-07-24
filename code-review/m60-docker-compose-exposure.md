# m60 — docker-compose publishes Postgres on 0.0.0.0 + obsolete version key

- **Severity:** Minor
- **Area:** Docker
- **Locations:** `docker-compose.yml:1,11`

5432 published on 0.0.0.0 exposes a postgres/postgres superuser to the LAN; use 127.0.0.1:5432:5432. `version: '3.8'` is obsolete under Compose v2.
