-- Xóa column last_heartbeat khỏi bảng kiosk_configs (không được sử dụng)
ALTER TABLE kiosk_configs DROP COLUMN IF EXISTS last_heartbeat;