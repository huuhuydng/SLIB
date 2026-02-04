-- V13: Enforce 1:1 relationship between activity_logs and point_transactions
-- Đảm bảo mỗi ActivityLog chỉ có tối đa 1 PointTransaction

-- Thêm UNIQUE constraint vào activity_log_id
ALTER TABLE point_transactions
ADD CONSTRAINT unique_activity_log_transaction UNIQUE (activity_log_id);

-- Comment giải thích
COMMENT ON CONSTRAINT unique_activity_log_transaction ON point_transactions IS 'Đảm bảo quan hệ 1:1 - mỗi ActivityLog chỉ có 1 PointTransaction';