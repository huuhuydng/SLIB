-- V30__add_library_settings_auto_cancel.sql
-- Add missing columns for auto-cancel and reputation settings

ALTER TABLE library_settings
ADD COLUMN IF NOT EXISTS min_reputation INTEGER NOT NULL DEFAULT 0;

ALTER TABLE library_settings
ADD COLUMN IF NOT EXISTS auto_cancel_minutes INTEGER NOT NULL DEFAULT 15;

ALTER TABLE library_settings
ADD COLUMN IF NOT EXISTS auto_cancel_on_leave_minutes INTEGER NOT NULL DEFAULT 30;

COMMENT ON COLUMN library_settings.min_reputation IS 'Điểm uy tín tối thiểu để đặt chỗ (0 = không giới hạn)';

COMMENT ON COLUMN library_settings.auto_cancel_minutes IS 'Số phút tự động hủy booking không confirm';

COMMENT ON COLUMN library_settings.auto_cancel_on_leave_minutes IS 'Số phút sau khi rời chỗ để tự hủy';