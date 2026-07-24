# M41 — Generated (STORED) columns rendered as static DEFAULT

- **Severity:** Major (correctness)
- **Area:** DDL generation
- **Locations:** `service/DdlGeneratorService.java:543-558` (`generateFullTableDdl`)

## Problem
Only identity is handled; a column's generation expression (stored in `defaultValue`) is emitted as `DEFAULT <expr>`.

## Impact
`total numeric GENERATED ALWAYS AS (qty*price) STORED` is recreated as a plain column with a static default — silent semantic corruption.

## Recommended fix
- Distinguish generated columns and emit `GENERATED ALWAYS AS (...) STORED`.

## Acceptance criteria
- [ ] A generated column round-trips to a generated column.
