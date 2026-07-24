# M27 — Unvalidated `Instant.parse` on window-comparison params → 500

- **Severity:** Major (robustness)
- **Area:** Web
- **Locations:** `resource/WindowComparisonResource.java:157-160`, `resource/WindowComparisonApiResource.java:102-105`

## Problem
`startA/endA/startB/endB` are parsed with `Instant.parse` with no null/format guard.

## Impact
A missing/malformed param (`startA=today` or omitted) throws `DateTimeParseException`/NPE → uncaught 500 instead of 400.

## Recommended fix
- Validate and return 400 with a clear message on parse failure.

## Acceptance criteria
- [ ] Malformed/missing timestamp yields 400.
