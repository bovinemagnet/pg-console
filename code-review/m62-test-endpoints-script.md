# m62 — test-endpoints.sh false-positives on the word 'Exception'; CLAUDE.md typo

- **Severity:** Minor
- **Area:** Tests
- **Locations:** `test-endpoints.sh:37`; `CLAUDE.md`

The error grep for bare `Exception` fails on any page legitimately containing the word (a query text, the audit log) despite a 200. CLAUDE.md references `test-enpoints.sh` (typo) and omits test-endpoints-write.sh. Match a real error marker; fix the docs.
