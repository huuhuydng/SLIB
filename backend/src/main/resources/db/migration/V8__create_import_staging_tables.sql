-- Staging table for bulk user import
-- Data is first inserted here, validated, then moved to users table

CREATE TABLE IF NOT EXISTS user_import_staging (
    id SERIAL PRIMARY KEY,
    batch_id UUID NOT NULL,
    row_number INTEGER,

-- User data columns
user_code VARCHAR(50),
email VARCHAR(100),
full_name VARCHAR(255),
phone VARCHAR(20),
dob DATE,
role VARCHAR(20) DEFAULT 'STUDENT',
avt_url TEXT,

-- Processing status
status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, VALID, INVALID, IMPORTED
error_message TEXT,

-- Timestamps
created_at TIMESTAMP DEFAULT NOW(), processed_at TIMESTAMP );

-- Index for batch operations
CREATE INDEX IF NOT EXISTS idx_staging_batch_id ON user_import_staging (batch_id);

CREATE INDEX IF NOT EXISTS idx_staging_status ON user_import_staging (batch_id, status);

-- Import job tracking table
CREATE TABLE IF NOT EXISTS import_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL UNIQUE,

-- Job info
file_name VARCHAR(255), total_rows INTEGER DEFAULT 0,

-- Progress counters
valid_count INTEGER DEFAULT 0,
invalid_count INTEGER DEFAULT 0,
imported_count INTEGER DEFAULT 0,
avatar_count INTEGER DEFAULT 0,
avatar_uploaded INTEGER DEFAULT 0,

-- Status
status VARCHAR(30) DEFAULT 'PENDING', -- PENDING, PARSING, VALIDATING, IMPORTING, ENRICHING, COMPLETED, FAILED
error_message TEXT,

-- Timestamps
created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_import_jobs_status ON import_jobs (status);