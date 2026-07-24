# M42 — FK constraints embedded in CREATE with no inter-table ordering

- **Severity:** Major (correctness)
- **Area:** DDL generation
- **Locations:** `service/DdlGeneratorService.java:591-605,483-518` (`reorderByDependency`)

## Problem
FK `ALTER TABLE ... ADD CONSTRAINT` is bundled inside each table's CREATE text, and `reorderByDependency` has no ordering between tables.

## Impact
For missing tables A→B where A's FK references B, A sorts first and its embedded `ADD CONSTRAINT ... REFERENCES B` executes before `CREATE TABLE B` → script aborts (fatal under SINGLE_TRANSACTION).

## Recommended fix
- Emit all CREATE TABLEs first, then all ADD CONSTRAINTs; or topologically order and defer FK statements.

## Acceptance criteria
- [ ] A→B dependency generates a script that applies in one transaction.
