ALTER TABLE library_settings
    ADD COLUMN IF NOT EXISTS seat_confirmation_lead_minutes INTEGER NOT NULL DEFAULT 15,
    ADD COLUMN IF NOT EXISTS booking_reminder_lead_minutes INTEGER NOT NULL DEFAULT 15,
    ADD COLUMN IF NOT EXISTS expiry_warning_lead_minutes INTEGER NOT NULL DEFAULT 10;

UPDATE library_settings
SET seat_confirmation_lead_minutes = COALESCE(seat_confirmation_lead_minutes, 15),
    booking_reminder_lead_minutes = COALESCE(booking_reminder_lead_minutes, 15),
    expiry_warning_lead_minutes = COALESCE(expiry_warning_lead_minutes, 10)
WHERE id = 1;
