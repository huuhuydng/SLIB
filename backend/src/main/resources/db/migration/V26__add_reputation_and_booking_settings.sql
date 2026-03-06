-- V26: Add reputation and auto-cancel settings
-- Thêm cấu hình điểm uy tín tối thiểu và auto-cancel booking

ALTER TABLE library_settings
ADD COLUMN IF NOT EXISTS min_reputation INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS auto_cancel_minutes INTEGER NOT NULL DEFAULT 15,
ADD COLUMN IF NOT EXISTS auto_cancel_on_leave_minutes INTEGER NOT NULL DEFAULT 30;
