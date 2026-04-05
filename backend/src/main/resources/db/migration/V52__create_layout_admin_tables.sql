CREATE TABLE IF NOT EXISTS layout_drafts (
    draft_id BIGSERIAL PRIMARY KEY,
    snapshot_json TEXT NOT NULL,
    based_on_published_version BIGINT,
    updated_by_user_id UUID,
    updated_by_name VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS layout_history (
    history_id BIGSERIAL PRIMARY KEY,
    action_type VARCHAR(32) NOT NULL,
    summary TEXT,
    snapshot_json TEXT NOT NULL,
    published_version BIGINT,
    created_by_user_id UUID,
    created_by_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_layout_history_created_at ON layout_history(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_layout_history_action_type ON layout_history(action_type);
