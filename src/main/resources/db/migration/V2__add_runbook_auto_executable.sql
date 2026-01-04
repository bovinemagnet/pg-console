-- Add auto_executable column to runbook table
-- This allows certain runbooks (like vacuum maintenance) to be auto-executed
-- since they contain only non-destructive operations

ALTER TABLE pgconsole.runbook
    ADD COLUMN IF NOT EXISTS auto_executable BOOLEAN DEFAULT FALSE;

-- Add comment for documentation
COMMENT ON COLUMN pgconsole.runbook.auto_executable IS
'Whether this runbook can be auto-executed. Only enable for runbooks with non-destructive operations (e.g., VACUUM, ANALYSE, read-only queries).';

-- Mark vacuum_maintenance runbook as auto-executable
UPDATE pgconsole.runbook
SET auto_executable = TRUE
WHERE name = 'vacuum_maintenance';

-- Also mark any other safe maintenance runbooks as auto-executable
-- (add more as needed)
