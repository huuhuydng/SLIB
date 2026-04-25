ALTER TABLE layout_schedules
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE layout_schedules
    ADD COLUMN max_retry_count INTEGER NOT NULL DEFAULT 3;

ALTER TABLE layout_schedules
    ADD COLUMN original_scheduled_for TIMESTAMP;

UPDATE layout_schedules
SET original_scheduled_for = scheduled_for
WHERE original_scheduled_for IS NULL;

ALTER TABLE layout_schedules
    ALTER COLUMN original_scheduled_for SET NOT NULL;
