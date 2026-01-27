-- SLIB AI Service - Database Initialization
-- Creates pgvector extension and library_vectors table for RAG

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create library_vectors table for storing document embeddings
CREATE TABLE IF NOT EXISTS library_vectors (
    id SERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    embedding vector (768), -- nomic-embed-text outputs 768 dimensions
    metadata JSONB DEFAULT '{}',
    source VARCHAR(500),
    category VARCHAR(100),
    chunk_index INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create index for vector similarity search using cosine distance
CREATE INDEX IF NOT EXISTS library_vectors_embedding_idx ON library_vectors USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- Create index for filtering by category and source
CREATE INDEX IF NOT EXISTS library_vectors_category_idx ON library_vectors (category);

CREATE INDEX IF NOT EXISTS library_vectors_source_idx ON library_vectors (source);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to auto-update updated_at
DROP TRIGGER IF EXISTS update_library_vectors_updated_at ON library_vectors;

CREATE TRIGGER update_library_vectors_updated_at
    BEFORE UPDATE ON library_vectors
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Log completion
DO $$
BEGIN
    RAISE NOTICE 'SLIB AI Database initialized successfully with pgvector!';
END $$;