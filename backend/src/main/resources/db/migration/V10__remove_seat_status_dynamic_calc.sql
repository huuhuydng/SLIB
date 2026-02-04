-- V10: Remove seat_status column, use dynamic calculation from reservations
--
-- GOAL: Seat availability is now calculated dynamically from reservations table
-- instead of storing status in seats table.
--
-- Changes:
-- 1. Add is_active column (replaces UNAVAILABLE status for admin restrictions)
-- 2. Migrate UNAVAILABLE seats to is_active = FALSE
-- 3. Drop seat_status, hold_expires_at, held_by_user columns

-- Step 1: Add is_active column
ALTER TABLE seats
ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Step 2: Migrate UNAVAILABLE status to is_active = FALSE
UPDATE seats SET is_active = FALSE WHERE seat_status = 'UNAVAILABLE';

-- Step 3: Drop columns no longer needed (seat status is now calculated dynamically)
ALTER TABLE seats DROP COLUMN IF EXISTS seat_status;

ALTER TABLE seats DROP COLUMN IF EXISTS hold_expires_at;

ALTER TABLE seats DROP COLUMN IF EXISTS held_by_user;