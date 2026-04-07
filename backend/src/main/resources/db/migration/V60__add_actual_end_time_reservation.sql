-- Add actual_end_time column to track when student physically left the seat
-- This allows distinguishing between "left early" vs "completed on time"
ALTER TABLE reservations ADD COLUMN actual_end_time TIMESTAMP;

-- Index for efficient lookup of active confirmed reservations
CREATE INDEX IF NOT EXISTS idx_reservations_confirmed_active
ON reservations (status, start_time, end_time)
WHERE status = 'CONFIRMED';
