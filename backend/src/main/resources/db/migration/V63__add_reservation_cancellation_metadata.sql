ALTER TABLE reservations
ADD COLUMN IF NOT EXISTS cancellation_reason TEXT;

ALTER TABLE reservations
ADD COLUMN IF NOT EXISTS cancelled_by_user_id UUID;
