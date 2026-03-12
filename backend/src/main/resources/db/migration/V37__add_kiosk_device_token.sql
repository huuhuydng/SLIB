-- V37: Them cot device_token cho kiosk_configs de ho tro xac thuc doc lap
ALTER TABLE kiosk_configs
    ADD COLUMN IF NOT EXISTS device_token TEXT,
    ADD COLUMN IF NOT EXISTS device_token_issued_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS device_token_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS device_token_issued_by UUID,
    ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMP;
