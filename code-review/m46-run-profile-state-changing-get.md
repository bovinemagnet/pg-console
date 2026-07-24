# m46 — GET /run-profile/{id} is state-changing (CSRF-prone)

- **Severity:** Minor
- **Area:** Web
- **Locations:** `resource/SchemaComparisonResource.java:206-235`

A GET performs updateLastRun + history record; it is cacheable/prefetchable and CSRF-triggerable via a link/image. Make it POST.
