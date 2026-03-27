-- Add timestamp for tracking when NFC UID was last assigned/changed on a seat
ALTER TABLE seats
ADD COLUMN IF NOT EXISTS nfc_tag_uid_updated_at TIMESTAMP;