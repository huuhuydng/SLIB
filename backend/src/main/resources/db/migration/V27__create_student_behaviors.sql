-- V27: Create student_behaviors table
-- Bảng lưu trữ hành vi của sinh viên để phân tích AI

CREATE TABLE IF NOT EXISTS student_behaviors (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    behavior_type VARCHAR(50) NOT NULL,
    description TEXT,
    related_booking_id INTEGER,
    related_seat_id INTEGER,
    related_zone_id INTEGER,
    points_impact INTEGER,
    metadata TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster queries
CREATE INDEX IF NOT EXISTS idx_student_behaviors_user_id ON student_behaviors(user_id);
CREATE INDEX IF NOT EXISTS idx_student_behaviors_type ON student_behaviors(behavior_type);
CREATE INDEX IF NOT EXISTS idx_student_behaviors_created_at ON student_behaviors(created_at);
