-- V8: Add Custom Dashboards support (Phase 23)
-- Allows users to create personalised dashboards with custom widgets

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
