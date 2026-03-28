-- V7: Add user authentication fields for username/password login
-- This migration adds support for traditional email/password authentication

-- ========================================
-- Rename student_code to user_code
-- ========================================
ALTER TABLE users RENAME COLUMN student_code TO user_code;

-- ========================================
-- Add new authentication and profile fields
-- ========================================
-- Username for login (can be email or custom username)
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(50);

-- Password hash (BCrypt encoded)
ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255);

-- Date of birth
ALTER TABLE users ADD COLUMN IF NOT EXISTS dob DATE;

-- Phone number
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20);

-- Avatar URL
ALTER TABLE users ADD COLUMN IF NOT EXISTS avt_url TEXT;

-- Flag to check if user has changed default password
ALTER TABLE users
ADD COLUMN IF NOT EXISTS password_changed BOOLEAN DEFAULT FALSE;

-- ========================================
-- Set default username from user_code for existing users
-- ========================================
UPDATE users SET username = user_code WHERE username IS NULL;

-- ========================================
-- Add unique constraint on username
-- ========================================
DO $$
BEGIN
    ALTER TABLE users ADD CONSTRAINT users_username_unique UNIQUE (username);
EXCEPTION WHEN duplicate_object THEN
    NULL;
END $$;

-- ========================================
-- Add index for faster username lookup
-- ========================================
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);