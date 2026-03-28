"""
Knowledge Base Service
Fetches knowledge from Java backend and provides context for AI responses
"""

from typing import List, Dict
import logging

from app.services.java_backend_client import get_java_client

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class KnowledgeBaseService:
    """
    Knowledge Base Service - connects to Java backend for real data
    """
    
    def __init__(self):
        self.java_client = get_java_client()
    
    def build_knowledge_context(self) -> str:
        """Build knowledge context string for AI prompt from database"""
        knowledge = self.java_client.get_knowledge()
        
        if not knowledge:
            logger.info("[KnowledgeBaseService] No knowledge found in database")
            return ""
        
        context = "\n--- KIẾN THỨC THƯ VIỆN ---\n"
        for item in knowledge:
            if item.get("isActive", True):  # Only include active items
                title = item.get("title", "")
                content = item.get("content", "")
                item_type = item.get("type", "INFO")
                context += f"[{item_type}] {title}: {content}\n"
        context += "--- HẾT KIẾN THỨC ---\n\n"
        
        logger.info(f"[KnowledgeBaseService] Built context from {len(knowledge)} items")
        return context
    
    def get_all_knowledge(self) -> List[Dict]:
        """Get all knowledge items from database"""
        return self.java_client.get_knowledge()
    
    def refresh(self):
        """Force refresh knowledge from backend"""
        self.java_client.get_knowledge(force_refresh=True)


# Singleton instance
knowledge_base_service = KnowledgeBaseService()
