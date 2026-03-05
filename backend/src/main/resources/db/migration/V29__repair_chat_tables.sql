-- =============================================
-- V29: Repair chat tables schema to match JPA entities (UUID based)
-- =============================================

DO $
DECLARE
    conversations_needs_rebuild BOOLEAN := FALSE;
    messages_needs_rebuild BOOLEAN := FALSE;
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'conversations') THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'conversations'
              AND column_name = 'id'
              AND udt_name <> 'uuid'
        ) OR EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'conversations'
              AND column_name = 'user_id'
        ) OR NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'conversations'
              AND column_name = 'student_id'
        ) THEN
            conversations_needs_rebuild := TRUE;
        END IF;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'messages') THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'messages'
              AND column_name = 'id'
              AND udt_name <> 'uuid'
        ) OR EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'messages'
              AND column_name = 'conversation_id'
              AND udt_name <> 'uuid'
        ) OR NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'messages'
              AND column_name = 'receiver_id'
        ) OR NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'messages'
              AND column_name = 'message_type'
        ) THEN
            messages_needs_rebuild := TRUE;
        END IF;
    END IF;

    IF messages_needs_rebuild OR conversations_needs_rebuild THEN
        DROP TABLE IF EXISTS messages CASCADE;
        DROP TABLE IF EXISTS conversations CASCADE;
    END IF;
END $;

CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    librarian_id UUID REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AI_HANDLING',
    escalation_reason TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    escalated_at TIMESTAMP,
    resolved_at TIMESTAMP,
    current_human_session INTEGER DEFAULT 0,
    ai_session_id VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT,
    attachment_url TEXT,
    message_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    sender_type VARCHAR(20),
    human_session_id INTEGER
);

CREATE INDEX IF NOT EXISTS idx_conversations_student_id ON conversations(student_id);
CREATE INDEX IF NOT EXISTS idx_conversations_status ON conversations(status);
CREATE INDEX IF NOT EXISTS idx_conversations_librarian_id ON conversations(librarian_id);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_session_human ON messages(conversation_id, human_session_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender_receiver ON messages(sender_id, receiver_id);

DROP TRIGGER IF EXISTS update_conversations_updated_at ON conversations;
CREATE TRIGGER update_conversations_updated_at
    BEFORE UPDATE ON conversations
    FOR EACH ROW
    EXECUTE PROCEDURE update_updated_at_column();
