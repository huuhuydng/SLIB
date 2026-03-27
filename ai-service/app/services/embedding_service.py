"""
Embedding Service
Handles vector embeddings using Ollama's nomic-embed-text model
"""

import logging
from typing import List
from langchain_community.embeddings import OllamaEmbeddings

from app.config.settings import get_settings

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class EmbeddingService:
    """
    Service for generating text embeddings using Ollama
    Uses nomic-embed-text model (768 dimensions)
    """
    
    def __init__(self):
        settings = get_settings()
        self.ollama_url = settings.ollama_url
        self.model = settings.ollama_embedding_model
        
        # Initialize LangChain Ollama embeddings
        self.embeddings = OllamaEmbeddings(
            model=self.model,
            base_url=self.ollama_url
        )
        
        logger.info(f"[EmbeddingService] Initialized with model={self.model}, url={self.ollama_url}")
    
    def embed_text(self, text: str) -> List[float]:
        """
        Generate embedding for a single text
        
        Args:
            text: Text to embed
            
        Returns:
            List of floats (768 dimensions for nomic-embed-text)
        """
        try:
            embedding = self.embeddings.embed_query(text)
            logger.debug(f"[EmbeddingService] Generated embedding with {len(embedding)} dimensions")
            return embedding
        except Exception as e:
            logger.error(f"[EmbeddingService] Error generating embedding: {e}")
            raise
    
    def embed_texts(self, texts: List[str]) -> List[List[float]]:
        """
        Generate embeddings for multiple texts
        
        Args:
            texts: List of texts to embed
            
        Returns:
            List of embeddings (each is 768 dimensions)
        """
        try:
            embeddings = self.embeddings.embed_documents(texts)
            logger.info(f"[EmbeddingService] Generated {len(embeddings)} embeddings")
            return embeddings
        except Exception as e:
            logger.error(f"[EmbeddingService] Error generating embeddings: {e}")
            raise
    
    def test_connection(self) -> dict:
        """Test connection to Ollama embedding service"""
        try:
            # Try to embed a simple text
            test_text = "Test connection"
            embedding = self.embed_text(test_text)
            
            return {
                "success": True,
                "message": f"Embedding service connected. Model: {self.model}",
                "dimensions": len(embedding)
            }
        except Exception as e:
            return {
                "success": False,
                "message": f"Embedding service error: {str(e)}"
            }


# Singleton instance
_embedding_service = None


def get_embedding_service() -> EmbeddingService:
    """Get singleton EmbeddingService instance"""
    global _embedding_service
    if _embedding_service is None:
        _embedding_service = EmbeddingService()
    return _embedding_service
