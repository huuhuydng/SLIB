-- =============================================
-- V24: Kiosk Session & Config Tables
-- =============================================

-- 1. Bảng cấu hình Kiosk Device
CREATE TABLE IF NOT EXISTS kiosk_configs (
    id SERIAL PRIMARY KEY,
    kiosk_code VARCHAR(50) NOT NULL UNIQUE,  -- KIOSK_001, KIOSK_002
    kiosk_name VARCHAR(200) NOT NULL,
    kiosk_type VARCHAR(50) NOT NULL,          -- 'INTERACTIVE', 'MONITORING'
    location VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    qr_secret_key VARCHAR(255),               -- Secret cho QR signature
    last_heartbeat TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng lưu session QR tạm thời
CREATE TABLE IF NOT EXISTS kiosk_qr_sessions (
    id SERIAL PRIMARY KEY,
    kiosk_id INTEGER NOT NULL REFERENCES kiosk_configs(id),
    session_token VARCHAR(255) NOT NULL UNIQUE,
    student_id UUID,                           -- FK to users (sau khi scan thành công)
    access_log_id UUID,                        -- FK to access_logs (check-in record)
    qr_payload TEXT NOT NULL,                   -- Nội dung QR đã mã hóa
    qr_expires_at TIMESTAMP NOT NULL,          -- Thời hạn QR (10 phút)
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, USED, EXPIRED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default kiosk config for testing
INSERT INTO kiosk_configs (kiosk_code, kiosk_name, kiosk_type, location, is_active, qr_secret_key)
VALUES ('KIOSK_001', 'Kiosk Sảnh A', 'INTERACTIVE', 'Sảnh tầng 1', true, 'slib_kiosk_secret_key_2024')
ON CONFLICT (kiosk_code) DO NOTHING;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_kiosk_configs_active ON kiosk_configs (is_active);
CREATE INDEX IF NOT EXISTS idx_kiosk_qr_sessions_token ON kiosk_qr_sessions (session_token);
CREATE INDEX IF NOT EXISTS idx_kiosk_qr_sessions_expires ON kiosk_qr_sessions (qr_expires_at);
CREATE INDEX IF NOT EXISTS idx_kiosk_qr_sessions_status ON kiosk_qr_sessions (status);
