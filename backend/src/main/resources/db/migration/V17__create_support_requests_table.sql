-- V17__create_support_requests_table.sql
-- Bảng yêu cầu hỗ trợ từ sinh viên

CREATE TABLE support_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    student_id UUID NOT NULL REFERENCES users (id),
    description TEXT NOT NULL,
    image_urls TEXT [],
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_response TEXT,
    resolved_by UUID REFERENCES users (id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX idx_support_requests_student ON support_requests (student_id);

CREATE INDEX idx_support_requests_status ON support_requests (status);

CREATE INDEX idx_support_requests_created ON support_requests (created_at DESC);