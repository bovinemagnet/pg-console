# M34 — Runtime log-level changes silently fail (getLevel NPE swallowed)

- **Severity:** Major (correctness)
- **Area:** Logging
- **Locations:** `logging/LogLevelManager.java:108-110`

## Problem
`originalLevels.put(loggerName, logger.getLevel())` NPEs when `getLevel()` is null (the normal case — loggers inherit levels); the catch swallows it and returns false.

## Impact
`PUT /api/logging/level/...`, presets, and `enableDebugMode()` silently fail unless the logger already had an explicit level.

## Recommended fix
- Handle null `getLevel()` (store a sentinel / use effective level) before mutating.

## Acceptance criteria
- [ ] Setting a level on an inheriting logger succeeds.
