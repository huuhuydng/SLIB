ALTER TABLE reservations
ADD COLUMN IF NOT EXISTS layout_changed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE reservations
ADD COLUMN IF NOT EXISTS layout_change_title VARCHAR(200);

ALTER TABLE reservations
ADD COLUMN IF NOT EXISTS layout_change_message TEXT;

ALTER TABLE reservations
ADD COLUMN IF NOT EXISTS layout_changed_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_reservations_layout_changed
ON reservations (layout_changed, start_time)
WHERE layout_changed = TRUE;
