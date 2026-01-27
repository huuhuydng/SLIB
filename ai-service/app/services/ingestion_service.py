"""
Ingestion Service
Handles document upload, chunking, embedding, and storage in PostgreSQL
"""

import logging
import io
from typing import List, Dict, Any, Optional
from datetime import datetime

from langchain.text_splitter import RecursiveCharacterTextSplitter
from sqlalchemy.orm import Session
from sqlalchemy import text

from app.config.settings import get_settings
from app.services.embedding_service import get_embedding_service
from app.models.vector_models import LibraryVector
from app.core.database import SessionLocal

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class IngestionService:
    """
    Service for ingesting documents into the vector database
    
    Workflow:
    1. Load document (PDF, DOCX, or raw text)
    2. Split into chunks using RecursiveCharacterTextSplitter
    3. Generate embeddings using Ollama
    4. Store in PostgreSQL with pgvector
    """
    
    def __init__(self):
        settings = get_settings()
        
        # Text splitter configuration
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=settings.chunk_size,
            chunk_overlap=settings.chunk_overlap,
            length_function=len,
            separators=["\n\n", "\n", ". ", " ", ""]
        )
        
        self.embedding_service = get_embedding_service()
        
        logger.info(
            f"[IngestionService] Initialized with chunk_size={settings.chunk_size}, "
            f"overlap={settings.chunk_overlap}"
        )
    
    def ingest_text(
        self, 
        content: str, 
        source: str, 
        category: str = "general",
        metadata: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Ingest raw text content
        
        Args:
            content: Text content to ingest
            source: Source identifier (e.g., filename)
            category: Category for organizing content
            metadata: Additional metadata
            
        Returns:
            Dict with success status and chunk count
        """
        try:
            logger.info(f"[IngestionService] Ingesting text from source: {source}")
            
            # Split text into chunks
            chunks = self.text_splitter.split_text(content)
            logger.info(f"[IngestionService] Created {len(chunks)} chunks")
            
            if not chunks:
                return {
                    "success": False,
                    "message": "No content to ingest",
                    "chunks_created": 0
                }
            
            # Generate embeddings for all chunks
            embeddings = self.embedding_service.embed_texts(chunks)
            logger.info(f"[IngestionService] Generated {len(embeddings)} embeddings")
            
            # Store in database
            db = SessionLocal()
            try:
                # Delete existing vectors from this source (replace mode)
                db.execute(
                    text("DELETE FROM library_vectors WHERE source = :source"),
                    {"source": source}
                )
                
                # Insert new vectors
                for i, (chunk, embedding) in enumerate(zip(chunks, embeddings)):
                    # Use raw SQL for pgvector insertion
                    db.execute(
                        text("""
                            INSERT INTO library_vectors 
                            (content, embedding, source, category, chunk_index, metadata, created_at, updated_at)
                            VALUES (:content, :embedding, :source, :category, :chunk_index, :metadata, :created_at, :updated_at)
                        """),
                        {
                            "content": chunk,
                            "embedding": str(embedding),  # pgvector accepts string format
                            "source": source,
                            "category": category,
                            "chunk_index": i,
                            "metadata": str(metadata or {}),
                            "created_at": datetime.now(),
                            "updated_at": datetime.now()
                        }
                    )
                
                db.commit()
                logger.info(f"[IngestionService] Stored {len(chunks)} chunks in database")
                
                return {
                    "success": True,
                    "message": f"Successfully ingested {len(chunks)} chunks from {source}",
                    "chunks_created": len(chunks),
                    "source": source
                }
                
            finally:
                db.close()
                
        except Exception as e:
            logger.error(f"[IngestionService] Error ingesting text: {e}")
            return {
                "success": False,
                "message": f"Error: {str(e)}",
                "chunks_created": 0
            }
    
    def ingest_pdf(
        self, 
        file_content: bytes, 
        filename: str, 
        category: str = "document"
    ) -> Dict[str, Any]:
        """
        Ingest PDF file
        
        Args:
            file_content: PDF file bytes
            filename: Name of the file
            category: Category for the document
            
        Returns:
            Dict with success status and chunk count
        """
        try:
            from PyPDF2 import PdfReader
            
            logger.info(f"[IngestionService] Processing PDF: {filename}")
            
            # Read PDF
            reader = PdfReader(io.BytesIO(file_content))
            text_content = ""
            
            for page in reader.pages:
                text_content += page.extract_text() + "\n\n"
            
            if not text_content.strip():
                return {
                    "success": False,
                    "message": "Could not extract text from PDF",
                    "chunks_created": 0
                }
            
            # Use ingest_text for the rest
            return self.ingest_text(
                content=text_content,
                source=filename,
                category=category,
                metadata={"file_type": "pdf", "pages": len(reader.pages)}
            )
            
        except Exception as e:
            logger.error(f"[IngestionService] Error processing PDF: {e}")
            return {
                "success": False,
                "message": f"PDF processing error: {str(e)}",
                "chunks_created": 0
            }
    
    def ingest_docx(
        self, 
        file_content: bytes, 
        filename: str, 
        category: str = "document"
    ) -> Dict[str, Any]:
        """
        Ingest DOCX file
        
        Args:
            file_content: DOCX file bytes
            filename: Name of the file
            category: Category for the document
            
        Returns:
            Dict with success status and chunk count
        """
        try:
            from docx import Document
            
            logger.info(f"[IngestionService] Processing DOCX: {filename}")
            
            # Read DOCX
            doc = Document(io.BytesIO(file_content))
            text_content = ""
            
            for paragraph in doc.paragraphs:
                text_content += paragraph.text + "\n"
            
            # Also extract from tables
            for table in doc.tables:
                for row in table.rows:
                    for cell in row.cells:
                        text_content += cell.text + " "
                    text_content += "\n"
            
            if not text_content.strip():
                return {
                    "success": False,
                    "message": "Could not extract text from DOCX",
                    "chunks_created": 0
                }
            
            # Use ingest_text for the rest
            return self.ingest_text(
                content=text_content,
                source=filename,
                category=category,
                metadata={"file_type": "docx"}
            )
            
        except Exception as e:
            logger.error(f"[IngestionService] Error processing DOCX: {e}")
            return {
                "success": False,
                "message": f"DOCX processing error: {str(e)}",
                "chunks_created": 0
            }
    
    def get_stats(self) -> Dict[str, Any]:
        """Get statistics about the knowledge base"""
        db = SessionLocal()
        try:
            # Total chunks
            result = db.execute(text("SELECT COUNT(*) FROM library_vectors"))
            total_chunks = result.scalar() or 0
            
            # Unique sources
            result = db.execute(text("SELECT COUNT(DISTINCT source) FROM library_vectors"))
            total_sources = result.scalar() or 0
            
            # Categories
            result = db.execute(text("SELECT DISTINCT category FROM library_vectors WHERE category IS NOT NULL"))
            categories = [row[0] for row in result.fetchall()]
            
            # Last updated
            result = db.execute(text("SELECT MAX(updated_at) FROM library_vectors"))
            last_updated = result.scalar()
            
            return {
                "total_chunks": total_chunks,
                "total_sources": total_sources,
                "categories": categories,
                "last_updated": last_updated
            }
            
        finally:
            db.close()
    
    def delete_source(self, source: str) -> Dict[str, Any]:
        """Delete all vectors from a specific source"""
        db = SessionLocal()
        try:
            result = db.execute(
                text("DELETE FROM library_vectors WHERE source = :source RETURNING id"),
                {"source": source}
            )
            deleted_count = len(result.fetchall())
            db.commit()
            
            return {
                "success": True,
                "message": f"Deleted {deleted_count} chunks from source: {source}",
                "deleted_count": deleted_count
            }
            
        except Exception as e:
            logger.error(f"[IngestionService] Error deleting source: {e}")
            return {
                "success": False,
                "message": f"Error: {str(e)}"
            }
        finally:
            db.close()


# Singleton instance
_ingestion_service = None


def get_ingestion_service() -> IngestionService:
    """Get singleton IngestionService instance"""
    global _ingestion_service
    if _ingestion_service is None:
        _ingestion_service = IngestionService()
    return _ingestion_service
