-- ============================================================
-- V15: Cleanup Legacy Columns
-- Remove unused columns from seats and reservations tables
-- ============================================================

-- ========================
-- 1. SEATS TABLE CLEANUP
-- ========================

-- Drop foreign key constraint for held_by_user first
ALTER TABLE seats DROP CONSTRAINT IF EXISTS seats_held_by_user_fkey;

-- Drop the seat_status column (now calculated dynamically by SeatAvailabilityService)
ALTER TABLE seats DROP COLUMN IF EXISTS seat_status;

-- Drop the seat_status enum type if no other tables use it
DROP TYPE IF EXISTS seat_status CASCADE;

-- Drop hold_expires_at column (legacy holding mechanism)
ALTER TABLE seats DROP COLUMN IF EXISTS hold_expires_at;

-- Drop held_by_user column (legacy holding mechanism)
ALTER TABLE seats DROP COLUMN IF EXISTS held_by_user;

-- ========================
-- 2. RESERVATIONS TABLE CLEANUP
-- ========================

-- Drop created_time column (duplicate of created_at)
ALTER TABLE reservations DROP COLUMN IF EXISTS created_time;

-- ============================================================
-- Summary:
-- - seats: removed seat_status, hold_expires_at, held_by_user
-- - reservations: removed created_time (kept created_at)
-- ============================================================