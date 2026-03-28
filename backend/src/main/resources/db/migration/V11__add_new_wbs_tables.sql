-- V11: Add new tables for missing WBS modules
-- Tables: reputation_rules, hce_devices, kiosk_images, complaints, feedbacks (new),
--         seat_status_reports, seat_violation_reports, notifications (new),
--         new_books, system_logs, backup_schedules, backup_history

-- Note: Keeping legacy tables from V1 for future use:
-- - chat_logs: For chat history tracking (UC139-145)
-- - student_schedules: For AI recommendation based on student schedule (UC082)
-- - reputation_history: Alternative tracking for reputation changes (UC095-096)
--   (point_transactions table also tracks this with more detail)

-- ========================================
-- Drop and recreate feedbacks with new structure
-- Old structure: feedback_id, user_id, content, sentiment_score, category, status
-- New structure: id, user_id, reservation_id, rating, content, category, ai_category_confidence, status, reviewed_by
-- ========================================
DROP TABLE IF EXISTS feedbacks CASCADE;

-- ========================================
-- Drop and recreate notifications with new structure
-- Old structure: notification_id, user_id, title, message, type, is_read
-- New structure: id, user_id, title, content, notification_type, reference_type, reference_id, is_read
-- ========================================
DROP TABLE IF EXISTS notifications CASCADE;

-- ========================================
-- Reputation Rules Table (WBS 4.4)
-- ========================================
CREATE TABLE IF NOT EXISTS reputation_rules (
    id SERIAL PRIMARY KEY,
    rule_code VARCHAR(50) UNIQUE NOT NULL,
    rule_name VARCHAR(200) NOT NULL,
    description TEXT,
    points INTEGER NOT NULL,
    rule_type VARCHAR(20) NOT NULL CHECK (
        rule_type IN ('PENALTY', 'REWARD')
    ),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

DROP TRIGGER IF EXISTS update_reputation_rules_updated_at ON reputation_rules;
CREATE TRIGGER update_reputation_rules_updated_at
    BEFORE UPDATE ON reputation_rules
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

-- Seed some default rules
INSERT INTO
    reputation_rules (
        rule_code,
        rule_name,
        description,
        points,
        rule_type
    )
VALUES (
        'NO_SHOW',
        'Không đến sau khi đặt chỗ',
        'Sinh viên đặt chỗ nhưng không đến trong thời gian quy định',
        -10,
        'PENALTY'
    ),
    (
        'LATE_CHECKOUT',
        'Trả chỗ muộn',
        'Trả chỗ quá thời gian quy định',
        -5,
        'PENALTY'
    ),
    (
        'NOISE_VIOLATION',
        'Gây ồn ào',
        'Gây ồn ào trong khu vực thư viện',
        -15,
        'PENALTY'
    ),
    (
        'UNAUTHORIZED_SEAT',
        'Sử dụng ghế không đúng',
        'Ngồi ghế đã được người khác đặt trước',
        -10,
        'PENALTY'
    ),
    (
        'CHECK_IN_BONUS',
        'Bonus check-in đúng giờ',
        'Check-in đúng giờ như đã đặt',
        2,
        'REWARD'
    ),
    (
        'WEEKLY_PERFECT',
        'Tuần hoàn hảo',
        'Không có vi phạm trong tuần',
        5,
        'REWARD'
    )
ON CONFLICT (rule_code) DO NOTHING;

-- ========================================
-- HCE Devices Table (WBS 4.6)
-- ========================================
CREATE TABLE IF NOT EXISTS hce_devices (
    id SERIAL PRIMARY KEY,
    device_id VARCHAR(50) UNIQUE NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    location VARCHAR(200),
    device_type VARCHAR(20) NOT NULL CHECK (
        device_type IN (
            'ENTRY_GATE',
            'EXIT_GATE',
            'SEAT_READER'
        )
    ),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (
        status IN (
            'ACTIVE',
            'INACTIVE',
            'MAINTENANCE'
        )
    ),
    last_heartbeat TIMESTAMP,
    area_id BIGINT REFERENCES areas (area_id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

DROP TRIGGER IF EXISTS update_hce_devices_updated_at ON hce_devices;
CREATE TRIGGER update_hce_devices_updated_at
    BEFORE UPDATE ON hce_devices
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

-- ========================================
-- Kiosk Images Table (WBS 4.8)
-- ========================================
CREATE TABLE IF NOT EXISTS kiosk_images (
    id SERIAL PRIMARY KEY,
    image_name VARCHAR(200) NOT NULL,
    image_url TEXT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    duration_seconds INTEGER NOT NULL DEFAULT 10,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

DROP TRIGGER IF EXISTS update_kiosk_images_updated_at ON kiosk_images;
CREATE TRIGGER update_kiosk_images_updated_at
    BEFORE UPDATE ON kiosk_images
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

-- ========================================
-- Complaints Table (WBS 7.2)
-- ========================================
CREATE TABLE IF NOT EXISTS complaints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL REFERENCES users (id),
    point_transaction_id UUID,
    subject VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    evidence_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (
        status IN (
            'PENDING',
            'ACCEPTED',
            'DENIED'
        )
    ),
    resolution_note TEXT,
    resolved_by UUID REFERENCES users (id),
    created_at TIMESTAMP DEFAULT NOW(),
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_complaints_user ON complaints (user_id);

CREATE INDEX IF NOT EXISTS idx_complaints_status ON complaints (status);

-- ========================================
-- Feedbacks Table (WBS 8.1) - New structure
-- ========================================
CREATE TABLE IF NOT EXISTS feedbacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL REFERENCES users (id),
    reservation_id UUID,
    rating INTEGER CHECK (
        rating >= 1
        AND rating <= 5
    ),
    content TEXT,
    category VARCHAR(50),
    ai_category_confidence DECIMAL(3, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW' CHECK (
        status IN ('NEW', 'REVIEWED', 'ACTED')
    ),
    reviewed_by UUID REFERENCES users (id),
    created_at TIMESTAMP DEFAULT NOW(),
    reviewed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_feedbacks_user ON feedbacks (user_id);

CREATE INDEX IF NOT EXISTS idx_feedbacks_status ON feedbacks (status);

-- ========================================
-- Seat Status Reports Table (WBS 8.2)
-- ========================================
CREATE TABLE IF NOT EXISTS seat_status_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL REFERENCES users (id),
    seat_id INTEGER NOT NULL REFERENCES seats (seat_id),
    issue_type VARCHAR(50) NOT NULL CHECK (
        issue_type IN (
            'BROKEN',
            'DIRTY',
            'MISSING_EQUIPMENT',
            'OTHER'
        )
    ),
    description TEXT,
    image_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (
        status IN (
            'PENDING',
            'VERIFIED',
            'RESOLVED',
            'REJECTED'
        )
    ),
    verified_by UUID REFERENCES users (id),
    created_at TIMESTAMP DEFAULT NOW(),
    verified_at TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_seat_status_reports_seat ON seat_status_reports (seat_id);

CREATE INDEX IF NOT EXISTS idx_seat_status_reports_status ON seat_status_reports (status);

-- ========================================
-- Seat Violation Reports Table (WBS 8.3)
-- ========================================
CREATE TABLE IF NOT EXISTS seat_violation_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    reporter_id UUID NOT NULL REFERENCES users (id),
    violator_id UUID REFERENCES users (id),
    seat_id INTEGER NOT NULL REFERENCES seats (seat_id),
    reservation_id UUID,
    violation_type VARCHAR(50) NOT NULL CHECK (
        violation_type IN (
            'UNAUTHORIZED_USE',
            'LEFT_BELONGINGS',
            'NOISE',
            'OTHER'
        )
    ),
    description TEXT,
    evidence_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (
        status IN (
            'PENDING',
            'VERIFIED',
            'RESOLVED',
            'REJECTED'
        )
    ),
    verified_by UUID REFERENCES users (id),
    point_deducted INTEGER,
    created_at TIMESTAMP DEFAULT NOW(),
    verified_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_seat_violation_reports_reporter ON seat_violation_reports (reporter_id);

CREATE INDEX IF NOT EXISTS idx_seat_violation_reports_status ON seat_violation_reports (status);

-- ========================================
-- Notifications Table (WBS 9) - New structure
-- ========================================
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL REFERENCES users (id),
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL CHECK (
        notification_type IN (
            'BOOKING',
            'REMINDER',
            'VIOLATION',
            'SYSTEM',
            'NEWS'
        )
    ),
    reference_type VARCHAR(50),
    reference_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications (user_id, is_read);

-- ========================================
-- New Books Table (WBS 10.4-10.6)
-- ========================================
CREATE TABLE IF NOT EXISTS new_books (
    id SERIAL PRIMARY KEY,
    title VARCHAR(300) NOT NULL,
    author VARCHAR(200),
    isbn VARCHAR(20),
    cover_url TEXT,
    description TEXT,
    category VARCHAR(100),
    publish_date DATE,
    arrival_date DATE DEFAULT CURRENT_DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID REFERENCES users (id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

DROP TRIGGER IF EXISTS update_new_books_updated_at ON new_books;
CREATE TRIGGER update_new_books_updated_at
    BEFORE UPDATE ON new_books
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

-- ========================================
-- System Logs Table (WBS 4.9.3)
-- ========================================
CREATE TABLE IF NOT EXISTS system_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID REFERENCES users (id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(100),
    details JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_system_logs_created ON system_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_system_logs_user ON system_logs (user_id);

CREATE INDEX IF NOT EXISTS idx_system_logs_action ON system_logs (action);

-- ========================================
-- Backup Schedules Table (WBS 4.9.4-4.9.5)
-- ========================================
CREATE TABLE IF NOT EXISTS backup_schedules (
    id SERIAL PRIMARY KEY,
    schedule_name VARCHAR(100) NOT NULL,
    cron_expression VARCHAR(50) NOT NULL,
    backup_type VARCHAR(20) NOT NULL CHECK (
        backup_type IN ('FULL', 'INCREMENTAL')
    ),
    retain_days INTEGER NOT NULL DEFAULT 30,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_backup_at TIMESTAMP,
    next_backup_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

DROP TRIGGER IF EXISTS update_backup_schedules_updated_at ON backup_schedules;
CREATE TRIGGER update_backup_schedules_updated_at
    BEFORE UPDATE ON backup_schedules
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();

-- Insert default backup schedule
INSERT INTO
    backup_schedules (
        schedule_name,
        cron_expression,
        backup_type,
        retain_days
    )
VALUES (
        'Daily Full Backup',
        '0 2 * * *',
        'FULL',
        30
    ),
    (
        'Weekly Incremental',
        '0 3 * * 0',
        'INCREMENTAL',
        7
    )
ON CONFLICT DO NOTHING;

-- ========================================
-- Backup History Table (WBS 4.9.4-4.9.5)
-- ========================================
CREATE TABLE IF NOT EXISTS backup_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    schedule_id INTEGER REFERENCES backup_schedules (id),
    file_path TEXT NOT NULL,
    file_size_bytes BIGINT,
    status VARCHAR(20) NOT NULL CHECK (
        status IN ('SUCCESS', 'FAILED')
    ),
    error_message TEXT,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_by UUID REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_backup_history_schedule ON backup_history (schedule_id);

CREATE INDEX IF NOT EXISTS idx_backup_history_status ON backup_history (status);