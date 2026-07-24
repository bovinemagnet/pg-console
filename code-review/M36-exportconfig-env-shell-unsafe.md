# M36 — Config export `env` format is shell-unsafe

- **Severity:** Major (security)
- **Area:** CLI
- **Locations:** `cli/ExportConfigCommand.java:289-299`

## Problem
Values are only quoted when containing space/quote; `$`, backticks, semicolons, and newlines pass through unquoted (and `$` is unescaped even inside the added double quotes).

## Impact
Sourcing the generated `env.sh` with a value like `$(command)` executes it; a newline corrupts the file.

## Recommended fix
- Single-quote all values and escape embedded single quotes; reject/encode newlines.

## Acceptance criteria
- [ ] A value containing `$(...)` is inert when the file is sourced.
