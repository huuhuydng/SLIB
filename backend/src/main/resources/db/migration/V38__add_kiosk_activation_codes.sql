CREATE TABLE IF NOT EXISTS kiosk_activation_codes (
    id SERIAL PRIMARY KEY,
    kiosk_id INTEGER NOT NULL REFERENCES kiosk_configs(id) ON DELETE CASCADE,
    code VARCHAR(6) NOT NULL UNIQUE,
    device_token TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kiosk_activation_codes_code ON kiosk_activation_codes(code);
CREATE INDEX IF NOT EXISTS idx_kiosk_activation_codes_expires ON kiosk_activation_codes(expires_at);
