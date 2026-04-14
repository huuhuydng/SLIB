ALTER TABLE library_settings
    ADD COLUMN IF NOT EXISTS closed_from_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS closed_until_at TIMESTAMP;
