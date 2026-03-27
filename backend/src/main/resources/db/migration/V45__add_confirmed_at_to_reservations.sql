ALTER TABLE reservations
ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP;

UPDATE reservations
SET confirmed_at = start_time
WHERE confirmed_at IS NULL
  AND status IN ('CONFIRMED', 'COMPLETED');

CREATE INDEX IF NOT EXISTS idx_reservations_feedback_pending
ON reservations (user_id, end_time DESC)
WHERE confirmed_at IS NOT NULL
  AND status IN ('CONFIRMED', 'COMPLETED');
