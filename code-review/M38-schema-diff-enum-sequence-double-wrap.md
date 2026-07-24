# M38 — Missing sequence/enum DDL is double-wrapped

- **Severity:** Major (correctness)
- **Area:** DDL generation
- **Locations:** `service/DdlGeneratorService.java:418-424,426-433`; sources at `service/SchemaComparisonService.java:796,860`

## Problem
`sourceDefinition` for MISSING sequences/enums is already a full `CREATE SEQUENCE...;`/`CREATE TYPE ... AS ENUM(...);`, then `generateCreateSequence`/`generateCreateEnumType` wraps it again.

## Impact
Every missing sequence/enum emits doubly-nested, invalid DDL (`CREATE TYPE s.t AS ENUM (CREATE TYPE s.t AS ENUM ('a');)`).

## Recommended fix
- Emit the stored definition as-is, or build from parts once — not both.

## Acceptance criteria
- [ ] A missing enum/sequence produces one valid CREATE statement.
