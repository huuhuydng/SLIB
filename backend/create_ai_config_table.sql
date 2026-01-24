-- Create AI Config table for SLIB system
-- This table stores AI chatbot configuration (Gemini API)

CREATE TABLE IF NOT EXISTS ai_config (
    id BIGSERIAL PRIMARY KEY,
    api_key VARCHAR(500) NOT NULL,
    model VARCHAR(50) NOT NULL DEFAULT 'gemini-1.5-pro',
    temperature DOUBLE PRECISION NOT NULL DEFAULT 0.7,
    max_tokens INTEGER NOT NULL DEFAULT 1024,
    system_prompt TEXT DEFAULT 'SLIB AI Assistant',
    enable_context BOOLEAN NOT NULL DEFAULT TRUE,
    enable_history BOOLEAN NOT NULL DEFAULT TRUE,
    response_language VARCHAR(10) DEFAULT 'vi',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);
