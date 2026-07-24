# m11 — Query fingerprint mishandles SQL doubled quotes

- **Severity:** Minor
- **Area:** Metrics
- **Locations:** `service/QueryFingerprintService.java:26`

The string-literal regex handles backslash escapes but not standard doubled quotes, so `'it''s'` normalises to `? ?` — weaker grouping (no crash). Extend the pattern.
