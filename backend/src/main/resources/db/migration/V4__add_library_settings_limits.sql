-- V4__add_library_settings_limits.sql
-- Thêm các cột giới hạn đặt chỗ cho library_settings

-- Thêm cột max_bookings_per_day (số lần đặt tối đa mỗi ngày)
ALTER TABLE library_settings
ADD COLUMN IF NOT EXISTS max_bookings_per_day INTEGER NOT NULL DEFAULT 3;

-- Thêm cột max_hours_per_day (số giờ tối đa được đặt trong 1 ngày)
ALTER TABLE library_settings
ADD COLUMN IF NOT EXISTS max_hours_per_day INTEGER NOT NULL DEFAULT 4;

-- Comment cho các cột mới
COMMENT ON COLUMN library_settings.max_bookings_per_day IS 'Số lần đặt tối đa mỗi ngày cho 1 user';

COMMENT ON COLUMN library_settings.max_hours_per_day IS 'Số giờ tối đa được đặt trong 1 ngày';

ALTER TABLE users DROP COLUMN IF EXISTS supabase_uid;