# m26 — FeatureToggleService is fail-open and ~900 lines of boilerplate

- **Severity:** Minor
- **Area:** Config
- **Locations:** `service/FeatureToggleService.java:816`

isPageEnabled default → true means a page whose ID is never registered can't be disabled. Collapse to a map-driven registry keyed by page ID and fail closed for unknown IDs.
