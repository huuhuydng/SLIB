ALTER TABLE new_books
    ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_new_books_active_pinned_arrival
    ON new_books (is_active, is_pinned DESC, arrival_date DESC, created_at DESC);
