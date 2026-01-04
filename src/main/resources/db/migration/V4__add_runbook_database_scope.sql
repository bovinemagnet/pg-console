-- Add database scope to runbook executions
--
-- This allows runbooks to be executed against a specific database within an instance,
-- rather than at the instance level. This is important for diagnostics that should
-- be scoped to a particular database (e.g., connection analysis, query performance).

-- Add database_name column to runbook_execution table
ALTER TABLE pgconsole.runbook_execution
    ADD COLUMN IF NOT EXISTS database_name TEXT;

-- Add comment for documentation
COMMENT ON COLUMN pgconsole.runbook_execution.database_name IS
    'The specific database this runbook execution applies to. NULL means all databases (instance-wide).';

-- Add index for filtering executions by database
CREATE INDEX IF NOT EXISTS idx_runbook_execution_database
    ON pgconsole.runbook_execution(database_name)
    WHERE database_name IS NOT NULL;
