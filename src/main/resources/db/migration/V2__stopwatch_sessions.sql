-- Stopwatch sessions table for recording before/after metric snapshots
CREATE TABLE IF NOT EXISTS pgconsole.stopwatch_session (
    id              BIGSERIAL PRIMARY KEY,
    instance_id     VARCHAR(255) NOT NULL DEFAULT 'default',
    status          VARCHAR(20) NOT NULL DEFAULT 'running',
    started_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    stopped_at      TIMESTAMP WITH TIME ZONE,
    notes           TEXT,
    start_snapshot  JSONB,
    end_snapshot    JSONB,
    top_queries_start JSONB,
    top_queries_end   JSONB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_stopwatch_session_instance_started
    ON pgconsole.stopwatch_session (instance_id, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_stopwatch_session_status
    ON pgconsole.stopwatch_session (status);
