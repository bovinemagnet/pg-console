-- V7: Add missing columns to notification_channel table
-- These columns were expected by the repository but missing from V5 migration

-- Add instance_filter column for filtering notifications by instance
ALTER TABLE pgconsole.notification_channel
    ADD COLUMN IF NOT EXISTS instance_filter TEXT[];

-- Add rate_limit_per_hour column for throttling notifications
ALTER TABLE pgconsole.notification_channel
    ADD COLUMN IF NOT EXISTS rate_limit_per_hour INT;

-- Add test_mode column for testing channel configuration
ALTER TABLE pgconsole.notification_channel
    ADD COLUMN IF NOT EXISTS test_mode BOOLEAN NOT NULL DEFAULT FALSE;
