-- V5: Notification Channels & Alerting Integration
-- Adds support for Slack, Teams, PagerDuty, Discord notifications
-- with escalation policies and maintenance windows

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
    failure_count INT NOT NULL DEFAULT 0
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
