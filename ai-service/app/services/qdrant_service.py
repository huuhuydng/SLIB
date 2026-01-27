"""
Qdrant Vector Database Service
Handles vector storage and retrieval using Qdrant (v1.16+ API)
"""

import logging
import uuid
from typing import List, Dict, Any, Optional
from datetime import datetime

from qdrant_client import QdrantClient
from qdrant_client.http import models
from qdrant_client.http.models import Distance, VectorParams, PointStruct

from app.config.settings import get_settings

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class QdrantService:
    """
    Service for managing vectors in Qdrant database (v1.16+ compatible)
    
    Provides:
    - Collection management (create, delete)
    - Vector upsert (insert/update)
    - Similarity search (using query_points)
    - Source management (delete by source)
    """
    
    def __init__(self):
        settings = get_settings()
        
        # Parse Qdrant URL
        url = settings.qdrant_url
        self.collection_name = settings.qdrant_collection
        
        # Initialize Qdrant client
        self.client = QdrantClient(url=url, timeout=30)
        
        logger.info(f"[QdrantService] Connecting to Qdrant at {url}")
        logger.info(f"[QdrantService] Using collection: {self.collection_name}")
        
        # Ensure collection exists
        self._ensure_collection()
    
    def _ensure_collection(self, vector_size: int = 768):
        """
        Create collection if it doesn't exist
        Default vector size 768 for nomic-embed-text
        """
        try:
            collections = self.client.get_collections()
            collection_names = [c.name for c in collections.collections]
            
            if self.collection_name not in collection_names:
                self.client.create_collection(
                    collection_name=self.collection_name,
                    vectors_config=VectorParams(
                        size=vector_size,
                        distance=Distance.COSINE
                    )
                )
                logger.info(f"[QdrantService] Created collection: {self.collection_name}")
            else:
                logger.info(f"[QdrantService] Collection exists: {self.collection_name}")
                
        except Exception as e:
            logger.error(f"[QdrantService] Error ensuring collection: {e}")
            raise
    
    def upsert_vectors(
        self,
        vectors: List[List[float]],
        contents: List[str],
        source: str,
        category: str = "general",
        metadata: Optional[Dict[str, Any]] = None
    ) -> int:
        """
        Insert or update vectors in Qdrant
        
        Args:
            vectors: List of embedding vectors
            contents: List of text content for each vector
            source: Source identifier
            category: Category for filtering
            metadata: Additional metadata
            
        Returns:
            Number of vectors upserted
        """
        try:
            points = []
            for i, (vector, content) in enumerate(zip(vectors, contents)):
                point_id = str(uuid.uuid4())
                payload = {
                    "content": content,
                    "source": source,
                    "category": category,
                    "chunk_index": i,
                    "created_at": datetime.now().isoformat(),
                    **(metadata or {})
                }
                points.append(PointStruct(
                    id=point_id,
                    vector=vector,
                    payload=payload
                ))
            
            # Delete existing vectors from this source first
            self.delete_by_source(source)
            
            # Upsert new vectors
            self.client.upsert(
                collection_name=self.collection_name,
                points=points
            )
            
            logger.info(f"[QdrantService] Upserted {len(points)} vectors from source: {source}")
            return len(points)
            
        except Exception as e:
            logger.error(f"[QdrantService] Error upserting vectors: {e}")
            raise
    
    def search(
        self,
        query_vector: List[float],
        limit: int = 5,
        category: Optional[str] = None,
        score_threshold: float = 0.0
    ) -> List[Dict[str, Any]]:
        """
        Search for similar vectors using query_points (Qdrant v1.16+ API)
        
        Args:
            query_vector: Query embedding vector
            limit: Maximum number of results
            category: Optional category filter
            score_threshold: Minimum similarity score
            
        Returns:
            List of search results with content and metadata
        """
        try:
            # Build filter if category specified
            filter_conditions = None
            if category:
                filter_conditions = models.Filter(
                    must=[
                        models.FieldCondition(
                            key="category",
                            match=models.MatchValue(value=category)
                        )
                    ]
                )
            
            # Use query_points (new API in qdrant-client 1.16+)
            results = self.client.query_points(
                collection_name=self.collection_name,
                query=query_vector,
                limit=limit,
                query_filter=filter_conditions,
                score_threshold=score_threshold,
                with_payload=True
            )
            
            # Format results
            formatted_results = []
            for point in results.points:
                formatted_results.append({
                    "id": point.id,
                    "score": point.score,
                    "content": point.payload.get("content", "") if point.payload else "",
                    "source": point.payload.get("source", "") if point.payload else "",
                    "category": point.payload.get("category", "") if point.payload else "",
                    "chunk_index": point.payload.get("chunk_index", 0) if point.payload else 0,
                    "metadata": {k: v for k, v in (point.payload or {}).items() 
                               if k not in ["content", "source", "category", "chunk_index"]}
                })
            
            logger.info(f"[QdrantService] Search returned {len(formatted_results)} results")
            return formatted_results
            
        except Exception as e:
            logger.error(f"[QdrantService] Error searching: {e}")
            return []
    
    def delete_by_source(self, source: str) -> int:
        """
        Delete all vectors from a specific source
        
        Args:
            source: Source identifier to delete
            
        Returns:
            Number deleted (estimated)
        """
        try:
            self.client.delete(
                collection_name=self.collection_name,
                points_selector=models.FilterSelector(
                    filter=models.Filter(
                        must=[
                            models.FieldCondition(
                                key="source",
                                match=models.MatchValue(value=source)
                            )
                        ]
                    )
                )
            )
            logger.info(f"[QdrantService] Deleted vectors from source: {source}")
            return 1  # Qdrant doesn't return count
            
        except Exception as e:
            logger.error(f"[QdrantService] Error deleting by source: {e}")
            return 0
    
    def get_stats(self) -> Dict[str, Any]:
        """Get collection statistics"""
        try:
            info = self.client.get_collection(self.collection_name)
            points_count = info.points_count or 0
            return {
                "collection_name": self.collection_name,
                "points_count": points_count,
                "indexed_vectors_count": getattr(info, 'indexed_vectors_count', 0) or 0,
                "status": info.status.value if info.status else "unknown",
                "optimizer_status": str(info.optimizer_status) if info.optimizer_status else "ok",
                # Format for KnowledgeStatsResponse compatibility
                "total_chunks": points_count,
                "total_sources": 1,  # Qdrant doesn't track this directly
                "categories": [],
                "last_updated": None
            }
        except Exception as e:
            logger.error(f"[QdrantService] Error getting stats: {e}")
            return {
                "error": str(e),
                "total_chunks": 0,
                "total_sources": 0,
                "categories": [],
                "last_updated": None
            }
    
    def delete_collection(self) -> bool:
        """Delete the entire collection"""
        try:
            self.client.delete_collection(self.collection_name)
            logger.info(f"[QdrantService] Deleted collection: {self.collection_name}")
            return True
        except Exception as e:
            logger.error(f"[QdrantService] Error deleting collection: {e}")
            return False


# Singleton instance
_qdrant_service = None


def get_qdrant_service() -> QdrantService:
    """Get singleton QdrantService instance"""
    global _qdrant_service
    if _qdrant_service is None:
        _qdrant_service = QdrantService()
    return _qdrant_service
