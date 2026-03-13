-- V13: Enforce 1:1 relationship between activity_logs and point_transactions

DO $$
BEGIN
    ALTER TABLE point_transactions
    ADD CONSTRAINT unique_activity_log_transaction UNIQUE (activity_log_id);
EXCEPTION WHEN duplicate_object THEN
    NULL;
END $$;

COMMENT ON CONSTRAINT unique_activity_log_transaction ON point_transactions IS 'Enforce 1:1 - each ActivityLog has at most 1 PointTransaction';
