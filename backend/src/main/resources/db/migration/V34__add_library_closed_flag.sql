-- V34: Add library_closed flag to library_settings
ALTER TABLE library_settings ADD COLUMN IF NOT EXISTS library_closed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE library_settings ADD COLUMN IF NOT EXISTS closed_reason VARCHAR(500);
