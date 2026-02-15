-- Migration: Add human_session_id support for chat architecture refactor
-- This allows tracking multiple librarian conversation rounds within a single session

-- Add currentHumanSession to conversations table
ALTER TABLE conversations
ADD COLUMN IF NOT EXISTS current_human_session INTEGER DEFAULT 0;

-- Add humanSessionId to messages table (nullable - NULL means bot conversation)
ALTER TABLE messages
ADD COLUMN IF NOT EXISTS human_session_id INTEGER DEFAULT NULL;

-- Add index for faster message queries by session and human_session_id
CREATE INDEX IF NOT EXISTS idx_messages_session_human ON messages (
    conversation_id,
    human_session_id
);

-- Update existing messages: set human_session_id based on sender_type
-- LIBRARIAN messages from existing conversations should have human_session_id = 1
UPDATE messages
SET
    human_session_id = 1
WHERE
    sender_type = 'LIBRARIAN'
    AND human_session_id IS NULL;