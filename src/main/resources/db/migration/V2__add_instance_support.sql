-- Add multi-instance support to history tables
-- Each record is now tagged with the instance it was sampled from

-- Add instance_id column to system_metrics_history
ALTER TABLE pgconsole.system_metrics_history
    ADD COLUMN IF NOT EXISTS instance_id TEXT NOT NULL DEFAULT 'default';

-- Add instance_id column to query_metrics_history
ALTER TABLE pgconsole.query_metrics_history
    ADD COLUMN IF NOT EXISTS instance_id TEXT NOT NULL DEFAULT 'default';

-- Add instance_id column to database_metrics_history
ALTER TABLE pgconsole.database_metrics_history
    ADD COLUMN IF NOT EXISTS instance_id TEXT NOT NULL DEFAULT 'default';

-- Update indexes to include instance_id for better query performance

-- System metrics indexes
DROP INDEX IF EXISTS pgconsole.idx_system_metrics_sampled_at;
CREATE INDEX idx_system_metrics_instance_sampled
    ON pgconsole.system_metrics_history(instance_id, sampled_at DESC);

-- Query metrics indexes
DROP INDEX IF EXISTS pgconsole.idx_query_metrics_sampled_at;
DROP INDEX IF EXISTS pgconsole.idx_query_metrics_query_id;
CREATE INDEX idx_query_metrics_instance_sampled
    ON pgconsole.query_metrics_history(instance_id, sampled_at DESC);
CREATE INDEX idx_query_metrics_instance_query
    ON pgconsole.query_metrics_history(instance_id, query_id, sampled_at DESC);

-- Database metrics indexes
DROP INDEX IF EXISTS pgconsole.idx_database_metrics_sampled_at;
DROP INDEX IF EXISTS pgconsole.idx_database_metrics_db_name;
CREATE INDEX idx_database_metrics_instance_sampled
    ON pgconsole.database_metrics_history(instance_id, sampled_at DESC);
CREATE INDEX idx_database_metrics_instance_db
    ON pgconsole.database_metrics_history(instance_id, database_name, sampled_at DESC);
