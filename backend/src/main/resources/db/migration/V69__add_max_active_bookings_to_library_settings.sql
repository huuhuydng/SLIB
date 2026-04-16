ALTER TABLE library_settings
    ADD COLUMN IF NOT EXISTS max_active_bookings INTEGER NOT NULL DEFAULT 2;
