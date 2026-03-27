-- Migration: Add human_session_id support for chat architecture refactor
-- This allows tracking multiple librarian conversation rounds within a single session
-- NOTE: Tables are created by V15_5 first

-- Add currentHumanSession to conversations table (if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'conversations') THEN
        ALTER TABLE conversations
        ADD COLUMN IF NOT EXISTS current_human_session INTEGER DEFAULT 0;
    END IF;
END $$;

-- Add humanSessionId to messages table (if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'messages') THEN
        ALTER TABLE messages
        ADD COLUMN IF NOT EXISTS human_session_id INTEGER DEFAULT NULL;
    END IF;
END $$;

-- Add index for faster message queries by session and human_session_id (if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'messages') THEN
        CREATE INDEX IF NOT EXISTS idx_messages_session_human ON messages (
            conversation_id,
            human_session_id
        );
    END IF;
END $$;

-- Update existing messages: set human_session_id based on sender_type
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'messages') THEN
        UPDATE messages
        SET
            human_session_id = 1
        WHERE
            sender_type = 'LIBRARIAN'
            AND human_session_id IS NULL;
    END IF;
END $$;
