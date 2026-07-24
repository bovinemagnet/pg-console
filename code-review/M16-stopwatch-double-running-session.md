# M16 — Stopwatch "one running session per instance" is not enforced

- **Severity:** Major (correctness/concurrency)
- **Area:** Persistence
- **Locations:** `repository/StopwatchRepository.java:49-74,178-203`; `service/StopwatchService.java:64-76`; `db/migration/V2__stopwatch_sessions.sql`

## Problem
The invariant is enforced only by check-then-insert; there is no partial unique index `ON stopwatch_session(instance_id) WHERE status = 'running'`.

## Impact
Two concurrent "start" clicks both pass `getActiveSession` and create two running sessions; `getActiveSession`'s `LIMIT 1` then hides one permanently (it can never be stopped).

## Recommended fix
- Add the partial unique index; handle the unique-violation as "already running".

## Acceptance criteria
- [ ] Concurrent starts result in exactly one running session.
