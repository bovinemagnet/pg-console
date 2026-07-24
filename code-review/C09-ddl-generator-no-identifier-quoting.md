# C09 — Generated DDL performs no identifier quoting (broken DDL + injection laundering)

- **Severity:** Critical (security + correctness)
- **Area:** DDL generation
- **Locations:** `service/DdlGeneratorService.java:132-157,202-240,302-455,536-630,695-718`

## Problem
Schema, table, column, constraint, owner and index names are interpolated raw into generated DDL. `escapeString()` covers only comment/enum-label literals.

## Impact
1. Any mixed-case or reserved-word identifier (`"Order"`, `"user"`) produces DDL that fails or targets the wrong object.
2. A user with DDL rights on a monitored database can create an object named e.g. `x; DROP TABLE audit;--`; that text lands verbatim in a migration script a DBA will execute — SQL injection laundered through the diff tool.

## Recommended fix
- Add a `quoteIdent()` helper (double embedded quotes, always wrap) and use it for every identifier in every DDL generator.

## Acceptance criteria
- [ ] Mixed-case/reserved identifiers round-trip to valid DDL.
- [ ] An identifier containing `;`/`"` is safely quoted, not executable.
