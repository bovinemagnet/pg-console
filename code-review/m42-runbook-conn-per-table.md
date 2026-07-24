# m42 — RunbookService opens a fresh connection per table in the SQL_TEMPLATE loop

- **Severity:** Minor
- **Area:** Runbooks
- **Locations:** `service/RunbookService.java:484-486`

A new Connection+Statement per table (up to 10), correctly closed but churning pool connections. Reuse one connection across the loop.
