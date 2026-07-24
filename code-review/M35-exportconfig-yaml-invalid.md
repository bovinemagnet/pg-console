# M35 — Config export YAML is structurally invalid

- **Severity:** Major (correctness)
- **Area:** CLI
- **Locations:** `cli/ExportConfigCommand.java:243-262`

## Problem
Parent keys are re-emitted per entry (`pg-console:` printed once per property), producing duplicate mapping keys / wrong nesting. `needsQuoting()` also ignores newlines.

## Impact
`export-config --format yaml | yq` errors or silently drops entries.

## Recommended fix
- Build a nested map and serialise with a YAML library (SnakeYAML) rather than hand-emitting.

## Acceptance criteria
- [ ] Exported YAML parses and round-trips.
