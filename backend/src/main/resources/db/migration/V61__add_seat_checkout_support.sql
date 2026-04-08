-- Add seat_id to access_logs for tracking which seat student checked out from
ALTER TABLE access_logs ADD COLUMN IF NOT EXISTS seat_id INTEGER REFERENCES seats(seat_id);
CREATE INDEX IF NOT EXISTS idx_access_logs_seat_id ON access_logs(seat_id);

-- Add actual_end_time to reservation (already in V60 migration, add index)
CREATE INDEX IF NOT EXISTS idx_reservations_confirmed_active
ON reservations (status, start_time, end_time)
WHERE status = 'CONFIRMED';
