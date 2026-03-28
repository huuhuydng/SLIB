-- V40__drop_unused_tables.sql
-- Xoa cac bang khong su dung: chat_logs, reputation_history, student_schedules
-- chat_logs: Da thay the boi conversations + messages
-- reputation_history: Da thay the boi point_transactions + reputation_rules
-- student_schedules: Khong co entity, khong co repository, khong duoc su dung

-- Drop RLS policies truoc khi drop tables
DROP POLICY IF EXISTS chat_logs_select_own ON chat_logs;
DROP POLICY IF EXISTS chat_logs_insert_own ON chat_logs;

DROP POLICY IF EXISTS reputation_history_select_own ON reputation_history;
DROP POLICY IF EXISTS reputation_history_insert_staff ON reputation_history;

DROP POLICY IF EXISTS student_schedules_select_own ON student_schedules;
DROP POLICY IF EXISTS student_schedules_insert_own ON student_schedules;
DROP POLICY IF EXISTS student_schedules_update_own ON student_schedules;
DROP POLICY IF EXISTS student_schedules_delete_own ON student_schedules;

-- Drop tables
DROP TABLE IF EXISTS chat_logs CASCADE;
DROP TABLE IF EXISTS reputation_history CASCADE;
DROP TABLE IF EXISTS student_schedules CASCADE;
