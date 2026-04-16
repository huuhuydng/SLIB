ALTER TABLE library_settings
    ADD COLUMN IF NOT EXISTS booking_cancel_deadline_hours INTEGER NOT NULL DEFAULT 12;

UPDATE library_settings
SET booking_cancel_deadline_hours = COALESCE(booking_cancel_deadline_hours, 12)
WHERE id = 1;
