-- Add notification setting columns to library_settings table
-- These control whether each notification type is sent globally (admin config)
ALTER TABLE library_settings
    ADD COLUMN IF NOT EXISTS notify_booking_success BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS notify_checkin_reminder BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS notify_time_expiry BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS notify_violation BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS notify_weekly_report BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS notify_device_alert BOOLEAN NOT NULL DEFAULT TRUE;
