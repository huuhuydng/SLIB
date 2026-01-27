"""
SQLModel definition for library_vectors table
Stores document chunks with their vector embeddings for RAG
"""

from typing import Optional, Dict, Any
from datetime import datetime
from sqlmodel import SQLModel, Field
from sqlalchemy import Column, Text
from pgvector.sqlalchemy import Vector


class LibraryVector(SQLModel, table=True):
    """
    Library Vector model for storing document embeddings
    
    Attributes:
        id: Primary key
        content: The text content of the chunk
        embedding: Vector embedding (768 dimensions for nomic-embed-text)
        metadata_: JSON metadata (source file info, etc.)
        source: Source file name or URL
        category: Category/type of document (e.g., "quy_dinh", "huong_dan")
        chunk_index: Index of this chunk within the source document
        created_at: When this record was created
        updated_at: When this record was last updated
    """
    __tablename__ = "library_vectors"
    
    id: Optional[int] = Field(default=None, primary_key=True)
    content: str = Field(sa_column=Column(Text, nullable=False))
    embedding: Any = Field(sa_column=Column(Vector(768)))  # nomic-embed-text = 768 dims
    metadata_: Optional[Dict] = Field(default=None, sa_column_name="metadata")
    source: Optional[str] = Field(default=None, max_length=500)
    category: Optional[str] = Field(default=None, max_length=100)
    chunk_index: int = Field(default=0)
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)
    
    class Config:
        arbitrary_types_allowed = True


class VectorSearchResult:
    """Result from vector similarity search"""
    
    def __init__(
        self, 
        id: int, 
        content: str, 
        similarity_score: float,
        source: Optional[str] = None,
        category: Optional[str] = None,
        metadata: Optional[Dict] = None
    ):
        self.id = id
        self.content = content
        self.similarity_score = similarity_score
        self.source = source
        self.category = category
        self.metadata = metadata or {}
    
    def __repr__(self):
        return f"VectorSearchResult(id={self.id}, score={self.similarity_score:.4f})"
