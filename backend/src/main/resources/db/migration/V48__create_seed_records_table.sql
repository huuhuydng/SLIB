CREATE TABLE IF NOT EXISTS seed_records (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(60) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    seed_scope VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_seed_records_entity UNIQUE (entity_type, entity_id)
);

CREATE INDEX IF NOT EXISTS idx_seed_records_scope ON seed_records (seed_scope);
CREATE INDEX IF NOT EXISTS idx_seed_records_entity_type ON seed_records (entity_type);
