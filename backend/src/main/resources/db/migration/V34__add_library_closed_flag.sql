-- V34: Add library_closed flag to library_settings
-- Cho phép admin tạm khoá thư viện (sinh viên không thể đặt chỗ)
ALTER TABLE library_settings
ADD COLUMN library_closed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE library_settings ADD COLUMN closed_reason VARCHAR(500);