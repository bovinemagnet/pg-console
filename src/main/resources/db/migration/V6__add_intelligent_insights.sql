-- V6: Phase 11 - Intelligent Insights & Automation
-- Creates tables for anomaly detection, predictive analytics, recommendations,
-- natural language queries, runbooks, and scheduled maintenance

-- ============================================================================
-- ANOMALY DETECTION TABLES
-- ============================================================================

-- Metric baselines for anomaly detection (statistical profiles)
CREATE TABLE IF NOT EXISTS pgconsole.metric_baseline (
    id BIGSERIAL PRIMARY KEY,
    instance_id TEXT NOT NULL,
    metric_name TEXT NOT NULL,
    metric_category TEXT NOT NULL,  -- 'system', 'query', 'database'

    -- Statistical baseline values
    baseline_mean DOUBLE PRECISION NOT NULL,
    baseline_stddev DOUBLE PRECISION NOT NULL,
    baseline_min DOUBLE PRECISION,
    baseline_max DOUBLE PRECISION,
    baseline_median DOUBLE PRECISION,
    baseline_p95 DOUBLE PRECISION,
    baseline_p99 DOUBLE PRECISION,
    sample_count INT NOT NULL,

    -- Time-based patterns
    hour_of_day INT,              -- NULL for overall, 0-23 for hourly patterns
    day_of_week INT,              -- NULL for overall, 0-6 for daily patterns (0=Sunday)

    -- Metadata
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,

    UNIQUE(instance_id, metric_name, metric_category, hour_of_day, day_of_week)
);

CREATE INDEX idx_metric_baseline_instance
    ON pgconsole.metric_baseline(instance_id, metric_name);
CREATE INDEX idx_metric_baseline_category
    ON pgconsole.metric_baseline(instance_id, metric_category);

-- Detected anomalies
CREATE TABLE IF NOT EXISTS pgconsole.detected_anomaly (
    id BIGSERIAL PRIMARY KEY,
    instance_id TEXT NOT NULL,
    metric_name TEXT NOT NULL,
    metric_category TEXT NOT NULL,

    -- Anomaly details
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    anomaly_value DOUBLE PRECISION NOT NULL,
    baseline_mean DOUBLE PRECISION NOT NULL,
    baseline_stddev DOUBLE PRECISION NOT NULL,
    deviation_sigma DOUBLE PRECISION NOT NULL,  -- Number of standard deviations

    -- Classification
    severity TEXT NOT NULL CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    anomaly_type TEXT NOT NULL CHECK (anomaly_type IN ('SPIKE', 'DROP', 'TREND', 'PATTERN_BREAK')),
    direction TEXT NOT NULL CHECK (direction IN ('ABOVE', 'BELOW')),

    -- Root cause analysis
    root_cause_suggestion TEXT,
    correlated_metrics JSONB,     -- Other metrics that changed at same time

    -- State tracking
    acknowledged_at TIMESTAMPTZ,
    acknowledged_by TEXT,
    resolved_at TIMESTAMPTZ,
    resolution_notes TEXT,

    -- Link to alert if one was fired
    alert_id BIGINT
);

CREATE INDEX idx_detected_anomaly_instance_time
    ON pgconsole.detected_anomaly(instance_id, detected_at DESC);
CREATE INDEX idx_detected_anomaly_severity
    ON pgconsole.detected_anomaly(severity, detected_at DESC);
CREATE INDEX idx_detected_anomaly_unresolved
    ON pgconsole.detected_anomaly(instance_id) WHERE resolved_at IS NULL;

-- ============================================================================
-- PREDICTIVE ANALYTICS TABLES
-- ============================================================================

-- Forecasts for various metrics
CREATE TABLE IF NOT EXISTS pgconsole.metric_forecast (
    id BIGSERIAL PRIMARY KEY,
    instance_id TEXT NOT NULL,
    metric_name TEXT NOT NULL,
    metric_category TEXT NOT NULL,

    -- Forecast parameters
    forecast_date DATE NOT NULL,
    forecast_value DOUBLE PRECISION NOT NULL,
    confidence_lower DOUBLE PRECISION,        -- Lower bound of confidence interval
    confidence_upper DOUBLE PRECISION,        -- Upper bound of confidence interval
    confidence_level DOUBLE PRECISION,        -- e.g., 0.95 for 95% confidence

    -- Model metadata
    model_type TEXT NOT NULL,                 -- 'LINEAR', 'EXPONENTIAL', 'SEASONAL'
    r_squared DOUBLE PRECISION,               -- Model fit quality
    data_points_used INT NOT NULL,
    training_period_days INT NOT NULL,

    -- Timestamps
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(instance_id, metric_name, forecast_date)
);

CREATE INDEX idx_metric_forecast_instance
    ON pgconsole.metric_forecast(instance_id, metric_name, forecast_date);

-- Capacity thresholds for alerting on forecasts
CREATE TABLE IF NOT EXISTS pgconsole.capacity_threshold (
    id BIGSERIAL PRIMARY KEY,
    instance_id TEXT NOT NULL,
    metric_name TEXT NOT NULL,

    -- Threshold values
    warning_threshold DOUBLE PRECISION NOT NULL,
    critical_threshold DOUBLE PRECISION NOT NULL,
    threshold_type TEXT NOT NULL CHECK (threshold_type IN ('ABSOLUTE', 'PERCENTAGE')),

    -- Notification settings
    notify_days_before INT DEFAULT 7,         -- Days before threshold breach to notify
    notification_channels TEXT[],

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,

    UNIQUE(instance_id, metric_name)
);

-- ============================================================================
-- AUTOMATED RECOMMENDATIONS ENGINE TABLES
-- ============================================================================

-- Unified recommendations from all sources
CREATE TABLE IF NOT EXISTS pgconsole.unified_recommendation (
    id BIGSERIAL PRIMARY KEY,
    instance_id TEXT NOT NULL,

    -- Recommendation details
    source TEXT NOT NULL,                     -- 'INDEX_ADVISOR', 'TABLE_MAINTENANCE', 'QUERY_REGRESSION', 'CONFIG_TUNING', 'ANOMALY'
    recommendation_type TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    rationale TEXT,

    -- Priority and impact
    severity TEXT NOT NULL CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    priority_score INT NOT NULL,              -- 1-100, computed from multiple factors
    estimated_impact TEXT,                    -- 'HIGH', 'MEDIUM', 'LOW'
    estimated_effort TEXT,                    -- 'MINIMAL', 'LOW', 'MEDIUM', 'HIGH'

    -- Suggested action
    suggested_sql TEXT,
    suggested_config JSONB,
    rollback_sql TEXT,

    -- Affected objects
    affected_objects JSONB,                   -- Tables, indexes, queries affected

    -- State tracking
    status TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'IN_PROGRESS', 'APPLIED', 'DISMISSED', 'DEFERRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    applied_at TIMESTAMPTZ,
    applied_by TEXT,
    dismissed_at TIMESTAMPTZ,
    dismissed_by TEXT,
    dismiss_reason TEXT,
    deferred_until DATE,

    -- Effectiveness tracking
    pre_metric_value DOUBLE PRECISION,
    post_metric_value DOUBLE PRECISION,
    effectiveness_rating TEXT CHECK (effectiveness_rating IN ('EXCELLENT', 'GOOD', 'NEUTRAL', 'POOR'))
);

CREATE INDEX idx_unified_recommendation_instance
    ON pgconsole.unified_recommendation(instance_id, status, priority_score DESC);
CREATE INDEX idx_unified_recommendation_severity
    ON pgconsole.unified_recommendation(severity, priority_score DESC);
CREATE INDEX idx_unified_recommendation_source
    ON pgconsole.unified_recommendation(source, created_at DESC);

-- Configuration tuning suggestions
CREATE TABLE IF NOT EXISTS pgconsole.config_tuning_suggestion (
    id BIGSERIAL PRIMARY KEY,
    instance_id TEXT NOT NULL,

    -- Parameter details
    parameter_name TEXT NOT NULL,
    current_value TEXT NOT NULL,
    suggested_value TEXT NOT NULL,
    default_value TEXT,

    -- Context
    category TEXT NOT NULL,                   -- 'MEMORY', 'CONNECTIONS', 'WAL', 'VACUUM', 'QUERY_PLANNER'
    rationale TEXT NOT NULL,
    expected_benefit TEXT,
    potential_risks TEXT,

    -- Thresholds that triggered suggestion
    triggering_metric TEXT,
    triggering_value DOUBLE PRECISION,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    applied_at TIMESTAMPTZ,
    requires_restart BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_config_tuning_instance
    ON pgconsole.config_tuning_suggestion(instance_id, category);

-- ============================================================================
-- NATURAL LANGUAGE QUERIES TABLES
-- ============================================================================

-- Query intent patterns for NL parsing
CREATE TABLE IF NOT EXISTS pgconsole.nl_query_pattern (
    id BIGSERIAL PRIMARY KEY,
    pattern TEXT NOT NULL,                    -- Regex pattern to match
    intent TEXT NOT NULL,                     -- 'SLOW_QUERIES', 'TABLE_GROWTH', 'LOCKS', etc.
    target_endpoint TEXT NOT NULL,            -- API endpoint or dashboard path
    parameters JSONB,                         -- Extracted parameters mapping
    priority INT NOT NULL DEFAULT 0,          -- Higher priority patterns matched first
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- Examples for documentation
    example_queries TEXT[],

    UNIQUE(pattern)
);

-- Insert default patterns
INSERT INTO pgconsole.nl_query_pattern (pattern, intent, target_endpoint, parameters, priority, example_queries)
VALUES
    ('(?i)slow\s+quer(y|ies).*yesterday', 'SLOW_QUERIES_YESTERDAY', '/slow-queries', '{"timeRange": "yesterday"}', 100, ARRAY['Show me slow queries from yesterday', 'slow queries yesterday']),
    ('(?i)slow\s+quer(y|ies).*last\s+(\d+)\s+hours?', 'SLOW_QUERIES_HOURS', '/slow-queries', '{"timeRange": "hours", "hoursParam": 2}', 90, ARRAY['slow queries last 3 hours', 'show slow queries from last 2 hours']),
    ('(?i)slow\s+quer(y|ies)', 'SLOW_QUERIES', '/slow-queries', '{}', 50, ARRAY['slow queries', 'show me slow queries']),
    ('(?i)which\s+tables?\s+(are\s+)?growing\s+(fastest|quickly)', 'TABLE_GROWTH', '/storage-insights', '{"sort": "growth"}', 100, ARRAY['Which tables are growing fastest?', 'tables growing quickly']),
    ('(?i)table\s+(size|growth|storage)', 'TABLE_SIZE', '/tables', '{}', 50, ARRAY['table sizes', 'show table storage']),
    ('(?i)why\s+(is\s+)?(the\s+)?database\s+slow', 'SLOW_DIAGNOSIS', '/insights', '{"mode": "diagnosis"}', 100, ARRAY['Why is the database slow?', 'why is database slow right now']),
    ('(?i)(current\s+)?lock(s|ing)?', 'LOCKS', '/locks', '{}', 50, ARRAY['show locks', 'current locking']),
    ('(?i)block(ed|ing)', 'BLOCKING', '/locks', '{"focus": "blocking"}', 60, ARRAY['blocked queries', 'show blocking']),
    ('(?i)active\s+(connections?|sessions?|queries?)', 'ACTIVITY', '/activity', '{}', 50, ARRAY['active connections', 'show active sessions']),
    ('(?i)replication\s+(status|lag)', 'REPLICATION', '/replication', '{}', 50, ARRAY['replication status', 'show replication lag']),
    ('(?i)(index|indexes)\s+(recommend|suggest|advice)', 'INDEX_ADVISOR', '/index-advisor', '{}', 50, ARRAY['index recommendations', 'suggest indexes']),
    ('(?i)vacuum\s+(status|progress|recommend)', 'VACUUM', '/vacuum-progress', '{}', 50, ARRAY['vacuum status', 'vacuum recommendations']),
    ('(?i)(disk|storage)\s+(usage|space)', 'STORAGE', '/storage-insights', '{}', 50, ARRAY['disk usage', 'storage space']),
    ('(?i)anomal(y|ies)', 'ANOMALIES', '/anomalies', '{}', 50, ARRAY['show anomalies', 'detected anomalies']),
    ('(?i)forecast|predict', 'FORECASTS', '/forecasts', '{}', 50, ARRAY['storage forecast', 'predict growth'])
ON CONFLICT (pattern) DO NOTHING;

-- NL query history for learning
CREATE TABLE IF NOT EXISTS pgconsole.nl_query_history (
    id BIGSERIAL PRIMARY KEY,
    query_text TEXT NOT NULL,
    matched_intent TEXT,
    resolved_to TEXT,
    successful BOOLEAN NOT NULL DEFAULT TRUE,
    user_feedback TEXT CHECK (user_feedback IN ('HELPFUL', 'NOT_HELPFUL', 'WRONG')),
    queried_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    queried_by TEXT
);

CREATE INDEX idx_nl_query_history_time
    ON pgconsole.nl_query_history(queried_at DESC);

-- ============================================================================
-- RUNBOOK INTEGRATION TABLES
-- ============================================================================

-- Runbook definitions
CREATE TABLE IF NOT EXISTS pgconsole.runbook (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    description TEXT,
    category TEXT NOT NULL,                   -- 'INCIDENT', 'MAINTENANCE', 'TROUBLESHOOTING', 'RECOVERY'

    -- Trigger conditions
    trigger_type TEXT NOT NULL CHECK (trigger_type IN ('MANUAL', 'ALERT', 'ANOMALY', 'SCHEDULED')),
    trigger_conditions JSONB,                 -- Conditions that auto-trigger this runbook

    -- Steps stored as JSON array
    steps JSONB NOT NULL,

    -- Metadata
    version INT NOT NULL DEFAULT 1,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,

    -- Estimated duration
    estimated_duration_minutes INT
);

-- Insert default runbooks
INSERT INTO pgconsole.runbook (name, title, description, category, trigger_type, trigger_conditions, steps, estimated_duration_minutes)
VALUES
    ('high_connection_usage', 'High Connection Usage Response', 'Steps to diagnose and resolve high connection usage', 'INCIDENT', 'ALERT',
     '{"alert_type": "CONNECTION_THRESHOLD", "severity": ["CRITICAL", "HIGH"]}',
     '[
         {"order": 1, "title": "Check Connection Count", "description": "View current active connections", "action_type": "NAVIGATE", "action": "/activity", "auto_execute": true},
         {"order": 2, "title": "Identify Long-Running Queries", "description": "Look for queries running longer than expected", "action_type": "NAVIGATE", "action": "/slow-queries?sort=duration", "auto_execute": false},
         {"order": 3, "title": "Check for Idle Connections", "description": "Identify idle-in-transaction connections", "action_type": "QUERY", "action": "SELECT * FROM pg_stat_activity WHERE state = ''idle in transaction'' AND now() - xact_start > interval ''5 minutes''", "auto_execute": true},
         {"order": 4, "title": "Consider Connection Pooling", "description": "If connections are legitimately high, consider adding PgBouncer", "action_type": "DOCUMENTATION", "action": "https://www.pgbouncer.org/", "auto_execute": false},
         {"order": 5, "title": "Terminate Idle Connections", "description": "If necessary, terminate long-idle connections", "action_type": "SQL_TEMPLATE", "action": "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = ''idle'' AND now() - state_change > interval ''30 minutes''", "auto_execute": false, "requires_confirmation": true}
     ]', 15),

    ('blocked_queries', 'Blocked Queries Investigation', 'Diagnose and resolve query blocking issues', 'TROUBLESHOOTING', 'ALERT',
     '{"alert_type": "BLOCKED_QUERIES", "min_count": 1}',
     '[
         {"order": 1, "title": "View Blocking Tree", "description": "See the blocking hierarchy", "action_type": "NAVIGATE", "action": "/locks", "auto_execute": true},
         {"order": 2, "title": "Identify Root Blocker", "description": "Find the query at the top of the blocking chain", "action_type": "QUERY", "action": "SELECT * FROM pg_stat_activity WHERE pid IN (SELECT DISTINCT blocking_pid FROM pg_locks WHERE granted = false)", "auto_execute": true},
         {"order": 3, "title": "Assess Blocker Query", "description": "Determine if the blocking query can be safely terminated", "action_type": "MANUAL", "action": "Review the blocking query and determine appropriate action", "auto_execute": false},
         {"order": 4, "title": "Terminate if Necessary", "description": "Cancel or terminate the blocking query", "action_type": "SQL_TEMPLATE", "action": "SELECT pg_cancel_backend({pid})", "auto_execute": false, "requires_confirmation": true}
     ]', 10),

    ('low_cache_hit', 'Low Cache Hit Ratio Investigation', 'Diagnose poor cache hit ratio', 'TROUBLESHOOTING', 'ALERT',
     '{"alert_type": "CACHE_HIT_RATIO", "threshold": 0.90}',
     '[
         {"order": 1, "title": "Check Current Cache Stats", "description": "View cache hit ratio details", "action_type": "NAVIGATE", "action": "/", "auto_execute": true},
         {"order": 2, "title": "Check shared_buffers Setting", "description": "Review current shared_buffers configuration", "action_type": "QUERY", "action": "SHOW shared_buffers", "auto_execute": true},
         {"order": 3, "title": "Identify Tables with Poor Cache Usage", "description": "Find tables with low cache hit rates", "action_type": "QUERY", "action": "SELECT schemaname, relname, heap_blks_hit, heap_blks_read, CASE WHEN heap_blks_hit + heap_blks_read > 0 THEN round(heap_blks_hit::numeric / (heap_blks_hit + heap_blks_read) * 100, 2) ELSE 0 END as hit_ratio FROM pg_statio_user_tables ORDER BY heap_blks_read DESC LIMIT 20", "auto_execute": true},
         {"order": 4, "title": "Consider Increasing shared_buffers", "description": "If memory is available, consider increasing shared_buffers", "action_type": "DOCUMENTATION", "action": "Recommended: 25% of system RAM for dedicated database servers", "auto_execute": false}
     ]', 20),

    ('vacuum_maintenance', 'Regular Vacuum Maintenance', 'Perform routine vacuum maintenance', 'MAINTENANCE', 'SCHEDULED',
     '{"schedule": "0 3 * * 0"}',
     '[
         {"order": 1, "title": "Check Tables Needing Vacuum", "description": "Identify tables with high dead tuple counts", "action_type": "NAVIGATE", "action": "/table-maintenance", "auto_execute": true},
         {"order": 2, "title": "Review Autovacuum Settings", "description": "Check current autovacuum configuration", "action_type": "QUERY", "action": "SELECT name, setting FROM pg_settings WHERE name LIKE ''autovacuum%''", "auto_execute": true},
         {"order": 3, "title": "Run VACUUM ANALYZE on Priority Tables", "description": "Vacuum tables with highest dead tuple ratios first", "action_type": "SQL_TEMPLATE", "action": "VACUUM ANALYZE {table_name}", "auto_execute": false}
     ]', 30)
ON CONFLICT (name) DO NOTHING;

-- Runbook execution history
CREATE TABLE IF NOT EXISTS pgconsole.runbook_execution (
    id BIGSERIAL PRIMARY KEY,
    runbook_id BIGINT NOT NULL REFERENCES pgconsole.runbook(id),
    instance_id TEXT NOT NULL,

    -- Execution details
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')),

    -- Trigger context
    triggered_by TEXT NOT NULL CHECK (triggered_by IN ('MANUAL', 'ALERT', 'ANOMALY', 'SCHEDULED')),
    trigger_context JSONB,                    -- Alert ID, anomaly ID, etc.

    -- Step progress
    current_step INT NOT NULL DEFAULT 1,
    step_results JSONB,                       -- Results from each step

    -- User context
    executed_by TEXT,
    notes TEXT
);

CREATE INDEX idx_runbook_execution_instance
    ON pgconsole.runbook_execution(instance_id, started_at DESC);
CREATE INDEX idx_runbook_execution_status
    ON pgconsole.runbook_execution(status) WHERE status = 'IN_PROGRESS';

-- ============================================================================
-- SCHEDULED MAINTENANCE AUTOMATION TABLES
-- ============================================================================

-- Scheduled maintenance tasks
CREATE TABLE IF NOT EXISTS pgconsole.scheduled_maintenance (
    id BIGSERIAL PRIMARY KEY,
    instance_id TEXT NOT NULL,

    -- Task details
    task_type TEXT NOT NULL CHECK (task_type IN ('VACUUM', 'VACUUM_FULL', 'ANALYSE', 'REINDEX', 'CLUSTER')),
    target_object TEXT NOT NULL,              -- Table name or '*' for all
    target_schema TEXT NOT NULL DEFAULT 'public',

    -- Scheduling
    schedule_type TEXT NOT NULL CHECK (schedule_type IN ('INTELLIGENT', 'CRON', 'ONE_TIME')),
    cron_expression TEXT,                     -- For CRON type
    scheduled_time TIMESTAMPTZ,               -- For ONE_TIME type

    -- Intelligent scheduling parameters
    activity_threshold DOUBLE PRECISION,      -- Run when activity below this %
    preferred_window_start TIME,              -- Preferred maintenance start time
    preferred_window_end TIME,                -- Preferred maintenance end time

    -- Execution limits
    max_duration_minutes INT DEFAULT 60,
    priority INT NOT NULL DEFAULT 5,          -- 1-10, higher = more important

    -- State
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_run_at TIMESTAMPTZ,
    next_run_at TIMESTAMPTZ,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT
);

CREATE INDEX idx_scheduled_maintenance_instance
    ON pgconsole.scheduled_maintenance(instance_id, enabled);
CREATE INDEX idx_scheduled_maintenance_next_run
    ON pgconsole.scheduled_maintenance(next_run_at) WHERE enabled = TRUE;

-- Maintenance execution history
CREATE TABLE IF NOT EXISTS pgconsole.maintenance_execution (
    id BIGSERIAL PRIMARY KEY,
    scheduled_maintenance_id BIGINT REFERENCES pgconsole.scheduled_maintenance(id),
    instance_id TEXT NOT NULL,

    -- Execution details
    task_type TEXT NOT NULL,
    target_object TEXT NOT NULL,

    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'RUNNING' CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),

    -- Results
    rows_affected BIGINT,
    pages_removed BIGINT,                     -- For VACUUM
    duration_seconds INT,
    error_message TEXT,

    -- Pre/post metrics
    pre_dead_tuples BIGINT,
    post_dead_tuples BIGINT,
    pre_table_size BIGINT,
    post_table_size BIGINT
);

CREATE INDEX idx_maintenance_execution_instance
    ON pgconsole.maintenance_execution(instance_id, started_at DESC);
CREATE INDEX idx_maintenance_execution_task
    ON pgconsole.maintenance_execution(scheduled_maintenance_id, started_at DESC);

-- Activity patterns for intelligent scheduling
CREATE TABLE IF NOT EXISTS pgconsole.activity_pattern (
    id BIGSERIAL PRIMARY KEY,
    instance_id TEXT NOT NULL,

    -- Time window
    day_of_week INT NOT NULL,                 -- 0-6 (Sunday-Saturday)
    hour_of_day INT NOT NULL,                 -- 0-23

    -- Activity statistics
    avg_connections DOUBLE PRECISION NOT NULL,
    avg_active_queries DOUBLE PRECISION NOT NULL,
    avg_transactions_per_hour DOUBLE PRECISION NOT NULL,

    -- Calculated scores
    activity_score DOUBLE PRECISION NOT NULL, -- 0-100, higher = more active
    maintenance_suitability DOUBLE PRECISION NOT NULL, -- 0-100, higher = better for maintenance

    -- Metadata
    sample_count INT NOT NULL,
    last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(instance_id, day_of_week, hour_of_day)
);

CREATE INDEX idx_activity_pattern_instance
    ON pgconsole.activity_pattern(instance_id, maintenance_suitability DESC);

-- ============================================================================
-- INSIGHTS DASHBOARD AGGREGATE TABLE
-- ============================================================================

-- Aggregated insights for dashboard display
CREATE TABLE IF NOT EXISTS pgconsole.insight_summary (
    id BIGSERIAL PRIMARY KEY,
    instance_id TEXT NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Anomaly summary
    anomaly_count_critical INT NOT NULL DEFAULT 0,
    anomaly_count_high INT NOT NULL DEFAULT 0,
    anomaly_count_medium INT NOT NULL DEFAULT 0,
    anomaly_count_low INT NOT NULL DEFAULT 0,

    -- Recommendation summary
    recommendation_count_critical INT NOT NULL DEFAULT 0,
    recommendation_count_high INT NOT NULL DEFAULT 0,
    recommendation_count_medium INT NOT NULL DEFAULT 0,
    recommendation_count_low INT NOT NULL DEFAULT 0,

    -- Forecast alerts
    storage_days_until_warning INT,
    storage_days_until_critical INT,
    connections_days_until_warning INT,

    -- Health score (0-100)
    overall_health_score INT NOT NULL,

    -- Top concerns (for quick display)
    top_concerns JSONB,

    UNIQUE(instance_id, calculated_at)
);

CREATE INDEX idx_insight_summary_instance
    ON pgconsole.insight_summary(instance_id, calculated_at DESC);
