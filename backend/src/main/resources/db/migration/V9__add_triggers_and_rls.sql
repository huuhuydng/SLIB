-- V9: Add auto user_settings creation, updated_at triggers, and Row-Level Security
-- This migration adds:
-- 1. Trigger to auto-create user_settings when user is created
-- 2. updated_at triggers for tables that have updated_at column but no trigger
-- 3. Row-Level Security (RLS) for all user-related tables

-- ========================================
-- 1. Auto-create user_settings when user is created
-- ========================================
CREATE OR REPLACE FUNCTION create_user_settings_automatically()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO user_settings (user_id)
    VALUES (NEW.id)
    ON CONFLICT (user_id) DO NOTHING;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop trigger if exists to avoid duplicate
DROP TRIGGER IF EXISTS create_user_settings_on_user_insert ON users;

CREATE TRIGGER create_user_settings_on_user_insert
    AFTER INSERT ON users
    FOR EACH ROW
    EXECUTE FUNCTION create_user_settings_automatically();

-- ========================================
-- 2. Add updated_at triggers for tables missing them
-- ========================================

-- refresh_tokens table
DROP TRIGGER IF EXISTS update_refresh_tokens_updated_at ON refresh_tokens;

CREATE TRIGGER update_refresh_tokens_updated_at
    BEFORE UPDATE ON refresh_tokens
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- import_jobs table
DROP TRIGGER IF EXISTS update_import_jobs_updated_at ON import_jobs;

CREATE TRIGGER update_import_jobs_updated_at
    BEFORE UPDATE ON import_jobs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ========================================
-- 3. Row-Level Security (RLS)
-- ========================================

-- Enable RLS on user-related tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

ALTER TABLE user_settings ENABLE ROW LEVEL SECURITY;

ALTER TABLE reservations ENABLE ROW LEVEL SECURITY;

ALTER TABLE access_logs ENABLE ROW LEVEL SECURITY;

ALTER TABLE chat_logs ENABLE ROW LEVEL SECURITY;

ALTER TABLE feedbacks ENABLE ROW LEVEL SECURITY;

ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

ALTER TABLE reputation_history ENABLE ROW LEVEL SECURITY;

ALTER TABLE student_schedules ENABLE ROW LEVEL SECURITY;

ALTER TABLE activity_logs ENABLE ROW LEVEL SECURITY;

ALTER TABLE point_transactions ENABLE ROW LEVEL SECURITY;

ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;

-- ========================================
-- RLS Policies for users table
-- ========================================
DROP POLICY IF EXISTS users_select_own ON users;

CREATE POLICY users_select_own ON users FOR
SELECT USING (
        id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS users_update_own ON users;

CREATE POLICY users_update_own ON users
FOR UPDATE
    USING (
        id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS users_insert_staff ON users;

CREATE POLICY users_insert_staff ON users FOR INSERT
WITH
    CHECK (is_staff ());

DROP POLICY IF EXISTS users_delete_staff ON users;

CREATE POLICY users_delete_staff ON users FOR DELETE USING (is_staff ());

-- ========================================
-- RLS Policies for user_settings table
-- ========================================
DROP POLICY IF EXISTS user_settings_select_own ON user_settings;

CREATE POLICY user_settings_select_own ON user_settings FOR
SELECT USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS user_settings_update_own ON user_settings;

CREATE POLICY user_settings_update_own ON user_settings
FOR UPDATE
    USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS user_settings_insert_own ON user_settings;

CREATE POLICY user_settings_insert_own ON user_settings FOR INSERT
WITH
    CHECK (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

-- ========================================
-- RLS Policies for reservations table
-- ========================================
DROP POLICY IF EXISTS reservations_select_own ON reservations;

CREATE POLICY reservations_select_own ON reservations FOR
SELECT USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS reservations_insert_own ON reservations;

CREATE POLICY reservations_insert_own ON reservations FOR INSERT
WITH
    CHECK (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS reservations_update_own ON reservations;

CREATE POLICY reservations_update_own ON reservations
FOR UPDATE
    USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS reservations_delete_own ON reservations;

CREATE POLICY reservations_delete_own ON reservations FOR DELETE USING (
    user_id = get_current_user_id ()
    OR is_staff ()
);

-- ========================================
-- RLS Policies for access_logs table
-- ========================================
DROP POLICY IF EXISTS access_logs_select_own ON access_logs;

CREATE POLICY access_logs_select_own ON access_logs FOR
SELECT USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS access_logs_insert_any ON access_logs;

CREATE POLICY access_logs_insert_any ON access_logs FOR INSERT
WITH
    CHECK (TRUE);
-- System can insert for any user

-- ========================================
-- RLS Policies for chat_logs table
-- ========================================
DROP POLICY IF EXISTS chat_logs_select_own ON chat_logs;

CREATE POLICY chat_logs_select_own ON chat_logs FOR
SELECT USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS chat_logs_insert_own ON chat_logs;

CREATE POLICY chat_logs_insert_own ON chat_logs FOR INSERT
WITH
    CHECK (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

-- ========================================
-- RLS Policies for feedbacks table
-- ========================================
DROP POLICY IF EXISTS feedbacks_select_own ON feedbacks;

CREATE POLICY feedbacks_select_own ON feedbacks FOR
SELECT USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS feedbacks_insert_own ON feedbacks;

CREATE POLICY feedbacks_insert_own ON feedbacks FOR INSERT
WITH
    CHECK (
        user_id = get_current_user_id ()
    );

DROP POLICY IF EXISTS feedbacks_update_staff ON feedbacks;

CREATE POLICY feedbacks_update_staff ON feedbacks
FOR UPDATE
    USING (is_staff ());

-- ========================================
-- RLS Policies for notifications table
-- ========================================
DROP POLICY IF EXISTS notifications_select_own ON notifications;

CREATE POLICY notifications_select_own ON notifications FOR
SELECT USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS notifications_insert_staff ON notifications;

CREATE POLICY notifications_insert_staff ON notifications FOR INSERT
WITH
    CHECK (is_staff ());

DROP POLICY IF EXISTS notifications_update_own ON notifications;

CREATE POLICY notifications_update_own ON notifications
FOR UPDATE
    USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

-- ========================================
-- RLS Policies for reputation_history table
-- ========================================
DROP POLICY IF EXISTS reputation_history_select_own ON reputation_history;

CREATE POLICY reputation_history_select_own ON reputation_history FOR
SELECT USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS reputation_history_insert_staff ON reputation_history;

CREATE POLICY reputation_history_insert_staff ON reputation_history FOR INSERT
WITH
    CHECK (is_staff ());

-- ========================================
-- RLS Policies for student_schedules table
-- ========================================
DROP POLICY IF EXISTS student_schedules_select_own ON student_schedules;

CREATE POLICY student_schedules_select_own ON student_schedules FOR
SELECT USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS student_schedules_insert_own ON student_schedules;

CREATE POLICY student_schedules_insert_own ON student_schedules FOR INSERT
WITH
    CHECK (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS student_schedules_update_own ON student_schedules;

CREATE POLICY student_schedules_update_own ON student_schedules
FOR UPDATE
    USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS student_schedules_delete_own ON student_schedules;

CREATE POLICY student_schedules_delete_own ON student_schedules FOR DELETE USING (
    user_id = get_current_user_id ()
    OR is_staff ()
);

-- ========================================
-- RLS Policies for activity_logs table
-- ========================================
DROP POLICY IF EXISTS activity_logs_select_own ON activity_logs;

CREATE POLICY activity_logs_select_own ON activity_logs FOR
SELECT USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS activity_logs_insert_any ON activity_logs;

CREATE POLICY activity_logs_insert_any ON activity_logs FOR INSERT
WITH
    CHECK (TRUE);
-- System can insert for any user

-- ========================================
-- RLS Policies for point_transactions table
-- ========================================
DROP POLICY IF EXISTS point_transactions_select_own ON point_transactions;

CREATE POLICY point_transactions_select_own ON point_transactions FOR
SELECT USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS point_transactions_insert_staff ON point_transactions;

CREATE POLICY point_transactions_insert_staff ON point_transactions FOR INSERT
WITH
    CHECK (is_staff ());

-- ========================================
-- RLS Policies for refresh_tokens table
-- ========================================
DROP POLICY IF EXISTS refresh_tokens_select_own ON refresh_tokens;

CREATE POLICY refresh_tokens_select_own ON refresh_tokens FOR
SELECT USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS refresh_tokens_insert_own ON refresh_tokens;

CREATE POLICY refresh_tokens_insert_own ON refresh_tokens FOR INSERT
WITH
    CHECK (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS refresh_tokens_update_own ON refresh_tokens;

CREATE POLICY refresh_tokens_update_own ON refresh_tokens
FOR UPDATE
    USING (
        user_id = get_current_user_id ()
        OR is_staff ()
    );

DROP POLICY IF EXISTS refresh_tokens_delete_own ON refresh_tokens;

CREATE POLICY refresh_tokens_delete_own ON refresh_tokens FOR DELETE USING (
    user_id = get_current_user_id ()
    OR is_staff ()
);

-- ========================================
-- Note: Public tables (areas, zones, seats, news, categories, library_settings)
-- don't need RLS as they are readable by everyone
-- ========================================