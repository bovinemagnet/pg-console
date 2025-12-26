-- pgconsole history schema
-- Stores sampled metrics for trend visualisation

-- System-level metrics sampled periodically
CREATE TABLE pgconsole.system_metrics_history (
    id BIGSERIAL PRIMARY KEY,
    sampled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Connection metrics
    total_connections INTEGER NOT NULL,
    max_connections INTEGER NOT NULL,
    active_queries INTEGER NOT NULL,
    idle_connections INTEGER NOT NULL,
    idle_in_transaction INTEGER NOT NULL,

    -- Blocking metrics
    blocked_queries INTEGER NOT NULL,
    longest_query_seconds DOUBLE PRECISION,
    longest_transaction_seconds DOUBLE PRECISION,

    -- Cache metrics (aggregated across databases)
    cache_hit_ratio DOUBLE PRECISION,

    -- Database size (total across all monitored databases)
    total_database_size_bytes BIGINT
);

-- Index for time-based queries
CREATE INDEX idx_system_metrics_sampled_at ON pgconsole.system_metrics_history(sampled_at DESC);

-- Query metrics from pg_stat_statements sampled periodically
CREATE TABLE pgconsole.query_metrics_history (
    id BIGSERIAL PRIMARY KEY,
    sampled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Query identification
    query_id TEXT NOT NULL,
    query_text TEXT,

    -- Cumulative counters (delta can be computed between samples)
    total_calls BIGINT NOT NULL,
    total_time_ms DOUBLE PRECISION NOT NULL,
    total_rows BIGINT NOT NULL,

    -- Point-in-time stats
    mean_time_ms DOUBLE PRECISION NOT NULL,
    min_time_ms DOUBLE PRECISION,
    max_time_ms DOUBLE PRECISION,
    stddev_time_ms DOUBLE PRECISION,

    -- Block I/O
    shared_blks_hit BIGINT,
    shared_blks_read BIGINT,
    temp_blks_written BIGINT
);

-- Indexes for query metrics
CREATE INDEX idx_query_metrics_sampled_at ON pgconsole.query_metrics_history(sampled_at DESC);
CREATE INDEX idx_query_metrics_query_id ON pgconsole.query_metrics_history(query_id, sampled_at DESC);

-- Per-database metrics history
CREATE TABLE pgconsole.database_metrics_history (
    id BIGSERIAL PRIMARY KEY,
    sampled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Database identification
    database_name TEXT NOT NULL,

    -- Connection metrics
    num_backends INTEGER NOT NULL,

    -- Transaction metrics
    xact_commit BIGINT NOT NULL,
    xact_rollback BIGINT NOT NULL,

    -- Cache metrics
    blks_hit BIGINT NOT NULL,
    blks_read BIGINT NOT NULL,
    cache_hit_ratio DOUBLE PRECISION,

    -- Tuple metrics
    tup_returned BIGINT,
    tup_fetched BIGINT,
    tup_inserted BIGINT,
    tup_updated BIGINT,
    tup_deleted BIGINT,

    -- Problem indicators
    deadlocks BIGINT,
    conflicts BIGINT,
    temp_files BIGINT,
    temp_bytes BIGINT,

    -- Size
    database_size_bytes BIGINT
);

-- Indexes for database metrics
CREATE INDEX idx_database_metrics_sampled_at ON pgconsole.database_metrics_history(sampled_at DESC);
CREATE INDEX idx_database_metrics_db_name ON pgconsole.database_metrics_history(database_name, sampled_at DESC);
