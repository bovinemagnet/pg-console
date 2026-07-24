# m38 — Negative limit crashes /api/slow-queries

- **Severity:** Minor
- **Area:** Web
- **Locations:** `resource/ApiResource.java:172`

`if (queries.size() > limit) subList(0, limit)` with limit=-1 throws IllegalArgumentException → 500. Clamp limit to a sane lower bound.
