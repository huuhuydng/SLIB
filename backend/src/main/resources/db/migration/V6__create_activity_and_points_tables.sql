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
created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() );

CREATE INDEX IF NOT EXISTS idx_activity_logs_user_id ON activity_logs (user_id);

CREATE INDEX IF NOT EXISTS idx_activity_logs_created_at ON activity_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_activity_logs_type ON activity_logs (activity_type);

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

CREATE INDEX IF NOT EXISTS idx_point_transactions_user_id ON point_transactions (user_id);

CREATE INDEX IF NOT EXISTS idx_point_transactions_created_at ON point_transactions (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_point_transactions_type ON point_transactions (transaction_type);