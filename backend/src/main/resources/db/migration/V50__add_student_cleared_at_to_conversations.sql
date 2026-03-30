ALTER TABLE conversations
ADD COLUMN IF NOT EXISTS student_cleared_at TIMESTAMP;
