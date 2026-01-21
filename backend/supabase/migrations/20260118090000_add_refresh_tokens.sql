-- ========================================
-- Migration: Add refresh_tokens table
-- ========================================

-- Create refresh_tokens table for JWT token management
CREATE TABLE IF NOT EXISTS "public"."refresh_tokens" (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "user_id" UUID NOT NULL REFERENCES "public"."users"("id") ON DELETE CASCADE,
    "token_hash" VARCHAR(255) NOT NULL UNIQUE,
    "expires_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "revoked" BOOLEAN DEFAULT FALSE NOT NULL,
    "device_info" VARCHAR(255),
    "created_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    "updated_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for faster lookup by user_id
CREATE INDEX IF NOT EXISTS "idx_refresh_tokens_user_id" ON "public"."refresh_tokens"("user_id");

-- Index for token lookup
CREATE INDEX IF NOT EXISTS "idx_refresh_tokens_token_hash" ON "public"."refresh_tokens"("token_hash");

-- Comment
COMMENT ON TABLE "public"."refresh_tokens" IS 'Stores refresh tokens for JWT authentication';
COMMENT ON COLUMN "public"."refresh_tokens"."token_hash" IS 'SHA-256 hash of the refresh token';
COMMENT ON COLUMN "public"."refresh_tokens"."revoked" IS 'Whether the token has been revoked (logout)';
COMMENT ON COLUMN "public"."refresh_tokens"."device_info" IS 'Optional device/browser info for tracking';
