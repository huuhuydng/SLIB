# MongoDB Service for Chat Message Storage
# Features:
# - Save/load chat messages by session_id
# - TTL index for auto-deletion after 30 days
# - Connection pooling for performance

import os
import logging
import re
from datetime import datetime, timezone
from typing import List, Dict, Any, Optional
from urllib.parse import quote_plus
from pymongo import MongoClient, ASCENDING
from pymongo.errors import ConnectionFailure, OperationFailure
from app.core.env_loader import load_project_env

load_project_env()

logger = logging.getLogger(__name__)

# TTL: 30 days in seconds
CHAT_MESSAGE_TTL_SECONDS = 30 * 24 * 60 * 60

# Singleton instance
_mongo_service = None


def _fix_mongo_uri(uri: str) -> str:
    """URL-encode username/password in MongoDB URI per RFC 3986"""
    # Match mongodb://user:pass@host pattern
    match = re.match(r'^(mongodb(?:\+srv)?://)([^:]+):([^@]+)@(.+)$', uri)
    if match:
        scheme, user, password, rest = match.groups()
        return f"{scheme}{quote_plus(user)}:{quote_plus(password)}@{rest}"
    return uri


class MongoService:
    """
    MongoDB service for storing chat messages with TTL auto-expiration.
    
    Collections:
    - chat_messages: stores all chat messages
      - session_id: string (indexed)
      - role: "user" | "assistant"
      - content: string
      - timestamp: datetime (TTL indexed)
      - debug: optional dict for debug info
    """
    
    def __init__(self):
        self.client = None
        self.db = None
        self.connected = False
        
        mongo_url = os.getenv("MONGODB_URL", "mongodb://localhost:27017/slib_chat")
        mongo_url = _fix_mongo_uri(mongo_url)
        
        try:
            self.client = MongoClient(mongo_url, serverSelectionTimeoutMS=5000)
            # Test connection
            self.client.admin.command('ping')
            
            # Get database name from URL or default
            db_name = mongo_url.split("/")[-1].split("?")[0] or "slib_chat"
            self.db = self.client[db_name]
            
            # Ensure indexes
            self._ensure_indexes()
            
            self.connected = True
            logger.info(f"[MongoService] Connected to MongoDB: {db_name}")
            
        except (ConnectionFailure, OperationFailure) as e:
            logger.warning(f"[MongoService] Failed to connect to MongoDB: {e}")
            self.connected = False
    
    def _ensure_indexes(self):
        """Create indexes for chat_messages collection"""
        if self.db is None:
            return
            
        collection = self.db.chat_messages
        
        # Index for fast session lookup
        collection.create_index([("session_id", ASCENDING)])
        
        # TTL index for auto-deletion after 30 days
        # MongoDB will automatically delete documents where 'created_at' is older than TTL
        collection.create_index(
            [("created_at", ASCENDING)],
            expireAfterSeconds=CHAT_MESSAGE_TTL_SECONDS,
            name="chat_ttl_index"
        )
        
        logger.info(f"[MongoService] Indexes created with TTL={CHAT_MESSAGE_TTL_SECONDS}s (30 days)")
    
    def save_message(self, session_id: str, role: str, content: str, 
                     debug: Optional[Dict[str, Any]] = None,
                     action: Optional[str] = None) -> bool:
        """Save a chat message to MongoDB"""
        if not self.connected or self.db is None:
            logger.warning("[MongoService] Not connected, skipping save")
            return False
        
        try:
            doc = {
                "session_id": session_id,
                "role": role,
                "content": content,
                "created_at": datetime.now(timezone.utc),
            }
            
            if debug:
                doc["debug"] = debug
            if action:
                doc["action"] = action
                
            self.db.chat_messages.insert_one(doc)
            logger.debug(f"[MongoService] Saved message for session {session_id[:8]}...")
            return True
            
        except Exception as e:
            logger.error(f"[MongoService] Failed to save message: {e}")
            return False
    
    def get_session_history(self, session_id: str, limit: int = 50) -> List[Dict[str, Any]]:
        """Get chat history for a session, ordered by timestamp"""
        if not self.connected or self.db is None:
            return []
        
        try:
            cursor = self.db.chat_messages.find(
                {"session_id": session_id},
                {"_id": 0, "session_id": 0}  # Exclude internal fields
            ).sort("created_at", ASCENDING).limit(limit)
            
            return list(cursor)
            
        except Exception as e:
            logger.error(f"[MongoService] Failed to get history: {e}")
            return []
    
    def clear_session(self, session_id: str) -> int:
        """Clear all messages for a session, returns count deleted"""
        if not self.connected or self.db is None:
            return 0
        
        try:
            result = self.db.chat_messages.delete_many({"session_id": session_id})
            logger.info(f"[MongoService] Cleared {result.deleted_count} messages for session {session_id[:8]}...")
            return result.deleted_count
            
        except Exception as e:
            logger.error(f"[MongoService] Failed to clear session: {e}")
            return 0
    
    def get_stats(self) -> Dict[str, Any]:
        """Get statistics about chat storage"""
        if not self.connected or self.db is None:
            return {"connected": False}
        
        try:
            total_messages = self.db.chat_messages.count_documents({})
            unique_sessions = len(self.db.chat_messages.distinct("session_id"))
            
            return {
                "connected": True,
                "total_messages": total_messages,
                "unique_sessions": unique_sessions,
                "ttl_days": CHAT_MESSAGE_TTL_SECONDS // (24 * 60 * 60)
            }
        except Exception as e:
            logger.error(f"[MongoService] Failed to get stats: {e}")
            return {"connected": True, "error": str(e)}
    
    def test_connection(self) -> bool:
        """Test MongoDB connection"""
        if not self.client:
            return False
        try:
            self.client.admin.command('ping')
            return True
        except:
            return False


def get_mongo_service() -> MongoService:
    """Get singleton MongoService instance"""
    global _mongo_service
    if _mongo_service is None:
        _mongo_service = MongoService()
    return _mongo_service
