-- Feedbacks table: Phản hồi sinh viên sau check-out
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
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    reviewed_by UUID REFERENCES users (id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP
);

-- Complaints table: Khiếu nại vi phạm
CREATE TABLE IF NOT EXISTS complaints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL REFERENCES users (id),
    point_transaction_id UUID,
    subject VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    evidence_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolution_note TEXT,
    resolved_by UUID REFERENCES users (id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_feedbacks_user ON feedbacks (user_id);

CREATE INDEX IF NOT EXISTS idx_feedbacks_status ON feedbacks (status);

CREATE INDEX IF NOT EXISTS idx_complaints_user ON complaints (user_id);

CREATE INDEX IF NOT EXISTS idx_complaints_status ON complaints (status);