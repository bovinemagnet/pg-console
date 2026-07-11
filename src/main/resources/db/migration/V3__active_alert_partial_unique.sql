-- C10: The table-wide UNIQUE on active_alert.alert_id suppressed recurring
-- incidents. Deduplication only matches unresolved rows, but a resolved row
-- (retained until purge) with the same alert_id made the next fire of the same
-- condition violate the constraint and throw, permanently hiding the incident.
--
-- Replace the table-wide uniqueness with a PARTIAL unique index that applies
-- only to unresolved rows, so at most one active alert per alert_id exists
-- while any number of historical resolved rows may share it.

ALTER TABLE pgconsole.active_alert
    DROP CONSTRAINT IF EXISTS active_alert_alert_id_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_active_alert_unresolved_alert_id
    ON pgconsole.active_alert (alert_id)
    WHERE resolved = FALSE;
