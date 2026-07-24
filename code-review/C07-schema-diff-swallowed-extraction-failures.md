# C07 — Schema extraction failures are swallowed but comparison reports success

- **Severity:** Critical (data-loss)
- **Area:** Schema comparison (instance-based path)
- **Locations:** `service/SchemaExtractorService.java:82-84,448,569,659,706`; `service/SchemaComparisonService.java:96-102`

## Problem
Instance-based extractors log and return an empty list on failure; the comparison then sets `success=true`, so a failed source extraction is indistinguishable from an empty schema. (The cross-database `Connection`-overload path throws and is not affected.)

## Impact
A source DB timeout mid-comparison makes every destination object appear EXTRA. With drops enabled, the generated migration script becomes a wall of `DROP ... CASCADE` that destroys the destination schema.

## Recommended fix
- Make the instance-based extractors propagate failures (mirror the throwing `Connection` overloads), and set `success=false` with the error message.
- Never emit destructive DDL from a comparison whose extraction did not fully succeed.

## Acceptance criteria
- [ ] A simulated extraction failure yields `success=false`, not an empty-schema diff.
- [ ] No `DROP` statements are generated when extraction is incomplete.
