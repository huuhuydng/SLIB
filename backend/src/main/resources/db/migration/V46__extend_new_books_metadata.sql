ALTER TABLE new_books
    ADD COLUMN IF NOT EXISTS source_url TEXT,
    ADD COLUMN IF NOT EXISTS publisher VARCHAR(255),
    ALTER COLUMN category TYPE VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_new_books_active_arrival
    ON new_books (is_active, arrival_date DESC, created_at DESC);
