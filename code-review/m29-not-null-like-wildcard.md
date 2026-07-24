# m29 — `conname NOT LIKE '%_not_null'` also excludes legit names

- **Severity:** Minor
- **Area:** Schema extraction
- **Locations:** `service/SchemaExtractorService.java:291`

`_` is a LIKE wildcard, so a user constraint named `chk_qty_not_null` is silently excluded from extraction/comparison/DDL. Escape the underscore or match precisely.
