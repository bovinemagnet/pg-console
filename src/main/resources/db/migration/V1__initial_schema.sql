-- pgconsole initial schema
-- Author: Paul Snow
-- Version: 0.0.0
--
-- This schema provides the complete database structure for pg-console,
-- including history tables, audit logging, schema comparison, notifications,
-- intelligent insights, and custom dashboards.

-- ============================================================================
-- HISTORY TABLES (metrics sampling for trend visualisation)
-- ============================================================================

-- System-level metrics sampled periodically
CREATE TABLE pgconsole.system_metrics_history (
    id BIGSERIAL PRIMARY KEY,
    sampled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    instance_id TEXT NOT NULL DEFAULT 'default',

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

CREATE INDEX idx_system_metrics_instance_sampled
    ON pgconsole.system_metrics_history(instance_id, sampled_at DESC);

-- Query metrics from pg_stat_statements sampled periodically
CREATE TABLE pgconsole.query_metrics_history (
    id BIGSERIAL PRIMARY KEY,
    sampled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    instance_id TEXT NOT NULL DEFAULT 'default',

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

CREATE INDEX idx_query_metrics_instance_sampled
    ON pgconsole.query_metrics_history(instance_id, sampled_at DESC);
CREATE INDEX idx_query_metrics_instance_query
    ON pgconsole.query_metrics_history(instance_id, query_id, sampled_at DESC);

-- Per-database metrics history
CREATE TABLE pgconsole.database_metrics_history (
    id BIGSERIAL PRIMARY KEY,
    sampled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    instance_id TEXT NOT NULL DEFAULT 'default',

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

CREATE INDEX idx_database_metrics_instance_sampled
    ON pgconsole.database_metrics_history(instance_id, sampled_at DESC);
CREATE INDEX idx_database_metrics_instance_db
    ON pgconsole.database_metrics_history(instance_id, database_name, sampled_at DESC);

-- ============================================================================
-- AUDIT AND BOOKMARKS TABLES
-- ============================================================================

-- Audit log for tracking admin actions
CREATE TABLE IF NOT EXISTS pgconsole.audit_log (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    instance_id TEXT NOT NULL,
    username TEXT,
    action TEXT NOT NULL,
    target_type TEXT,
    target_id TEXT,
    details JSONB,
    client_ip TEXT,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp ON pgconsole.audit_log(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_instance ON pgconsole.audit_log(instance_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON pgconsole.audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_log_username ON pgconsole.audit_log(username);

-- Query bookmarks for tracking and annotating slow queries
CREATE TABLE IF NOT EXISTS pgconsole.query_bookmark (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    instance_id TEXT NOT NULL,
    query_id TEXT NOT NULL,
    query_text TEXT,
    title TEXT,
    notes TEXT,
    tags TEXT[],
    created_by TEXT,
    priority TEXT DEFAULT 'normal',
    status TEXT DEFAULT 'active',
    UNIQUE(instance_id, query_id)
);

CREATE INDEX IF NOT EXISTS idx_query_bookmark_instance ON pgconsole.query_bookmark(instance_id);
CREATE INDEX IF NOT EXISTS idx_query_bookmark_status ON pgconsole.query_bookmark(status);
CREATE INDEX IF NOT EXISTS idx_query_bookmark_tags ON pgconsole.query_bookmark USING GIN(tags);

-- Scheduled report configuration
CREATE TABLE IF NOT EXISTS pgconsole.scheduled_report (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    name TEXT NOT NULL,
    instance_id TEXT NOT NULL,
    report_type TEXT NOT NULL,
    schedule TEXT NOT NULL,
    recipients TEXT[] NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_run_at TIMESTAMPTZ,
    next_run_at TIMESTAMPTZ,
    config JSONB
);

CREATE INDEX IF NOT EXISTS idx_scheduled_report_instance ON pgconsole.scheduled_report(instance_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_report_next_run ON pgconsole.scheduled_report(next_run_at) WHERE enabled = TRUE;

-- ============================================================================
-- SCHEMA COMPARISON TABLES
-- ============================================================================

-- Comparison profiles for saved configurations
CREATE TABLE IF NOT EXISTS pgconsole.comparison_profile (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    source_instance TEXT NOT NULL,
    destination_instance TEXT NOT NULL,
    source_schema TEXT NOT NULL DEFAULT 'public',
    destination_schema TEXT NOT NULL DEFAULT 'public',
    filter_config JSONB,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_run_at TIMESTAMPTZ,
    last_run_summary JSONB
);

CREATE INDEX IF NOT EXISTS idx_comparison_profile_instances
    ON pgconsole.comparison_profile(source_instance, destination_instance);

CREATE INDEX IF NOT EXISTS idx_comparison_profile_default
    ON pgconsole.comparison_profile(is_default) WHERE is_default = TRUE;

-- Comparison history for audit and drift detection
CREATE TABLE IF NOT EXISTS pgconsole.comparison_history (
    id BIGSERIAL PRIMARY KEY,
    compared_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    source_instance TEXT NOT NULL,
    destination_instance TEXT NOT NULL,
    source_schema TEXT NOT NULL,
    destination_schema TEXT NOT NULL,
    performed_by TEXT,
    missing_count INT NOT NULL DEFAULT 0,
    extra_count INT NOT NULL DEFAULT 0,
    modified_count INT NOT NULL DEFAULT 0,
    matching_count INT NOT NULL DEFAULT 0,
    profile_name TEXT,
    result_snapshot JSONB,
    filter_config JSONB
);

CREATE INDEX IF NOT EXISTS idx_comparison_history_time
    ON pgconsole.comparison_history(compared_at DESC);

CREATE INDEX IF NOT EXISTS idx_comparison_history_instances
    ON pgconsole.comparison_history(source_instance, destination_instance);

COMMENT ON TABLE pgconsole.comparison_profile IS 'Saved schema comparison configurations for quick re-runs';
COMMENT ON TABLE pgconsole.comparison_history IS 'Audit log of schema comparisons for drift detection';

-- ============================================================================
-- NOTIFICATION AND ALERTING TABLES
-- ============================================================================

-- Notification channels configuration
CREATE TABLE IF NOT EXISTS pgconsole.notification_channel (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    channel_type TEXT NOT NULL, -- SLACK, TEAMS, PAGERDUTY, DISCORD, EMAIL
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config JSONB NOT NULL DEFAULT '{}',
    severity_filter TEXT[], -- NULL = all severities, or array of: CRITICAL, HIGH, MEDIUM, LOW
    alert_type_filter TEXT[], -- NULL = all types, or specific alert types
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMPTZ,
    success_count INT NOT NULL DEFAULT 0,
    failure_count INT NOT NULL DEFAULT 0,
    -- Additional columns for filtering and rate limiting
    instance_filter TEXT[],
    rate_limit_per_hour INT,
    test_mode BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_notification_channel_type ON pgconsole.notification_channel(channel_type);
CREATE INDEX idx_notification_channel_enabled ON pgconsole.notification_channel(enabled) WHERE enabled = TRUE;

-- Escalation policies
CREATE TABLE IF NOT EXISTS pgconsole.escalation_policy (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    repeat_count INT NOT NULL DEFAULT 0, -- 0 = no repeat, N = repeat N times after all tiers
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Escalation tiers (linked to policies)
CREATE TABLE IF NOT EXISTS pgconsole.escalation_tier (
    id BIGSERIAL PRIMARY KEY,
    policy_id BIGINT NOT NULL REFERENCES pgconsole.escalation_policy(id) ON DELETE CASCADE,
    tier_order INT NOT NULL, -- 1, 2, 3...
    delay_minutes INT NOT NULL DEFAULT 0, -- Time to wait before escalating to next tier
    channel_ids BIGINT[] NOT NULL, -- Array of notification_channel IDs
    UNIQUE(policy_id, tier_order)
);

CREATE INDEX idx_escalation_tier_policy ON pgconsole.escalation_tier(policy_id);

-- Maintenance windows (suppress alerts during maintenance)
CREATE TABLE IF NOT EXISTS pgconsole.maintenance_window (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence_pattern TEXT, -- DAILY, WEEKLY, MONTHLY, or cron expression
    instance_filter TEXT[], -- NULL = all instances, or specific instance names
    alert_type_filter TEXT[], -- NULL = all alert types
    created_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_window CHECK (end_time > start_time)
);

CREATE INDEX idx_maintenance_window_active ON pgconsole.maintenance_window(start_time, end_time);

-- Alert acknowledgements
CREATE TABLE IF NOT EXISTS pgconsole.alert_acknowledgement (
    id BIGSERIAL PRIMARY KEY,
    alert_id TEXT NOT NULL, -- Unique identifier for the alert
    acknowledged_by TEXT NOT NULL,
    acknowledged_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    note TEXT,
    expires_at TIMESTAMPTZ, -- NULL = permanent until resolved
    UNIQUE(alert_id)
);

CREATE INDEX idx_alert_ack_alert ON pgconsole.alert_acknowledgement(alert_id);
CREATE INDEX idx_alert_ack_expires ON pgconsole.alert_acknowledgement(expires_at) WHERE expires_at IS NOT NULL;

-- Alert silences (pattern-based suppression)
CREATE TABLE IF NOT EXISTS pgconsole.alert_silence (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    matchers JSONB NOT NULL, -- JSON array of {field, operator, value} matchers
    start_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    end_time TIMESTAMPTZ NOT NULL,
    created_by TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_silence CHECK (end_time > start_time)
);

CREATE INDEX idx_alert_silence_active ON pgconsole.alert_silence(start_time, end_time);

-- Notification history (audit log of all notifications)
CREATE TABLE IF NOT EXISTS pgconsole.notification_history (
    id BIGSERIAL PRIMARY KEY,
    channel_id BIGINT REFERENCES pgconsole.notification_channel(id) ON DELETE SET NULL,
    channel_name TEXT NOT NULL, -- Denormalised for history preservation
    channel_type TEXT NOT NULL,
    alert_id TEXT NOT NULL,
    alert_type TEXT NOT NULL,
    alert_severity TEXT NOT NULL,
    alert_message TEXT NOT NULL,
    instance_name TEXT,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    success BOOLEAN NOT NULL,
    response_code INT,
    response_body TEXT,
    error_message TEXT,
    escalation_tier INT, -- Which tier triggered this notification
    dedup_key TEXT -- For PagerDuty deduplication
);

CREATE INDEX idx_notification_history_channel ON pgconsole.notification_history(channel_id);
CREATE INDEX idx_notification_history_alert ON pgconsole.notification_history(alert_id);
CREATE INDEX idx_notification_history_time ON pgconsole.notification_history(sent_at DESC);
CREATE INDEX idx_notification_history_success ON pgconsole.notification_history(success) WHERE success = FALSE;

-- Active alerts tracking (for escalation)
CREATE TABLE IF NOT EXISTS pgconsole.active_alert (
    id BIGSERIAL PRIMARY KEY,
    alert_id TEXT NOT NULL UNIQUE,
    alert_type TEXT NOT NULL,
    alert_severity TEXT NOT NULL,
    alert_message TEXT NOT NULL,
    instance_name TEXT,
    fired_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_notification_at TIMESTAMPTZ,
    current_escalation_tier INT NOT NULL DEFAULT 1,
    escalation_policy_id BIGINT REFERENCES pgconsole.escalation_policy(id) ON DELETE SET NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMPTZ,
    metadata JSONB DEFAULT '{}'
);

CREATE INDEX idx_active_alert_id ON pgconsole.active_alert(alert_id);
CREATE INDEX idx_active_alert_unresolved ON pgconsole.active_alert(resolved, acknowledged) WHERE resolved = FALSE;
CREATE INDEX idx_active_alert_escalation ON pgconsole.active_alert(escalation_policy_id, current_escalation_tier) WHERE resolved = FALSE;

-- Email digest queue
CREATE TABLE IF NOT EXISTS pgconsole.email_digest_queue (
    id BIGSERIAL PRIMARY KEY,
    recipient TEXT NOT NULL,
    alert_id TEXT NOT NULL,
    alert_type TEXT NOT NULL,
    alert_severity TEXT NOT NULL,
    alert_message TEXT NOT NULL,
    instance_name TEXT,
    queued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMPTZ
);

CREATE INDEX idx_email_digest_pending ON pgconsole.email_digest_queue(recipient, processed) WHERE processed = FALSE;

-- Insert default escalation policy
INSERT INTO pgconsole.escalation_policy (name, description, enabled, repeat_count)
VALUES ('Default', 'Default escalation policy for all alerts', TRUE, 0)
ON CONFLICT (name) DO NOTHING;

-- ============================================================================
-- INTELLIGENT INSIGHTS TABLES
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
    estimated_duration_minutes INT,

    -- Auto-execution capability
    auto_executable BOOLEAN DEFAULT FALSE
);

-- Insert default runbooks
INSERT INTO pgconsole.runbook (name, title, description, category, trigger_type, trigger_conditions, steps, estimated_duration_minutes, auto_executable)
VALUES
    ('high_connection_usage', 'High Connection Usage Response', 'Steps to diagnose and resolve high connection usage', 'INCIDENT', 'ALERT',
     '{"alert_type": "CONNECTION_THRESHOLD", "severity": ["CRITICAL", "HIGH"]}',
     '[
         {"order": 1, "title": "Check Connection Count", "description": "View current active connections", "action_type": "NAVIGATE", "action": "/activity", "auto_execute": true},
         {"order": 2, "title": "Identify Long-Running Queries", "description": "Look for queries running longer than expected", "action_type": "NAVIGATE", "action": "/slow-queries?sort=duration", "auto_execute": false},
         {"order": 3, "title": "Check for Idle Connections", "description": "Identify idle-in-transaction connections", "action_type": "QUERY", "action": "SELECT * FROM pg_stat_activity WHERE state = ''idle in transaction'' AND now() - xact_start > interval ''5 minutes''", "auto_execute": true},
         {"order": 4, "title": "Consider Connection Pooling", "description": "If connections are legitimately high, consider adding PgBouncer", "action_type": "DOCUMENTATION", "action": "https://www.pgbouncer.org/", "auto_execute": false},
         {"order": 5, "title": "Terminate Idle Connections", "description": "If necessary, terminate long-idle connections", "action_type": "SQL_TEMPLATE", "action": "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = ''idle'' AND now() - state_change > interval ''30 minutes''", "auto_execute": false, "requires_confirmation": true}
     ]', 15, FALSE),

    ('high_connection_usage_diagnostic', 'High Connection Usage Diagnostic',
     'Diagnostic investigation of high connection usage. This read-only runbook gathers connection data without terminating any connections. Use the full high_connection_usage runbook if connection termination is needed.',
     'INCIDENT', 'ALERT',
     '{"alert_type": "CONNECTION_THRESHOLD", "severity": ["CRITICAL", "HIGH"]}',
     '[
         {"order": 1, "title": "Check Connection Count", "description": "View current active connections and their states", "action_type": "NAVIGATE", "action": "/activity", "expected_outcome": "Review the number of active, idle, and idle-in-transaction connections", "auto_execute": true},
         {"order": 2, "title": "Identify Long-Running Queries", "description": "Look for queries running longer than expected that may be holding connections", "action_type": "NAVIGATE", "action": "/slow-queries?sort=duration", "expected_outcome": "Identify any unusually long-running queries that should be investigated", "auto_execute": true},
         {"order": 3, "title": "Check for Idle-in-Transaction Connections", "description": "Identify connections stuck in idle-in-transaction state for over 5 minutes", "action_type": "QUERY", "action": "SELECT pid, usename, application_name, client_addr, state, now() - xact_start AS transaction_duration, now() - query_start AS query_duration, LEFT(query, 100) AS query_preview FROM pg_stat_activity WHERE state = ''idle in transaction'' AND now() - xact_start > interval ''5 minutes'' ORDER BY xact_start", "expected_outcome": "List of connections that have been idle in transaction for too long - these may need attention", "auto_execute": true},
         {"order": 4, "title": "Check Connection Distribution by Application", "description": "See which applications are using the most connections", "action_type": "QUERY", "action": "SELECT application_name, state, COUNT(*) as connection_count, COUNT(*) FILTER (WHERE state = ''active'') as active, COUNT(*) FILTER (WHERE state = ''idle'') as idle, COUNT(*) FILTER (WHERE state = ''idle in transaction'') as idle_in_txn FROM pg_stat_activity WHERE backend_type = ''client backend'' GROUP BY application_name, state ORDER BY connection_count DESC", "expected_outcome": "Identify which applications are consuming the most connections and their states", "auto_execute": true}
     ]', 5, TRUE),

    ('blocked_queries', 'Blocked Queries Investigation', 'Diagnose and resolve query blocking issues', 'TROUBLESHOOTING', 'ALERT',
     '{"alert_type": "BLOCKED_QUERIES", "min_count": 1}',
     '[
         {"order": 1, "title": "View Blocking Tree", "description": "See the blocking hierarchy", "action_type": "NAVIGATE", "action": "/locks", "auto_execute": true},
         {"order": 2, "title": "Identify Root Blocker", "description": "Find the query at the top of the blocking chain", "action_type": "QUERY", "action": "SELECT * FROM pg_stat_activity WHERE pid IN (SELECT DISTINCT blocking_pid FROM pg_locks WHERE granted = false)", "auto_execute": true},
         {"order": 3, "title": "Assess Blocker Query", "description": "Determine if the blocking query can be safely terminated", "action_type": "MANUAL", "action": "Review the blocking query and determine appropriate action", "auto_execute": false},
         {"order": 4, "title": "Terminate if Necessary", "description": "Cancel or terminate the blocking query", "action_type": "SQL_TEMPLATE", "action": "SELECT pg_cancel_backend({pid})", "auto_execute": false, "requires_confirmation": true}
     ]', 10, FALSE),

    ('low_cache_hit', 'Low Cache Hit Ratio Investigation', 'Diagnose poor cache hit ratio', 'TROUBLESHOOTING', 'ALERT',
     '{"alert_type": "CACHE_HIT_RATIO", "threshold": 0.90}',
     '[
         {"order": 1, "title": "Check Current Cache Stats", "description": "View cache hit ratio details", "action_type": "NAVIGATE", "action": "/", "auto_execute": true},
         {"order": 2, "title": "Check shared_buffers Setting", "description": "Review current shared_buffers configuration", "action_type": "QUERY", "action": "SHOW shared_buffers", "auto_execute": true},
         {"order": 3, "title": "Identify Tables with Poor Cache Usage", "description": "Find tables with low cache hit rates", "action_type": "QUERY", "action": "SELECT schemaname, relname, heap_blks_hit, heap_blks_read, CASE WHEN heap_blks_hit + heap_blks_read > 0 THEN round(heap_blks_hit::numeric / (heap_blks_hit + heap_blks_read) * 100, 2) ELSE 0 END as hit_ratio FROM pg_statio_user_tables ORDER BY heap_blks_read DESC LIMIT 20", "auto_execute": true},
         {"order": 4, "title": "Consider Increasing shared_buffers", "description": "If memory is available, consider increasing shared_buffers", "action_type": "DOCUMENTATION", "action": "Recommended: 25% of system RAM for dedicated database servers", "auto_execute": false}
     ]', 20, TRUE),

    ('vacuum_maintenance', 'Regular Vacuum Maintenance', 'Perform routine vacuum maintenance', 'MAINTENANCE', 'SCHEDULED',
     '{"schedule": "0 3 * * 0"}',
     '[
         {"order": 1, "title": "Check Tables Needing Vacuum", "description": "Identify tables with high dead tuple counts", "action_type": "NAVIGATE", "action": "/table-maintenance", "auto_execute": true},
         {"order": 2, "title": "Review Autovacuum Settings", "description": "Check current autovacuum configuration", "action_type": "QUERY", "action": "SELECT name, setting FROM pg_settings WHERE name LIKE ''autovacuum%''", "auto_execute": true},
         {"order": 3, "title": "Run VACUUM ANALYZE on Priority Tables", "description": "Vacuum tables with highest dead tuple ratios first", "action_type": "SQL_TEMPLATE", "action": "VACUUM ANALYZE {table_name}", "auto_execute": false}
     ]', 30, TRUE)
ON CONFLICT (name) DO NOTHING;

-- Runbook execution history
CREATE TABLE IF NOT EXISTS pgconsole.runbook_execution (
    id BIGSERIAL PRIMARY KEY,
    runbook_id BIGINT NOT NULL REFERENCES pgconsole.runbook(id),
    instance_id TEXT NOT NULL,

    -- Database scope (NULL means instance-wide)
    database_name TEXT,

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
CREATE INDEX idx_runbook_execution_database
    ON pgconsole.runbook_execution(database_name) WHERE database_name IS NOT NULL;

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

-- ============================================================================
-- CUSTOM DASHBOARDS TABLES
-- ============================================================================

-- Custom Dashboard table
CREATE TABLE IF NOT EXISTS pgconsole.custom_dashboard (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    instance_id TEXT NOT NULL DEFAULT 'default',
    name TEXT NOT NULL,
    description TEXT,
    layout JSONB NOT NULL DEFAULT '{"columns": 2}',
    created_by TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    is_shared BOOLEAN DEFAULT FALSE,
    tags TEXT[],
    UNIQUE(instance_id, name)
);

-- Custom Widget table
CREATE TABLE IF NOT EXISTS pgconsole.custom_widget (
    id BIGSERIAL PRIMARY KEY,
    dashboard_id BIGINT NOT NULL REFERENCES pgconsole.custom_dashboard(id) ON DELETE CASCADE,
    widget_type TEXT NOT NULL,
    title TEXT,
    config JSONB DEFAULT '{}',
    position INTEGER NOT NULL DEFAULT 0,
    width INTEGER DEFAULT 6,
    height INTEGER DEFAULT 1
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_dashboard_instance
    ON pgconsole.custom_dashboard(instance_id);

CREATE INDEX IF NOT EXISTS idx_dashboard_shared
    ON pgconsole.custom_dashboard(is_shared)
    WHERE is_shared = TRUE;

CREATE INDEX IF NOT EXISTS idx_dashboard_tags
    ON pgconsole.custom_dashboard USING GIN(tags);

CREATE INDEX IF NOT EXISTS idx_widget_dashboard
    ON pgconsole.custom_widget(dashboard_id);

CREATE INDEX IF NOT EXISTS idx_widget_position
    ON pgconsole.custom_widget(dashboard_id, position);

-- Add comments
COMMENT ON TABLE pgconsole.custom_dashboard IS 'User-defined custom dashboards with personalised widgets';
COMMENT ON TABLE pgconsole.custom_widget IS 'Widgets belonging to custom dashboards';

COMMENT ON COLUMN pgconsole.custom_dashboard.layout IS 'Dashboard layout configuration (columns, etc.)';
COMMENT ON COLUMN pgconsole.custom_dashboard.is_default IS 'Whether this dashboard is shown by default for the user';
COMMENT ON COLUMN pgconsole.custom_dashboard.is_shared IS 'Whether this dashboard is visible to other users';

COMMENT ON COLUMN pgconsole.custom_widget.widget_type IS 'Widget type: connections, cache-ratio, active-queries, blocked-queries, db-size, top-tables, top-indexes, sparkline-connections, sparkline-queries, custom-sql';
COMMENT ON COLUMN pgconsole.custom_widget.config IS 'Widget-specific configuration (e.g., SQL query for custom-sql type)';
COMMENT ON COLUMN pgconsole.custom_widget.width IS 'Widget width in grid columns (1-12, Bootstrap grid)';
COMMENT ON COLUMN pgconsole.custom_widget.height IS 'Widget height in rows';

-- ============================================================================
-- COMPREHENSIVE TABLE AND COLUMN DOCUMENTATION
-- ============================================================================
-- Author: Paul Snow
-- Version: 0.0.0
-- Purpose: Provides detailed documentation for all pgconsole schema tables
-- ============================================================================

-- ============================================================================
-- HISTORY TABLES DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE pgconsole.system_metrics_history IS
'Stores system-level PostgreSQL metrics sampled at regular intervals for trend analysis and sparkline visualisation. '
'Captures connection counts, blocking information, cache performance, and database size. '
'Sampled by MetricsSamplerService and visualised in the overview dashboard. '
'Retention period controlled by PG_CONSOLE_HISTORY_RETENTION environment variable.';

COMMENT ON COLUMN pgconsole.system_metrics_history.instance_id IS
'Identifies the PostgreSQL instance being monitored. Default is ''default''. '
'Allows multi-instance monitoring in future releases.';

COMMENT ON COLUMN pgconsole.system_metrics_history.blocked_queries IS
'Count of queries waiting for locks at sample time. '
'Used to detect blocking trends and visualise contention patterns.';

COMMENT ON COLUMN pgconsole.system_metrics_history.cache_hit_ratio IS
'Overall cache hit ratio across all monitored databases, expressed as a decimal (0.0-1.0). '
'Calculated as blks_hit / (blks_hit + blks_read). '
'Values below 0.90 typically indicate insufficient shared_buffers or memory pressure.';

COMMENT ON COLUMN pgconsole.system_metrics_history.total_database_size_bytes IS
'Sum of all monitored database sizes in bytes. '
'Excludes template databases. Used for capacity planning and growth forecasting.';

COMMENT ON TABLE pgconsole.query_metrics_history IS
'Stores per-query metrics from pg_stat_statements sampled periodically. '
'Tracks the top N slowest queries (configurable via PG_CONSOLE_HISTORY_TOP_QUERIES). '
'Enables trend analysis of query performance degradation and regression detection. '
'Cumulative counters allow delta calculation between samples for rate-of-change analysis.';

COMMENT ON COLUMN pgconsole.query_metrics_history.query_id IS
'MD5 hash of normalised query text from pg_stat_statements. '
'Used to track the same query pattern across samples even as literal values change.';

COMMENT ON COLUMN pgconsole.query_metrics_history.total_calls IS
'Cumulative count of query executions from pg_stat_statements. '
'Delta between samples indicates execution frequency during the sampling interval.';

COMMENT ON COLUMN pgconsole.query_metrics_history.total_time_ms IS
'Cumulative execution time in milliseconds from pg_stat_statements. '
'Delta between samples shows total time spent in this query during the interval.';

COMMENT ON COLUMN pgconsole.query_metrics_history.mean_time_ms IS
'Average execution time per query call at sample time. '
'Sudden increases indicate performance regression or plan changes.';

COMMENT ON COLUMN pgconsole.query_metrics_history.shared_blks_hit IS
'Cumulative shared buffer cache hits. High values indicate good cache utilisation.';

COMMENT ON COLUMN pgconsole.query_metrics_history.shared_blks_read IS
'Cumulative shared buffer cache misses requiring disk I/O. '
'High values relative to hits indicate cache pressure or large table scans.';

COMMENT ON COLUMN pgconsole.query_metrics_history.temp_blks_written IS
'Cumulative temporary disk blocks written for work_mem overflow operations. '
'Non-zero values indicate sorts, hashes, or aggregations exceeding work_mem. '
'Persistent high values suggest work_mem tuning needed.';

COMMENT ON TABLE pgconsole.database_metrics_history IS
'Stores per-database metrics from pg_stat_database sampled periodically. '
'Enables per-database trend analysis, capacity planning, and anomaly detection. '
'Complements system-level metrics by providing database-specific insights. '
'Used for sparkline generation and database comparison dashboards.';

COMMENT ON COLUMN pgconsole.database_metrics_history.database_name IS
'Name of the monitored database. '
'Excludes template databases by default unless explicitly configured.';

COMMENT ON COLUMN pgconsole.database_metrics_history.xact_commit IS
'Cumulative count of committed transactions. '
'Delta between samples indicates transaction throughput.';

COMMENT ON COLUMN pgconsole.database_metrics_history.xact_rollback IS
'Cumulative count of rolled-back transactions. '
'High rollback rates may indicate application errors or constraint violations.';

COMMENT ON COLUMN pgconsole.database_metrics_history.cache_hit_ratio IS
'Database-specific cache hit ratio calculated as blks_hit / (blks_hit + blks_read). '
'Allows identification of databases with poor cache performance.';

COMMENT ON COLUMN pgconsole.database_metrics_history.deadlocks IS
'Cumulative count of deadlocks detected in this database. '
'Increases indicate transaction conflicts requiring application-level investigation.';

COMMENT ON COLUMN pgconsole.database_metrics_history.temp_files IS
'Cumulative count of temporary files created. '
'Indicates work_mem overflow frequency.';

COMMENT ON COLUMN pgconsole.database_metrics_history.temp_bytes IS
'Cumulative bytes written to temporary files. '
'High values indicate queries requiring more work_mem than configured.';

-- ============================================================================
-- AUDIT AND BOOKMARKS DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE pgconsole.audit_log IS
'Comprehensive audit trail of administrative actions performed via pg-console. '
'Tracks configuration changes, query terminations, maintenance operations, and user interactions. '
'Provides accountability, compliance support, and forensic investigation capabilities. '
'Never purged automatically; manual archival required for long-term retention.';

COMMENT ON COLUMN pgconsole.audit_log.instance_id IS
'PostgreSQL instance identifier where the action was performed. '
'Critical for multi-instance deployments to track changes across environments.';

COMMENT ON COLUMN pgconsole.audit_log.username IS
'Username of the person who performed the action. '
'Sourced from authentication context (future feature) or client-provided identifier.';

COMMENT ON COLUMN pgconsole.audit_log.action IS
'Type of action performed, e.g., TERMINATE_QUERY, UPDATE_SETTING, CREATE_DASHBOARD. '
'Standardised action names enable filtering and compliance reporting.';

COMMENT ON COLUMN pgconsole.audit_log.target_type IS
'Type of object affected by the action (e.g., QUERY, TABLE, SETTING, DASHBOARD). '
'Paired with target_id for precise object identification.';

COMMENT ON COLUMN pgconsole.audit_log.target_id IS
'Unique identifier of the affected object (e.g., PID, query_id, setting name). '
'Combined with target_type provides full context.';

COMMENT ON COLUMN pgconsole.audit_log.details IS
'JSONB field containing action-specific details such as before/after values, parameters, or context. '
'Flexible schema allows rich audit data without table modifications.';

COMMENT ON COLUMN pgconsole.audit_log.client_ip IS
'IP address of the client that initiated the action. '
'Useful for security investigations and access pattern analysis.';

COMMENT ON COLUMN pgconsole.audit_log.success IS
'Indicates whether the action completed successfully. '
'Failed actions include error_message for troubleshooting.';

COMMENT ON TABLE pgconsole.query_bookmark IS
'Tracks and annotates slow or problematic queries for ongoing monitoring and investigation. '
'Allows teams to tag queries with priority, status, and notes for collaborative troubleshooting. '
'Integrates with the slow queries dashboard for quick access to tracked queries. '
'Supports workflow management from detection through resolution.';

COMMENT ON COLUMN pgconsole.query_bookmark.query_id IS
'MD5 hash from pg_stat_statements identifying the normalised query pattern. '
'Combined with instance_id ensures unique bookmarks per instance.';

COMMENT ON COLUMN pgconsole.query_bookmark.query_text IS
'Denormalised query text for display purposes. '
'Stored to preserve readability even if query is purged from pg_stat_statements.';

COMMENT ON COLUMN pgconsole.query_bookmark.title IS
'User-provided short description summarising the query purpose or issue. '
'Displayed in bookmark lists for quick identification.';

COMMENT ON COLUMN pgconsole.query_bookmark.notes IS
'Detailed notes documenting investigation findings, optimisation attempts, or resolution steps. '
'Supports markdown for rich formatting (future enhancement).';

COMMENT ON COLUMN pgconsole.query_bookmark.tags IS
'Array of free-text tags for categorisation (e.g., ''performance'', ''regression'', ''monitoring''). '
'GIN index enables efficient tag-based searching.';

COMMENT ON COLUMN pgconsole.query_bookmark.priority IS
'Indicates urgency level: low, normal, high, critical. '
'Used to focus attention on the most impactful queries.';

COMMENT ON COLUMN pgconsole.query_bookmark.status IS
'Workflow state: active, investigating, resolved, false-positive. '
'Tracks bookmark lifecycle from detection to closure.';

COMMENT ON TABLE pgconsole.scheduled_report IS
'Configuration for automated periodic reports delivered via email or other channels. '
'Supports daily, weekly, or custom schedules using cron expressions. '
'Report types include slow query summaries, capacity reports, and health checks. '
'Future enhancement for proactive monitoring without manual dashboard checks.';

COMMENT ON COLUMN pgconsole.scheduled_report.report_type IS
'Type of report to generate: SLOW_QUERIES, CAPACITY, HEALTH_SUMMARY, CUSTOM. '
'Determines report template and data sources.';

COMMENT ON COLUMN pgconsole.scheduled_report.schedule IS
'Cron expression defining report frequency (e.g., ''0 9 * * MON'' for Monday 9am). '
'Standard cron syntax supported.';

COMMENT ON COLUMN pgconsole.scheduled_report.recipients IS
'Array of email addresses to receive the report. '
'Supports distribution lists for team-wide visibility.';

COMMENT ON COLUMN pgconsole.scheduled_report.config IS
'Report-specific configuration in JSONB format. '
'May include filters (e.g., min_duration for slow queries), thresholds, or formatting options.';

COMMENT ON COLUMN pgconsole.scheduled_report.next_run_at IS
'Calculated timestamp for the next scheduled report generation. '
'Updated automatically after each successful run based on cron schedule.';

-- ============================================================================
-- NOTIFICATION AND ALERTING DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE pgconsole.notification_channel IS
'Defines external notification destinations for alerts (Slack, Teams, PagerDuty, Discord, Email). '
'Each channel has its own configuration (webhook URLs, API keys, etc.) stored securely in JSONB. '
'Supports severity and alert-type filtering to route only relevant alerts to each channel. '
'Enables flexible multi-channel alerting strategies with per-channel rate limiting.';

COMMENT ON COLUMN pgconsole.notification_channel.channel_type IS
'Notification platform: SLACK, TEAMS, PAGERDUTY, DISCORD, EMAIL. '
'Determines the API integration and message format used.';

COMMENT ON COLUMN pgconsole.notification_channel.config IS
'JSONB containing channel-specific configuration such as webhook_url, api_key, routing_key. '
'Schema varies by channel_type. Sensitive values should be encrypted at rest (future enhancement).';

COMMENT ON COLUMN pgconsole.notification_channel.severity_filter IS
'Array of severity levels this channel accepts (CRITICAL, HIGH, MEDIUM, LOW). '
'NULL means all severities. Enables routing critical alerts to PagerDuty and low-severity to Slack.';

COMMENT ON COLUMN pgconsole.notification_channel.alert_type_filter IS
'Array of alert types this channel accepts (e.g., BLOCKED_QUERIES, HIGH_CONNECTIONS). '
'NULL means all types. Allows specialised channels for specific alert categories.';

COMMENT ON COLUMN pgconsole.notification_channel.instance_filter IS
'Array of instance IDs this channel monitors. '
'NULL means all instances. Enables per-environment notification routing.';

COMMENT ON COLUMN pgconsole.notification_channel.rate_limit_per_hour IS
'Maximum number of notifications allowed per hour to prevent alert storms. '
'NULL means no limit. Protects against channel fatigue during incidents.';

COMMENT ON COLUMN pgconsole.notification_channel.test_mode IS
'When true, notifications are logged but not sent externally. '
'Used for testing channel configuration without spamming recipients.';

COMMENT ON TABLE pgconsole.escalation_policy IS
'Defines multi-tier escalation paths for unacknowledged alerts. '
'If an alert remains unacknowledged, it escalates through tiers with increasing urgency. '
'Each tier can notify different channels with configurable delays. '
'Ensures critical alerts reach on-call personnel if initial notifications are missed.';

COMMENT ON COLUMN pgconsole.escalation_policy.repeat_count IS
'Number of times to repeat all escalation tiers after completing the final tier. '
'0 means notify once through all tiers then stop. Prevents indefinite escalation loops.';

COMMENT ON TABLE pgconsole.escalation_tier IS
'Individual escalation tier within an escalation policy. '
'Each tier specifies notification channels and delay before escalating to next tier. '
'Tier 1 typically notifies Slack/Teams, Tier 2 sends email, Tier 3 pages on-call via PagerDuty. '
'Linked to escalation_policy via foreign key with CASCADE delete.';

COMMENT ON COLUMN pgconsole.escalation_tier.tier_order IS
'Numeric position in the escalation sequence (1, 2, 3...). '
'Lower numbers execute first. Must be unique within a policy.';

COMMENT ON COLUMN pgconsole.escalation_tier.delay_minutes IS
'Minutes to wait before escalating to the next tier if unacknowledged. '
'Tier 1 typically has 0 delay (immediate). Tier 2 might wait 15 minutes.';

COMMENT ON COLUMN pgconsole.escalation_tier.channel_ids IS
'Array of notification_channel IDs to notify at this tier. '
'Allows multi-channel notifications per tier (e.g., Slack + Email simultaneously).';

COMMENT ON TABLE pgconsole.maintenance_window IS
'Defines scheduled maintenance periods during which alerts are suppressed. '
'Prevents alert noise during planned downtime, deployments, or maintenance activities. '
'Supports one-time and recurring windows with optional instance/alert-type filtering. '
'Active windows are checked before firing alerts.';

COMMENT ON COLUMN pgconsole.maintenance_window.recurring IS
'Indicates if this window repeats on a schedule. '
'One-time windows (false) are used for specific events. Recurring windows (true) use recurrence_pattern.';

COMMENT ON COLUMN pgconsole.maintenance_window.recurrence_pattern IS
'Defines repeat schedule: DAILY, WEEKLY, MONTHLY, or custom cron expression. '
'Only used when recurring is true. E.g., ''WEEKLY'' for same time each week.';

COMMENT ON COLUMN pgconsole.maintenance_window.alert_type_filter IS
'Array of alert types to suppress (e.g., [''BLOCKED_QUERIES'', ''HIGH_CONNECTIONS'']). '
'NULL suppresses all alert types. Allows partial suppression during maintenance.';

COMMENT ON TABLE pgconsole.alert_acknowledgement IS
'Tracks manual acknowledgements of active alerts to prevent duplicate escalations. '
'When an alert is acknowledged, escalation pauses until expiry or resolution. '
'Supports temporary acknowledgements with expiration for alerts requiring ongoing attention. '
'One acknowledgement per alert_id enforced by unique constraint.';

COMMENT ON COLUMN pgconsole.alert_acknowledgement.alert_id IS
'Unique identifier for the acknowledged alert, matching active_alert.alert_id. '
'Typically includes instance, alert type, and trigger details for uniqueness.';

COMMENT ON COLUMN pgconsole.alert_acknowledgement.acknowledged_by IS
'Username or identifier of the person who acknowledged the alert. '
'Provides accountability and context for handoff between team members.';

COMMENT ON COLUMN pgconsole.alert_acknowledgement.note IS
'Optional note explaining the acknowledgement reason or planned action. '
'Useful for communicating status to other team members.';

COMMENT ON COLUMN pgconsole.alert_acknowledgement.expires_at IS
'Timestamp when acknowledgement expires and escalation resumes. '
'NULL means permanent acknowledgement until alert resolves. Useful for known ongoing issues.';

COMMENT ON TABLE pgconsole.alert_silence IS
'Pattern-based alert suppression for known issues or expected conditions. '
'Unlike maintenance windows (time-based), silences are condition-based using matchers. '
'Useful for suppressing alerts during deployments, known slow queries, or test environments. '
'Silences have start/end times and require manual creation.';

COMMENT ON COLUMN pgconsole.alert_silence.matchers IS
'JSONB array of matcher objects: [{"field": "instance", "operator": "equals", "value": "test"}]. '
'Alerts matching all matchers are suppressed. Supports flexible pattern matching.';

COMMENT ON COLUMN pgconsole.alert_silence.created_by IS
'Username who created the silence for audit trail. '
'Required field ensures accountability for suppression decisions.';

COMMENT ON TABLE pgconsole.notification_history IS
'Complete audit log of all notification attempts including successes and failures. '
'Tracks delivery to each channel with response codes and error messages. '
'Enables troubleshooting of notification delivery issues and provides compliance evidence. '
'Denormalises channel name/type to preserve history even after channel deletion.';

COMMENT ON COLUMN pgconsole.notification_history.channel_id IS
'Foreign key to notification_channel (nullable via ON DELETE SET NULL). '
'Preserves history even if channel is deleted.';

COMMENT ON COLUMN pgconsole.notification_history.channel_name IS
'Denormalised channel name for historical display. '
'Ensures notification history remains readable after channel modifications.';

COMMENT ON COLUMN pgconsole.notification_history.dedup_key IS
'PagerDuty deduplication key for grouping related alerts. '
'Ensures multiple notifications for the same alert update a single PagerDuty incident.';

COMMENT ON COLUMN pgconsole.notification_history.escalation_tier IS
'Which escalation tier triggered this notification (1, 2, 3...). '
'NULL if not part of an escalation policy. Tracks escalation progression.';

COMMENT ON COLUMN pgconsole.notification_history.response_code IS
'HTTP response code from the notification API (e.g., 200, 404, 500). '
'NULL for non-HTTP channels. Used to diagnose delivery failures.';

COMMENT ON COLUMN pgconsole.notification_history.response_body IS
'API response body for debugging failed notifications. '
'Truncated to reasonable length. Contains error details from external services.';

COMMENT ON TABLE pgconsole.active_alert IS
'Tracks currently active alerts for escalation management and deduplication. '
'Each unique alert condition creates one active_alert row until resolved. '
'Enables tracking of escalation tier progression and acknowledgement status. '
'Resolved alerts remain in table for audit but are excluded from active processing.';

COMMENT ON COLUMN pgconsole.active_alert.alert_id IS
'Unique identifier for this alert instance. '
'Typically a composite of instance, alert type, and specific condition (e.g., ''default:blocked_queries:pid_12345''). '
'Used for deduplication and acknowledgement linking.';

COMMENT ON COLUMN pgconsole.active_alert.current_escalation_tier IS
'Current position in escalation policy (1, 2, 3...). '
'Increments as unacknowledged alerts escalate through tiers. Reset on acknowledgement.';

COMMENT ON COLUMN pgconsole.active_alert.escalation_policy_id IS
'Foreign key to escalation_policy defining escalation path. '
'NULL if no escalation policy assigned (alert fires once only).';

COMMENT ON COLUMN pgconsole.active_alert.acknowledged IS
'Indicates whether this alert has been manually acknowledged. '
'Acknowledged alerts do not escalate but remain active until resolved.';

COMMENT ON COLUMN pgconsole.active_alert.resolved IS
'Indicates whether the underlying condition has cleared. '
'Resolved alerts are retained for history but excluded from escalation processing.';

COMMENT ON COLUMN pgconsole.active_alert.metadata IS
'JSONB containing alert-specific context such as affected queries, tables, or metrics. '
'Used for rich notification content and forensic investigation.';

COMMENT ON TABLE pgconsole.email_digest_queue IS
'Queues alerts for periodic email digests rather than immediate per-alert emails. '
'Aggregates multiple alerts into a single email sent on schedule (e.g., hourly). '
'Reduces email fatigue for low-severity alerts whilst maintaining visibility. '
'Processed alerts are marked processed and can be archived after digest delivery.';

COMMENT ON COLUMN pgconsole.email_digest_queue.recipient IS
'Email address to receive the digest. '
'Same recipient receives all queued alerts in a single email.';

COMMENT ON COLUMN pgconsole.email_digest_queue.processed IS
'Indicates whether this alert has been included in a sent digest. '
'Unprocessed alerts are included in the next scheduled digest run.';

-- ============================================================================
-- INTELLIGENT INSIGHTS DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE pgconsole.metric_baseline IS
'Statistical baselines for metrics used in anomaly detection and alerting. '
'Captures mean, standard deviation, percentiles, and time-based patterns (hourly/daily). '
'Calculated periodically from historical data to establish "normal" behaviour. '
'Anomalies are detected when current values deviate significantly from baseline (e.g., >3 sigma). '
'Supports context-aware baselines (weekday vs weekend, business hours vs night).';

COMMENT ON COLUMN pgconsole.metric_baseline.metric_category IS
'Category of the metric: system, query, or database. '
'Determines which history table is queried for baseline calculation.';

COMMENT ON COLUMN pgconsole.metric_baseline.baseline_mean IS
'Arithmetic mean of the metric over the baseline period. '
'Used as the central tendency for deviation calculations.';

COMMENT ON COLUMN pgconsole.metric_baseline.baseline_stddev IS
'Standard deviation of the metric over the baseline period. '
'Used to calculate sigma-based deviation thresholds (e.g., mean ± 3*stddev).';

COMMENT ON COLUMN pgconsole.metric_baseline.baseline_p95 IS
'95th percentile value. '
'Useful for understanding typical peak values and setting conservative thresholds.';

COMMENT ON COLUMN pgconsole.metric_baseline.baseline_p99 IS
'99th percentile value. '
'Represents extreme but not anomalous peaks. Values above p99 warrant investigation.';

COMMENT ON COLUMN pgconsole.metric_baseline.hour_of_day IS
'Hour of day (0-23) for time-specific baselines, or NULL for overall baseline. '
'Enables detection of anomalies relative to time-of-day patterns (e.g., high load at 3am is anomalous).';

COMMENT ON COLUMN pgconsole.metric_baseline.day_of_week IS
'Day of week (0-6, Sunday=0) for day-specific baselines, or NULL for overall baseline. '
'Distinguishes weekday vs weekend patterns for business applications.';

COMMENT ON COLUMN pgconsole.metric_baseline.period_start IS
'Start of the time range used for baseline calculation. '
'Typically 7-30 days of historical data. Too short risks noise, too long misses shifts.';

COMMENT ON COLUMN pgconsole.metric_baseline.period_end IS
'End of the time range used for baseline calculation. '
'Usually the most recent complete day to avoid partial data.';

COMMENT ON TABLE pgconsole.detected_anomaly IS
'Records detected metric anomalies with severity classification and root cause suggestions. '
'Created automatically when metrics deviate significantly from established baselines. '
'Tracks acknowledgement and resolution workflow similar to alerts. '
'Supports statistical anomaly types: spikes, drops, sustained trends, and pattern breaks. '
'Correlated metrics help identify cascading effects or common root causes.';

COMMENT ON COLUMN pgconsole.detected_anomaly.deviation_sigma IS
'Number of standard deviations from baseline mean. '
'Values >3 sigma indicate strong anomalies. >5 sigma suggests critical issues or data errors.';

COMMENT ON COLUMN pgconsole.detected_anomaly.severity IS
'Calculated severity: CRITICAL (>5 sigma), HIGH (3-5 sigma), MEDIUM (2-3 sigma), LOW (<2 sigma). '
'Can be manually adjusted based on business impact.';

COMMENT ON COLUMN pgconsole.detected_anomaly.anomaly_type IS
'Classification of anomaly behaviour: SPIKE (sudden increase), DROP (sudden decrease), '
'TREND (sustained directional change), PATTERN_BREAK (expected pattern violated).';

COMMENT ON COLUMN pgconsole.detected_anomaly.direction IS
'Whether anomaly is ABOVE or BELOW baseline. '
'Direction matters for interpretation: high connections vs low cache hits.';

COMMENT ON COLUMN pgconsole.detected_anomaly.root_cause_suggestion IS
'AI-generated or rule-based suggestion for likely root cause. '
'E.g., "Query regression detected on query_id abc123" or "Possible connection leak".';

COMMENT ON COLUMN pgconsole.detected_anomaly.correlated_metrics IS
'JSONB array of other metrics that showed anomalies at the same time. '
'Helps identify cascading effects: e.g., spike in active queries + drop in cache hit ratio.';

COMMENT ON COLUMN pgconsole.detected_anomaly.resolution_notes IS
'Free-text notes documenting investigation and resolution. '
'Builds institutional knowledge for similar future anomalies.';

COMMENT ON TABLE pgconsole.metric_forecast IS
'Time-series forecasts for capacity planning and proactive alerting. '
'Predicts future metric values using linear, exponential, or seasonal models. '
'Includes confidence intervals to quantify prediction uncertainty. '
'Used to forecast database size, connection growth, and resource exhaustion dates. '
'Recalculated periodically as new data becomes available.';

COMMENT ON COLUMN pgconsole.metric_forecast.forecast_date IS
'Date for which the forecast applies. '
'Typically extends 7-90 days into the future depending on metric volatility.';

COMMENT ON COLUMN pgconsole.metric_forecast.forecast_value IS
'Predicted metric value on forecast_date. '
'Point estimate from the forecasting model.';

COMMENT ON COLUMN pgconsole.metric_forecast.confidence_lower IS
'Lower bound of the confidence interval. '
'E.g., at 95% confidence, the true value is likely above this bound.';

COMMENT ON COLUMN pgconsole.metric_forecast.confidence_upper IS
'Upper bound of the confidence interval. '
'Wider intervals indicate greater uncertainty in the forecast.';

COMMENT ON COLUMN pgconsole.metric_forecast.model_type IS
'Forecasting algorithm used: LINEAR (simple trend), EXPONENTIAL (accelerating growth), '
'SEASONAL (recurring patterns). Model selection based on historical data characteristics.';

COMMENT ON COLUMN pgconsole.metric_forecast.r_squared IS
'Coefficient of determination (0-1) indicating model fit quality. '
'Values >0.8 indicate strong predictive power. <0.5 suggests model may not be reliable.';

COMMENT ON COLUMN pgconsole.metric_forecast.training_period_days IS
'Number of days of historical data used to train the forecasting model. '
'Longer periods improve stability but may miss recent trend changes.';

COMMENT ON TABLE pgconsole.capacity_threshold IS
'Defines warning and critical thresholds for capacity metrics with proactive notification. '
'When forecasts predict threshold breaches within notify_days_before, alerts are fired. '
'Enables "you will run out of disk space in 5 days" type alerts. '
'Thresholds can be absolute (e.g., 100GB) or percentage (e.g., 80% of max_connections).';

COMMENT ON COLUMN pgconsole.capacity_threshold.warning_threshold IS
'Value at which a warning alert is triggered. '
'Should allow sufficient time for remediation before critical threshold.';

COMMENT ON COLUMN pgconsole.capacity_threshold.critical_threshold IS
'Value at which a critical alert is triggered. '
'Should be set below hard limits to allow emergency response time.';

COMMENT ON COLUMN pgconsole.capacity_threshold.threshold_type IS
'ABSOLUTE (e.g., 500GB database size) or PERCENTAGE (e.g., 80% of max_connections). '
'Percentage thresholds automatically adjust as configured limits change.';

COMMENT ON COLUMN pgconsole.capacity_threshold.notify_days_before IS
'Number of days before forecasted threshold breach to trigger alerts. '
'E.g., 7 days warning allows time for capacity planning and procurement.';

COMMENT ON TABLE pgconsole.unified_recommendation IS
'Aggregates recommendations from all analysis sources into a prioritised action list. '
'Sources include: missing indexes, table bloat, query regressions, config tuning, and anomalies. '
'Each recommendation includes rationale, estimated impact/effort, and suggested SQL. '
'Tracks full lifecycle from detection through application and effectiveness measurement. '
'Priority score (1-100) combines severity, impact, and effort for optimal ordering.';

COMMENT ON COLUMN pgconsole.unified_recommendation.source IS
'Origin of the recommendation: INDEX_ADVISOR, TABLE_MAINTENANCE, QUERY_REGRESSION, '
'CONFIG_TUNING, or ANOMALY. Helps filter and attribute recommendations.';

COMMENT ON COLUMN pgconsole.unified_recommendation.recommendation_type IS
'Specific recommendation category within source (e.g., CREATE_INDEX, VACUUM_FULL, TUNE_WORK_MEM). '
'More granular than source for precise filtering.';

COMMENT ON COLUMN pgconsole.unified_recommendation.rationale IS
'Detailed explanation of why this recommendation was generated. '
'Includes supporting data: e.g., "Sequential scan cost: 12543.21, index scan cost: 23.45".';

COMMENT ON COLUMN pgconsole.unified_recommendation.priority_score IS
'Calculated score (1-100) combining severity, estimated impact, and estimated effort. '
'Higher scores indicate more impactful, less risky, easier-to-implement recommendations.';

COMMENT ON COLUMN pgconsole.unified_recommendation.estimated_impact IS
'Expected performance improvement: HIGH (>50%), MEDIUM (10-50%), LOW (<10%). '
'Based on cost estimates, query frequency, or historical data.';

COMMENT ON COLUMN pgconsole.unified_recommendation.estimated_effort IS
'Implementation complexity: MINIMAL (automatic), LOW (simple SQL), MEDIUM (testing needed), '
'HIGH (requires maintenance window or schema changes).';

COMMENT ON COLUMN pgconsole.unified_recommendation.suggested_sql IS
'Executable SQL statement to apply the recommendation. '
'E.g., "CREATE INDEX idx_users_email ON users(email)". Can be copy-pasted for quick action.';

COMMENT ON COLUMN pgconsole.unified_recommendation.rollback_sql IS
'SQL to reverse the recommendation if results are unsatisfactory. '
'E.g., "DROP INDEX idx_users_email". NULL if rollback is not straightforward.';

COMMENT ON COLUMN pgconsole.unified_recommendation.affected_objects IS
'JSONB array of tables, indexes, or queries affected by this recommendation. '
'Enables filtering (e.g., show all recommendations for table ''orders'').';

COMMENT ON COLUMN pgconsole.unified_recommendation.status IS
'Workflow state: OPEN (new), IN_PROGRESS (being evaluated), APPLIED (implemented), '
'DISMISSED (rejected), DEFERRED (postponed to deferred_until date).';

COMMENT ON COLUMN pgconsole.unified_recommendation.effectiveness_rating IS
'Post-implementation assessment: EXCELLENT (met/exceeded expectations), GOOD (modest improvement), '
'NEUTRAL (no measurable change), POOR (degraded performance). Used to refine future recommendations.';

COMMENT ON COLUMN pgconsole.unified_recommendation.pre_metric_value IS
'Metric value before applying recommendation (e.g., query execution time). '
'Paired with post_metric_value to measure actual effectiveness.';

COMMENT ON TABLE pgconsole.config_tuning_suggestion IS
'Automated suggestions for PostgreSQL configuration parameter tuning. '
'Analyses current metrics, workload patterns, and system resources to recommend adjustments. '
'Categories include memory allocation, connection limits, WAL settings, vacuum, and query planner. '
'Includes current vs suggested values, rationale, expected benefits, and potential risks. '
'Flags parameters requiring restart for operational planning.';

COMMENT ON COLUMN pgconsole.config_tuning_suggestion.parameter_name IS
'PostgreSQL configuration parameter to tune (e.g., shared_buffers, work_mem, max_connections).';

COMMENT ON COLUMN pgconsole.config_tuning_suggestion.current_value IS
'Current value from pg_settings. '
'Stored as text to handle all parameter types (integers, memory units, booleans, enums).';

COMMENT ON COLUMN pgconsole.config_tuning_suggestion.suggested_value IS
'Recommended value based on workload analysis and system resources. '
'Includes unit suffixes for memory parameters (e.g., ''4GB'', ''256MB'').';

COMMENT ON COLUMN pgconsole.config_tuning_suggestion.default_value IS
'PostgreSQL default value for this parameter. '
'Useful to distinguish custom settings from defaults.';

COMMENT ON COLUMN pgconsole.config_tuning_suggestion.category IS
'Configuration domain: MEMORY, CONNECTIONS, WAL, VACUUM, QUERY_PLANNER. '
'Helps group related tuning suggestions.';

COMMENT ON COLUMN pgconsole.config_tuning_suggestion.expected_benefit IS
'Description of anticipated performance improvement or operational benefit. '
'E.g., "Reduce temporary file usage by 80%" or "Improve query planning for large joins".';

COMMENT ON COLUMN pgconsole.config_tuning_suggestion.potential_risks IS
'Warnings about possible negative effects or prerequisites. '
'E.g., "Increasing shared_buffers requires more RAM; ensure sufficient physical memory".';

COMMENT ON COLUMN pgconsole.config_tuning_suggestion.triggering_metric IS
'Metric that caused this suggestion to be generated (e.g., temp_bytes, cache_hit_ratio). '
'Links suggestion back to observed behaviour.';

COMMENT ON COLUMN pgconsole.config_tuning_suggestion.requires_restart IS
'Indicates if applying this parameter change requires PostgreSQL restart. '
'True for parameters like shared_buffers, max_connections. False for SIGHUP-reloadable parameters.';

-- ============================================================================
-- NATURAL LANGUAGE QUERIES DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE pgconsole.nl_query_pattern IS
'Defines regular expression patterns for natural language query parsing. '
'Maps user queries like "show me slow queries from yesterday" to dashboard endpoints. '
'Patterns are matched by priority (higher first) to handle specificity (e.g., "yesterday" before generic). '
'Extracted parameters from regex groups are passed to endpoints as query strings. '
'Extensible system allows adding new patterns without code changes.';

COMMENT ON COLUMN pgconsole.nl_query_pattern.pattern IS
'Regular expression pattern to match against user input. '
'Uses Java regex syntax with named groups for parameter extraction. '
'Case-insensitive flag (?i) recommended for user-friendly matching.';

COMMENT ON COLUMN pgconsole.nl_query_pattern.intent IS
'Semantic intent identifier for this pattern (e.g., SLOW_QUERIES_YESTERDAY, TABLE_GROWTH). '
'Used for analytics and pattern effectiveness tracking.';

COMMENT ON COLUMN pgconsole.nl_query_pattern.target_endpoint IS
'Dashboard path or API endpoint to redirect to (e.g., /slow-queries, /storage-insights). '
'Query parameters from ''parameters'' field appended to this base path.';

COMMENT ON COLUMN pgconsole.nl_query_pattern.parameters IS
'JSONB mapping of extracted regex groups to query parameters. '
'E.g., {"timeRange": "yesterday", "hoursParam": 2} becomes ?timeRange=yesterday&hours=2';

COMMENT ON COLUMN pgconsole.nl_query_pattern.priority IS
'Match priority (higher values matched first). '
'Allows specific patterns (e.g., "last 3 hours") to override generic ones (e.g., "slow queries").';

COMMENT ON COLUMN pgconsole.nl_query_pattern.example_queries IS
'Array of example user inputs that match this pattern. '
'Used for testing, documentation, and autocomplete suggestions.';

COMMENT ON TABLE pgconsole.nl_query_history IS
'Audit log of natural language queries for learning and improvement. '
'Tracks successful matches, failed matches, and user feedback. '
'Feedback (HELPFUL/NOT_HELPFUL/WRONG) trains future pattern improvements. '
'Unmatched queries with negative feedback identify gaps in pattern coverage.';

COMMENT ON COLUMN pgconsole.nl_query_history.query_text IS
'Exact user input for the natural language query. '
'Preserved for pattern refinement and analytics.';

COMMENT ON COLUMN pgconsole.nl_query_history.matched_intent IS
'Intent identifier from the matched nl_query_pattern. '
'NULL if no pattern matched the query.';

COMMENT ON COLUMN pgconsole.nl_query_history.resolved_to IS
'Full URL the query was resolved to (endpoint + parameters). '
'E.g., "/slow-queries?timeRange=yesterday". NULL if match failed.';

COMMENT ON COLUMN pgconsole.nl_query_history.user_feedback IS
'User-provided feedback: HELPFUL (query answered their question), NOT_HELPFUL (wrong endpoint), '
'WRONG (pattern matched incorrectly). NULL if no feedback provided.';

-- ============================================================================
-- RUNBOOK INTEGRATION DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE pgconsole.runbook IS
'Defines structured troubleshooting and maintenance procedures as executable workflows. '
'Runbooks contain ordered steps combining navigation, SQL queries, and manual actions. '
'Can be triggered manually, by alerts, anomalies, or on schedule. '
'Steps support auto-execution for safe operations and require confirmation for destructive actions. '
'Provides guided incident response, knowledge capture, and automation for common operational tasks.';

COMMENT ON COLUMN pgconsole.runbook.trigger_type IS
'How this runbook is initiated: MANUAL (user-triggered), ALERT (auto-triggered by alerts), '
'ANOMALY (auto-triggered by detected anomalies), SCHEDULED (periodic execution).';

COMMENT ON COLUMN pgconsole.runbook.trigger_conditions IS
'JSONB criteria for automatic triggering. '
'E.g., {"alert_type": "CONNECTION_THRESHOLD", "severity": ["CRITICAL", "HIGH"]}. '
'NULL for manual-only runbooks.';

COMMENT ON COLUMN pgconsole.runbook.steps IS
'JSONB array of runbook steps: [{"order": 1, "title": "Check connections", "action_type": "NAVIGATE", '
'"action": "/activity", "auto_execute": true, "requires_confirmation": false}]. '
'Action types: NAVIGATE (dashboard), QUERY (SQL), MANUAL (human step), SQL_TEMPLATE (parameterised SQL), '
'DOCUMENTATION (external link).';

COMMENT ON COLUMN pgconsole.runbook.version IS
'Runbook version number, incremented on updates. '
'Allows tracking changes and linking executions to specific runbook versions.';

COMMENT ON COLUMN pgconsole.runbook.estimated_duration_minutes IS
'Expected time to complete this runbook. '
'Helps operators plan and prioritise during incidents.';

COMMENT ON COLUMN pgconsole.runbook.auto_executable IS
'Whether this runbook can be auto-executed without user intervention. '
'Only enable for runbooks with non-destructive operations (e.g., VACUUM, ANALYSE, read-only queries). '
'Runbooks with destructive steps like pg_terminate_backend should have this set to FALSE.';

COMMENT ON TABLE pgconsole.runbook_execution IS
'Tracks individual runbook executions from start to completion. '
'Records which steps were executed, their results, and overall status. '
'Provides audit trail for incident response and operational changes. '
'Links back to triggering context (alert ID, anomaly ID, schedule).';

COMMENT ON COLUMN pgconsole.runbook_execution.triggered_by IS
'Source of execution: MANUAL, ALERT, ANOMALY, or SCHEDULED. '
'Matches trigger types from runbook definition.';

COMMENT ON COLUMN pgconsole.runbook_execution.trigger_context IS
'JSONB containing context about what triggered this execution. '
'E.g., {"alert_id": "default:blocked_queries:12345", "severity": "CRITICAL"}.';

COMMENT ON COLUMN pgconsole.runbook_execution.current_step IS
'Currently executing step number (matches step.order from runbook.steps). '
'Tracks progress through the runbook for resumption or monitoring.';

COMMENT ON COLUMN pgconsole.runbook_execution.step_results IS
'JSONB array of results from each completed step. '
'[{"step": 1, "completed_at": "2025-01-01T10:00:00Z", "result": "23 active connections found"}]. '
'Provides execution history and context for subsequent steps.';

COMMENT ON COLUMN pgconsole.runbook_execution.database_name IS
'The specific database this runbook execution applies to. '
'NULL means all databases (instance-wide). '
'Useful for scoping diagnostics to a particular database (e.g., connection analysis, query performance).';

-- ============================================================================
-- SCHEDULED MAINTENANCE AUTOMATION DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE pgconsole.scheduled_maintenance IS
'Configuration for automated maintenance tasks (VACUUM, ANALYSE, REINDEX, CLUSTER). '
'Supports three scheduling modes: INTELLIGENT (run during low-activity periods), '
'CRON (fixed schedule), and ONE_TIME (specific timestamp). '
'Intelligent scheduling uses activity_pattern data to find optimal execution windows. '
'Includes safety limits (max_duration_minutes) to prevent runaway operations.';

COMMENT ON COLUMN pgconsole.scheduled_maintenance.task_type IS
'Type of maintenance: VACUUM (standard), VACUUM_FULL (requires lock), ANALYSE (statistics), '
'REINDEX (rebuild index), CLUSTER (reorder table). Each has different resource and locking implications.';

COMMENT ON COLUMN pgconsole.scheduled_maintenance.target_object IS
'Name of table or index to maintain, or ''*'' for all objects in target_schema. '
'Wildcard allows schema-wide maintenance with a single schedule.';

COMMENT ON COLUMN pgconsole.scheduled_maintenance.schedule_type IS
'Scheduling strategy: INTELLIGENT (opportunistic during low activity), '
'CRON (fixed schedule), ONE_TIME (run once at scheduled_time).';

COMMENT ON COLUMN pgconsole.scheduled_maintenance.cron_expression IS
'Standard cron expression for CRON schedule type (e.g., ''0 2 * * 0'' for Sundays at 2am). '
'NULL for other schedule types.';

COMMENT ON COLUMN pgconsole.scheduled_maintenance.activity_threshold IS
'For INTELLIGENT scheduling: activity score threshold below which task can run. '
'E.g., 30 means run only when activity is in lowest 30% of observed patterns.';

COMMENT ON COLUMN pgconsole.scheduled_maintenance.preferred_window_start IS
'For INTELLIGENT scheduling: earliest time of day to consider running (e.g., ''22:00''). '
'Constrains opportunistic scheduling to acceptable hours.';

COMMENT ON COLUMN pgconsole.scheduled_maintenance.preferred_window_end IS
'For INTELLIGENT scheduling: latest time of day to consider running (e.g., ''06:00''). '
'Prevents maintenance from running into business hours.';

COMMENT ON COLUMN pgconsole.scheduled_maintenance.max_duration_minutes IS
'Maximum allowed execution time before task is cancelled. '
'Prevents maintenance from interfering with business hours or peak periods.';

COMMENT ON COLUMN pgconsole.scheduled_maintenance.priority IS
'Priority level 1-10 for execution ordering when multiple tasks are eligible. '
'Higher values run first. Useful for prioritising critical tables.';

COMMENT ON TABLE pgconsole.maintenance_execution IS
'Audit log of maintenance task executions with before/after metrics. '
'Records success, failure, and resource impact of each operation. '
'Pre/post metrics (dead tuples, table size) measure maintenance effectiveness. '
'Links to scheduled_maintenance for recurring tasks, or NULL for ad-hoc operations.';

COMMENT ON COLUMN pgconsole.maintenance_execution.scheduled_maintenance_id IS
'Foreign key to scheduled_maintenance for scheduled tasks. '
'NULL for manual one-off maintenance operations.';

COMMENT ON COLUMN pgconsole.maintenance_execution.rows_affected IS
'Number of rows processed by the maintenance operation. '
'Interpretation varies by task_type: for VACUUM, rows where dead tuples were removed.';

COMMENT ON COLUMN pgconsole.maintenance_execution.pages_removed IS
'Number of disk pages freed by VACUUM operations. '
'Indicates effectiveness of vacuum in reclaiming space. 0 suggests table bloat remains.';

COMMENT ON COLUMN pgconsole.maintenance_execution.duration_seconds IS
'Actual execution time in seconds. '
'Used to refine max_duration_minutes estimates and schedule future maintenance.';

COMMENT ON COLUMN pgconsole.maintenance_execution.pre_dead_tuples IS
'Count of dead tuples before maintenance (from pg_stat_user_tables). '
'Baseline for measuring vacuum effectiveness.';

COMMENT ON COLUMN pgconsole.maintenance_execution.post_dead_tuples IS
'Count of dead tuples after maintenance. '
'Should be significantly lower than pre_dead_tuples for successful vacuum.';

COMMENT ON COLUMN pgconsole.maintenance_execution.pre_table_size IS
'Table size in bytes before maintenance. '
'VACUUM typically does not reduce this unless VACUUM FULL is used.';

COMMENT ON COLUMN pgconsole.maintenance_execution.post_table_size IS
'Table size in bytes after maintenance. '
'VACUUM FULL should show reduction; regular VACUUM typically shows no change or small increase.';

COMMENT ON TABLE pgconsole.activity_pattern IS
'Learned database activity patterns by day-of-week and hour-of-day. '
'Calculates average connections, active queries, and transaction rates for each time slot. '
'Activity score and maintenance suitability scores enable intelligent scheduling. '
'Updated periodically from system_metrics_history to adapt to changing workload patterns. '
'Unique constraint ensures one pattern per instance per time slot.';

COMMENT ON COLUMN pgconsole.activity_pattern.day_of_week IS
'Day of week: 0 (Sunday) through 6 (Saturday). '
'Distinguishes weekend vs weekday patterns.';

COMMENT ON COLUMN pgconsole.activity_pattern.hour_of_day IS
'Hour of day: 0-23 in server local time zone. '
'Identifies peak hours and maintenance windows.';

COMMENT ON COLUMN pgconsole.activity_pattern.avg_connections IS
'Average connection count during this time slot across all sampled periods. '
'Higher values indicate busier periods.';

COMMENT ON COLUMN pgconsole.activity_pattern.avg_active_queries IS
'Average number of actively executing queries during this time slot. '
'Better indicator of actual workload than connection count.';

COMMENT ON COLUMN pgconsole.activity_pattern.avg_transactions_per_hour IS
'Average transaction throughput (commits + rollbacks) per hour. '
'High transaction rates suggest poor time for maintenance.';

COMMENT ON COLUMN pgconsole.activity_pattern.activity_score IS
'Normalised activity score 0-100 (higher = busier). '
'Calculated from weighted combination of connections, active queries, and transaction rate. '
'Used to identify low-activity windows.';

COMMENT ON COLUMN pgconsole.activity_pattern.maintenance_suitability IS
'Suitability score 0-100 for scheduling maintenance (higher = better). '
'Inverse of activity_score with adjustments for time-of-day preferences. '
'Directly used by intelligent scheduler to find optimal execution windows.';

COMMENT ON COLUMN pgconsole.activity_pattern.sample_count IS
'Number of samples used to calculate averages. '
'Higher counts indicate more reliable patterns. Newly added time slots have low counts.';

-- ============================================================================
-- INSIGHTS DASHBOARD AGGREGATE DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE pgconsole.insight_summary IS
'Pre-calculated dashboard summary for fast insights page rendering. '
'Aggregates anomaly counts, recommendation counts, capacity forecasts, and health scores. '
'Updated periodically (e.g., every 15 minutes) to avoid expensive real-time calculations. '
'Top concerns JSONB provides ready-to-display critical issues for at-a-glance health assessment. '
'Overall health score (0-100) combines multiple factors: anomaly severity, recommendation urgency, capacity headroom.';

COMMENT ON COLUMN pgconsole.insight_summary.anomaly_count_critical IS
'Count of unresolved CRITICAL severity anomalies. '
'Requires immediate attention; displayed prominently in insights dashboard.';

COMMENT ON COLUMN pgconsole.insight_summary.anomaly_count_high IS
'Count of unresolved HIGH severity anomalies. '
'Should be investigated within hours.';

COMMENT ON COLUMN pgconsole.insight_summary.recommendation_count_critical IS
'Count of open CRITICAL severity recommendations. '
'Typically performance-impacting issues like missing critical indexes.';

COMMENT ON COLUMN pgconsole.insight_summary.storage_days_until_warning IS
'Days until forecasted storage reaches warning threshold. '
'NULL if no threshold breach predicted. Values <7 trigger alerts.';

COMMENT ON COLUMN pgconsole.insight_summary.storage_days_until_critical IS
'Days until forecasted storage reaches critical threshold. '
'NULL if no threshold breach predicted. Values <3 trigger urgent alerts.';

COMMENT ON COLUMN pgconsole.insight_summary.connections_days_until_warning IS
'Days until forecasted connection count reaches warning threshold. '
'NULL if no threshold breach predicted. Indicates need for connection pooling or max_connections increase.';

COMMENT ON COLUMN pgconsole.insight_summary.overall_health_score IS
'Composite health score 0-100 (100 = perfect health). '
'Factors: anomaly counts (weighted by severity), open critical recommendations, '
'capacity headroom, recent alert frequency. Provides single-number system health metric.';

COMMENT ON COLUMN pgconsole.insight_summary.top_concerns IS
'JSONB array of the 3-5 most critical issues for dashboard display. '
'[{"type": "anomaly", "severity": "CRITICAL", "title": "Connection spike detected", "link": "/anomalies/123"}]. '
'Pre-formatted for immediate rendering without additional queries.';
