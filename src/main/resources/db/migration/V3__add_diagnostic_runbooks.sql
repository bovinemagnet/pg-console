-- Add diagnostic-only runbooks and mark safe runbooks as auto-executable
--
-- This migration:
-- 1. Creates high_connection_usage_diagnostic - a read-only version of high_connection_usage
--    without the destructive pg_terminate_backend step
-- 2. Marks low_cache_hit as auto-executable since all its steps are read-only

-- Insert the diagnostic version of high_connection_usage
-- Contains only the first 4 steps (all read-only operations)
INSERT INTO pgconsole.runbook (name, title, description, category, trigger_type, trigger_conditions, steps, estimated_duration_minutes, auto_executable)
VALUES
    ('high_connection_usage_diagnostic', 'High Connection Usage Diagnostic',
     'Diagnostic investigation of high connection usage. This read-only runbook gathers connection data without terminating any connections. Use the full high_connection_usage runbook if connection termination is needed.',
     'INCIDENT', 'ALERT',
     '{"alert_type": "CONNECTION_THRESHOLD", "severity": ["CRITICAL", "HIGH"]}',
     '[
         {"order": 1, "title": "Check Connection Count", "description": "View current active connections and their states", "action_type": "NAVIGATE", "action": "/activity", "expected_outcome": "Review the number of active, idle, and idle-in-transaction connections", "auto_execute": true},
         {"order": 2, "title": "Identify Long-Running Queries", "description": "Look for queries running longer than expected that may be holding connections", "action_type": "NAVIGATE", "action": "/slow-queries?sort=duration", "expected_outcome": "Identify any unusually long-running queries that should be investigated", "auto_execute": true},
         {"order": 3, "title": "Check for Idle-in-Transaction Connections", "description": "Identify connections stuck in idle-in-transaction state for over 5 minutes", "action_type": "QUERY", "action": "SELECT pid, usename, application_name, client_addr, state, now() - xact_start AS transaction_duration, now() - query_start AS query_duration, LEFT(query, 100) AS query_preview FROM pg_stat_activity WHERE state = ''idle in transaction'' AND now() - xact_start > interval ''5 minutes'' ORDER BY xact_start", "expected_outcome": "List of connections that have been idle in transaction for too long - these may need attention", "auto_execute": true},
         {"order": 4, "title": "Check Connection Distribution by Application", "description": "See which applications are using the most connections", "action_type": "QUERY", "action": "SELECT application_name, state, COUNT(*) as connection_count, COUNT(*) FILTER (WHERE state = ''active'') as active, COUNT(*) FILTER (WHERE state = ''idle'') as idle, COUNT(*) FILTER (WHERE state = ''idle in transaction'') as idle_in_txn FROM pg_stat_activity WHERE backend_type = ''client backend'' GROUP BY application_name, state ORDER BY connection_count DESC", "expected_outcome": "Identify which applications are consuming the most connections and their states", "auto_execute": true}
     ]', 5, TRUE)
ON CONFLICT (name) DO NOTHING;

-- Mark low_cache_hit as auto-executable since all steps are read-only
-- Steps: NAVIGATE (dashboard), QUERY (SHOW), QUERY (pg_statio_user_tables), DOCUMENTATION
UPDATE pgconsole.runbook
SET auto_executable = TRUE
WHERE name = 'low_cache_hit';
