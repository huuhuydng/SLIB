-- V6: Create activity_logs and point_transactions tables for Activity History feature

-- ========================================
-- 1. Activity Logs Table
-- ========================================
CREATE TABLE IF NOT EXISTS activity_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

-- Activity type: CHECK_IN, CHECK_OUT, BOOKING_SUCCESS, BOOKING_CANCEL, NFC_CONFIRM, GATE_ENTRY, NO_SHOW
activity_type VARCHAR(50) NOT NULL,

-- Activity details
title VARCHAR(255) NOT NULL, -- e.g., "Check-in thành công"
description TEXT, -- e.g., "Khu yên tỉnh - Ghế A15"

-- Optional references
reservation_id UUID REFERENCES reservations (reservation_id) ON DELETE SET NULL,
seat_code VARCHAR(20),
zone_name VARCHAR(100),

-- Duration for check-out activities (in minutes)
duration_minutes INTEGER,

-- Metadata
created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

-- Indexing
CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_activity_logs_user_id ON activity_logs (user_id);

CREATE INDEX idx_activity_logs_created_at ON activity_logs (created_at DESC);

CREATE INDEX idx_activity_logs_type ON activity_logs (activity_type);

-- ========================================
-- 2. Point Transactions Table
-- ========================================
CREATE TABLE IF NOT EXISTS point_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

-- Point change: positive = credit, negative = debit
points INTEGER NOT NULL,

-- Transaction type: REWARD, PENALTY, WEEKLY_BONUS, NO_SHOW_PENALTY, CHECK_OUT_LATE_PENALTY
transaction_type VARCHAR(50) NOT NULL,

-- Details
title VARCHAR(255) NOT NULL, -- e.g., "Thưởng: Tuần học chăm chỉ"
description TEXT, -- e.g., "Hoàn thành 10 giờ học trong tuần"

-- Balance after this transaction
balance_after INTEGER,

-- Optional reference to activity that caused this
activity_log_id UUID REFERENCES activity_logs (id) ON DELETE SET NULL,

-- Metadata
created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() );

CREATE INDEX idx_point_transactions_user_id ON point_transactions (user_id);

CREATE INDEX idx_point_transactions_created_at ON point_transactions (created_at DESC);

CREATE INDEX idx_point_transactions_type ON point_transactions (transaction_type);

-- ========================================
-- 3. Sample Data
-- ========================================

-- Get a sample user ID (first user in system)
DO $$
DECLARE
    sample_user_id UUID;
    activity_id1 UUID;
    activity_id2 UUID;
    activity_id3 UUID;
BEGIN
    -- Get first user
    SELECT id INTO sample_user_id FROM users LIMIT 1;
    
    IF sample_user_id IS NOT NULL THEN
        -- Insert sample activity logs
        INSERT INTO activity_logs (id, user_id, activity_type, title, description, seat_code, zone_name, duration_minutes, created_at)
        VALUES 
            (gen_random_uuid(), sample_user_id, 'CHECK_OUT', 'Check-out thành công', 'Khu yên tỉnh - Ghế A15', 'A15', 'Khu yên tỉnh', 125, NOW() - INTERVAL '1 hour')
        RETURNING id INTO activity_id1;
        
        INSERT INTO activity_logs (id, user_id, activity_type, title, description, seat_code, zone_name, created_at)
        VALUES 
            (gen_random_uuid(), sample_user_id, 'CHECK_IN', 'Check-in thành công', 'Khu yên tỉnh - Ghế A15', 'A15', 'Khu yên tỉnh', NOW() - INTERVAL '3 hours')
        RETURNING id INTO activity_id2;
        
        INSERT INTO activity_logs (id, user_id, activity_type, title, description, seat_code, zone_name, created_at)
        VALUES 
            (gen_random_uuid(), sample_user_id, 'BOOKING_SUCCESS', 'Đặt chỗ thành công', 'Đã đặt ghế A15 (09:00 - 11:00)', 'A15', 'Khu yên tỉnh', NOW() - INTERVAL '5 hours')
        RETURNING id INTO activity_id3;
        
        INSERT INTO activity_logs (user_id, activity_type, title, description, seat_code, zone_name, duration_minutes, created_at)
        VALUES 
            (sample_user_id, 'CHECK_OUT', 'Check-out thành công', 'Khu thảo luận - Ghế B02', 'B02', 'Khu thảo luận', 209, NOW() - INTERVAL '1 day'),
            (sample_user_id, 'GATE_ENTRY', 'Check-in vào cửa', 'Khu thảo luận - Ghế B02', 'B02', 'Khu thảo luận', NULL, NOW() - INTERVAL '1 day'),
            (sample_user_id, 'NO_SHOW', 'Không check-in (No-show)', 'Đã đặt ghế C05 nhưng không đến check-in', 'C05', 'Khu máy tính', NULL, NOW() - INTERVAL '3 days'),
            (sample_user_id, 'BOOKING_CANCEL', 'Hủy đặt chỗ', 'Đã hủy đặt ghế D10 (14:00 - 16:00)', 'D10', 'Phòng họp nhóm', NULL, NOW() - INTERVAL '5 days');
        
        -- Insert sample point transactions
        INSERT INTO point_transactions (user_id, points, transaction_type, title, description, balance_after, created_at)
        VALUES 
            (sample_user_id, -10, 'NO_SHOW_PENALTY', 'Không check-in (No-show)', 'Bạn đã đặt ghế A15 nhưng không đến check-in trong thời gian quy định.', 90, NOW() - INTERVAL '3 days'),
            (sample_user_id, 5, 'WEEKLY_BONUS', 'Thưởng: Tuần học chăm chỉ', 'Hoàn thành 10 giờ học trong tuần.', 100, NOW() - INTERVAL '7 days'),
            (sample_user_id, -5, 'CHECK_OUT_LATE_PENALTY', 'Check-out trễ', 'Bạn rời khỏi thư viện nhưng quên check-out quá 30 phút.', 95, NOW() - INTERVAL '10 days'),
            (sample_user_id, 10, 'WEEKLY_BONUS', 'Thưởng: Tuần học xuất sắc', 'Hoàn thành 20 giờ học trong tuần.', 100, NOW() - INTERVAL '14 days'),
            (sample_user_id, -3, 'PENALTY', 'Vi phạm nội quy', 'Gây ồn ào trong khu yên tỉnh', 87, NOW() - INTERVAL '20 days');
    END IF;
END $$;