-- V39: Add performance indexes for frequently queried tables

-- Reservations indexes
CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations (status);
CREATE INDEX IF NOT EXISTS idx_reservations_user_id ON reservations (user_id);
CREATE INDEX IF NOT EXISTS idx_reservations_seat_id ON reservations (seat_id);
CREATE INDEX IF NOT EXISTS idx_reservations_start_time ON reservations (start_time);
CREATE INDEX IF NOT EXISTS idx_reservations_status_start_end ON reservations (status, start_time, end_time);
CREATE INDEX IF NOT EXISTS idx_reservations_created_at ON reservations (created_at);

-- Access logs indexes
CREATE INDEX IF NOT EXISTS idx_access_logs_checkout_null ON access_logs (check_out_time) WHERE check_out_time IS NULL;
CREATE INDEX IF NOT EXISTS idx_access_logs_checkin_time ON access_logs (check_in_time);
CREATE INDEX IF NOT EXISTS idx_access_logs_user_id ON access_logs (user_id);
