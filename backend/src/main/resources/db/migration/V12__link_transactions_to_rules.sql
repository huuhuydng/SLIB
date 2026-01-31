-- V12: Link point_transactions to reputation_rules
-- Thêm foreign key để mỗi giao dịch điểm tham chiếu đến quy tắc đã áp dụng

-- Thêm cột rule_id vào bảng point_transactions
ALTER TABLE point_transactions
ADD COLUMN IF NOT EXISTS rule_id INTEGER REFERENCES reputation_rules (id);

-- Tạo index để tăng tốc lookup
CREATE INDEX IF NOT EXISTS idx_point_transactions_rule ON point_transactions (rule_id);

-- Comment giải thích
COMMENT ON COLUMN point_transactions.rule_id IS 'FK đến reputation_rules - quy tắc nào đã áp dụng cho giao dịch này';