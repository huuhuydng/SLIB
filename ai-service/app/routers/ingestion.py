"""
Ingestion Router
Handles document upload and knowledge base management endpoints
"""

import logging
from fastapi import APIRouter, HTTPException, UploadFile, File, Form
from typing import Optional

from app.models.schemas import (
    IngestTextRequest,
    IngestResponse,
    KnowledgeStatsResponse
)
from app.services.ingestion_service import get_ingestion_service

# Configure logging
logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/ingest", tags=["Ingestion"])


@router.post("/upload", response_model=IngestResponse)
async def upload_document(
    file: UploadFile = File(...),
    category: str = Form(default="document"),
    source: Optional[str] = Form(default=None)
):
    """
    Upload and ingest a document (PDF or DOCX)
    
    Args:
        file: PDF or DOCX file
        category: Category for organizing (e.g., "quy_dinh", "huong_dan")
        source: Optional custom source name (defaults to filename)
    
    Example:
    ```bash
    curl -X POST http://localhost:8001/api/v1/ingest/upload \\
        -F "file=@library_rules.pdf" \\
        -F "category=quy_dinh"
    ```
    """
    try:
        ingestion_service = get_ingestion_service()
        
        # Determine file type
        filename = file.filename or "unknown"
        source_name = source or filename
        file_extension = filename.lower().split(".")[-1]
        
        # Read file content
        content = await file.read()
        
        if file_extension == "pdf":
            result = ingestion_service.ingest_pdf(content, source_name, category)
        elif file_extension in ["docx", "doc"]:
            result = ingestion_service.ingest_docx(content, source_name, category)
        else:
            # Try as raw text
            text_content = content.decode("utf-8", errors="ignore")
            result = ingestion_service.ingest_text(text_content, source_name, category)
        
        return IngestResponse(
            success=result["success"],
            message=result["message"],
            chunks_created=result.get("chunks_created", 0),
            source=result.get("source")
        )
        
    except Exception as e:
        logger.error(f"[IngestionRouter] Error uploading document: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/text", response_model=IngestResponse)
async def ingest_text(request: IngestTextRequest):
    """
    Ingest raw text content
    
    Example request:
    ```json
    {
        "content": "Thư viện mở cửa từ 7h sáng đến 22h hàng ngày...",
        "source": "gio_mo_cua",
        "category": "thong_tin_chung"
    }
    ```
    """
    try:
        ingestion_service = get_ingestion_service()
        
        result = ingestion_service.ingest_text(
            content=request.content,
            source=request.source,
            category=request.category,
            metadata=request.metadata
        )
        
        return IngestResponse(
            success=result["success"],
            message=result["message"],
            chunks_created=result.get("chunks_created", 0),
            source=result.get("source")
        )
        
    except Exception as e:
        logger.error(f"[IngestionRouter] Error ingesting text: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/stats", response_model=KnowledgeStatsResponse)
async def get_knowledge_stats():
    """
    Get statistics about the knowledge base
    
    Returns:
        total_chunks: Number of vector chunks
        total_sources: Number of unique sources
        categories: List of categories
        last_updated: Last update timestamp
    """
    try:
        ingestion_service = get_ingestion_service()
        stats = ingestion_service.get_stats()
        
        return KnowledgeStatsResponse(
            total_chunks=stats["total_chunks"],
            total_sources=stats["total_sources"],
            categories=stats["categories"],
            last_updated=stats["last_updated"]
        )
        
    except Exception as e:
        logger.error(f"[IngestionRouter] Error getting stats: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/source/{source}")
async def delete_source(source: str):
    """
    Delete all vectors from a specific source
    
    Args:
        source: Source identifier to delete
    """
    try:
        ingestion_service = get_ingestion_service()
        result = ingestion_service.delete_source(source)
        
        return result
        
    except Exception as e:
        logger.error(f"[IngestionRouter] Error deleting source: {e}")
        raise HTTPException(status_code=500, detail=str(e))
