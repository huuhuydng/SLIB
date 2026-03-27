-- Prevent the same user from having multiple active reservations in the same time slot.
-- This is a safety net against race conditions (e.g. concurrent booking from kiosk + mobile).
CREATE UNIQUE INDEX IF NOT EXISTS uq_reservation_user_slot_active
ON reservations (user_id, start_time, end_time)
WHERE status IN ('BOOKED', 'PROCESSING', 'CONFIRMED');
