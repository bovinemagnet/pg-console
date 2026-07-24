# m50 — Filenames.java has a duplicate dead condition

- **Severity:** Minor
- **Area:** Util
- **Locations:** `util/Filenames.java:38-39`

`c == '\\'` appears twice in the same if. Harmless; remove the duplicate.
