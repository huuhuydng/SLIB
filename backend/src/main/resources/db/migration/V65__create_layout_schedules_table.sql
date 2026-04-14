CREATE TABLE layout_schedules (
    schedule_id BIGSERIAL PRIMARY KEY,
    snapshot_json TEXT NOT NULL,
    based_on_published_version BIGINT NOT NULL,
    scheduled_for TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_error TEXT,
    requested_by_user_id UUID,
    requested_by_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    cancelled_at TIMESTAMP,
    executed_at TIMESTAMP
);

CREATE INDEX idx_layout_schedules_status_scheduled_for
    ON layout_schedules (status, scheduled_for);
