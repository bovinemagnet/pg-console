# M37 — FK extraction cross-joins pg_attribute → duplicated columns

- **Severity:** Major (correctness)
- **Area:** Schema extraction
- **Locations:** `service/SchemaExtractorService.java:189-207`

## Problem
The FK query joins `pg_attribute` twice (`a1` on conkey, `a2` on confkey) producing a cross-product, so composite FKs aggregate duplicated column lists.

## Impact
A 2-column FK yields `FOREIGN KEY (a, a, b, b) REFERENCES t (x, x, y, y)` in the diff display and generated DDL — invalid.

## Recommended fix
- Use `unnest` with ordinality to pair conkey/confkey positionally instead of cross-joining.

## Acceptance criteria
- [ ] A composite FK extracts the correct column pairs.
