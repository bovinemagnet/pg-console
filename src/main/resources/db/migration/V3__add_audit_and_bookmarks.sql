-- V3: Add audit log and query bookmarks tables for Phase 7

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
