-- M16: "one running session per instance" was enforced only by a check-then-insert
-- race in the service. Two concurrent starts could both pass getActiveSession and
-- create two 'running' rows; getActiveSession's LIMIT 1 then hides one permanently.
--
-- Enforce the invariant in the database with a partial unique index so a concurrent
-- second start fails with a unique violation instead of creating a phantom session.

CREATE UNIQUE INDEX IF NOT EXISTS uq_stopwatch_running_per_instance
    ON pgconsole.stopwatch_session (instance_id)
    WHERE status = 'running';
