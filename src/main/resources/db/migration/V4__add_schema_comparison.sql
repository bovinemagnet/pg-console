-- V4: Schema Comparison & Migration Support for Phase 12
-- Author: Paul Snow
-- Version: 0.0.0

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

-- Add comment on tables
COMMENT ON TABLE pgconsole.comparison_profile IS 'Saved schema comparison configurations for quick re-runs';
COMMENT ON TABLE pgconsole.comparison_history IS 'Audit log of schema comparisons for drift detection';
