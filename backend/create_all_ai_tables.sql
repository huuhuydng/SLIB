-- Create all AI-related tables for SLIB system
-- Execute this script to set up complete AI infrastructure

-- 1. Chat Sessions table
CREATE TABLE IF NOT EXISTS chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    title VARCHAR(200),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    closed_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_chat_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 2. Chat Messages table
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    needs_review BOOLEAN NOT NULL DEFAULT FALSE,
    confidence_score DOUBLE PRECISION,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
);

-- 3. Knowledge Base table
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

-- 4. Prompt Templates table
CREATE TABLE IF NOT EXISTS prompt_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    prompt TEXT NOT NULL,
    context VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_chat_sessions_user_id ON chat_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_sessions_status ON chat_sessions(status);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id ON chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_type ON knowledge_base(type);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_is_active ON knowledge_base(is_active);
CREATE INDEX IF NOT EXISTS idx_prompt_templates_context ON prompt_templates(context);
CREATE INDEX IF NOT EXISTS idx_prompt_templates_is_active ON prompt_templates(is_active);

-- Verify all tables exist
SELECT 'ai_config exists' WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ai_config');
SELECT 'chat_sessions exists' WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'chat_sessions');
SELECT 'chat_messages exists' WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'chat_messages');
SELECT 'knowledge_base exists' WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'knowledge_base');
SELECT 'prompt_templates exists' WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'prompt_templates');
