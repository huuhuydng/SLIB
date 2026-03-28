-- V4__add_library_settings_limits.sql
-- Columns max_bookings_per_day and max_hours_per_day are now part of V1 migration
-- This migration is kept for backward compatibility but does nothing if columns exist

-- Comment cho các cột
COMMENT ON COLUMN library_settings.max_bookings_per_day IS 'Số lần đặt tối đa mỗi ngày cho 1 user';
COMMENT ON COLUMN library_settings.max_hours_per_day IS 'Số giờ tối đa được đặt trong 1 ngày';

-- Clean up old column if exists
ALTER TABLE users DROP COLUMN IF EXISTS supabase_uid;