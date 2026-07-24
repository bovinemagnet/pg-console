# m37 — MODIFIED view/matview and ALTER TYPE ADD VALUE edge cases

- **Severity:** Minor
- **Area:** DDL generation
- **Locations:** `service/DdlGeneratorService.java:242-249,256-281`

A MODIFIED view emits one CREATE OR REPLACE per attribute diff (duplicates); a MODIFIED matview emits nothing (default→null); ALTER TYPE ... ADD VALUE ... AFTER may reference a missing label; parseListString breaks on enum labels containing commas. Handle each case.
