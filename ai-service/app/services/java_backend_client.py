"""
Java Backend Client Service
Fetches AI config, knowledge base, and prompts from Java backend (port 8080)
"""

import httpx
import logging
from typing import Dict, List, Optional, Any
from app.config.settings import get_settings

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class JavaBackendClient:
    """
    Client to fetch AI configuration from Java Spring Boot backend
    """
    
    def __init__(self):
        settings = get_settings()
        self.base_url = settings.java_backend_url.rstrip("/")
        self.admin_url = f"{self.base_url}/ai/admin"
        self.client = httpx.Client(timeout=10.0)
        self._cached_config: Optional[Dict] = None
        self._cached_knowledge: Optional[List[Dict]] = None
        self._cached_prompts: Optional[List[Dict]] = None
        self._cached_library_settings: Optional[Dict] = None
    
    def get_ai_config(self, force_refresh: bool = False) -> Dict[str, Any]:
        """
        Get AI configuration from Java backend
        
        Returns:
            Dict with provider, model, temperature, systemPrompt, etc.
        """
        if self._cached_config and not force_refresh:
            return self._cached_config
        
        try:
            logger.info("[JavaBackendClient] Fetching AI config...")
            response = self.client.get(f"{self.admin_url}/config")
            response.raise_for_status()
            data = response.json()
            
            if data.get("configured") and data.get("config"):
                self._cached_config = data["config"]
                logger.info(f"[JavaBackendClient] Loaded config: provider={self._cached_config.get('provider')}")
                return self._cached_config
            else:
                # Return defaults if not configured
                logger.info("[JavaBackendClient] No config found, using defaults")
                return self._get_default_config()
                
        except Exception as e:
            logger.error(f"[JavaBackendClient] Error fetching config: {e}")
            return self._get_default_config()
    
    def get_knowledge(self, force_refresh: bool = False) -> List[Dict]:
        """
        Get knowledge base from Java backend
        
        Returns:
            List of knowledge items [{title, content, type}, ...]
        """
        if self._cached_knowledge and not force_refresh:
            return self._cached_knowledge
        
        try:
            logger.info("[JavaBackendClient] Fetching knowledge base...")
            response = self.client.get(f"{self.admin_url}/knowledge")
            response.raise_for_status()
            
            self._cached_knowledge = response.json()
            logger.info(f"[JavaBackendClient] Loaded {len(self._cached_knowledge)} knowledge items")
            return self._cached_knowledge
            
        except Exception as e:
            logger.error(f"[JavaBackendClient] Error fetching knowledge: {e}")
            return []
    
    def get_prompts(self, force_refresh: bool = False) -> List[Dict]:
        """
        Get prompt templates from Java backend
        
        Returns:
            List of prompts [{name, prompt, context, isActive}, ...]
        """
        if self._cached_prompts and not force_refresh:
            return self._cached_prompts
        
        try:
            logger.info("[JavaBackendClient] Fetching prompts...")
            response = self.client.get(f"{self.admin_url}/prompts")
            response.raise_for_status()
            
            self._cached_prompts = response.json()
            logger.info(f"[JavaBackendClient] Loaded {len(self._cached_prompts)} prompts")
            return self._cached_prompts
            
        except Exception as e:
            logger.error(f"[JavaBackendClient] Error fetching prompts: {e}")
            return []

    def get_library_settings(self, force_refresh: bool = False) -> Dict[str, Any]:
        """
        Get public library settings from Java backend.

        Returns:
            Dict with openTime, closeTime, workingDays, libraryClosed, etc.
        """
        if self._cached_library_settings and not force_refresh:
            return self._cached_library_settings

        try:
            logger.info("[JavaBackendClient] Fetching library settings...")
            response = self.client.get(f"{self.base_url}/settings/library")
            response.raise_for_status()

            self._cached_library_settings = response.json()
            return self._cached_library_settings
        except Exception as e:
            logger.error(f"[JavaBackendClient] Error fetching library settings: {e}")
            return {}
    
    def refresh_all(self):
        """Force refresh all cached data from backend"""
        logger.info("[JavaBackendClient] Refreshing all data from backend...")
        self.get_ai_config(force_refresh=True)
        self.get_knowledge(force_refresh=True)
        self.get_prompts(force_refresh=True)
        self.get_library_settings(force_refresh=True)
    
    def _get_default_config(self) -> Dict[str, Any]:
        """Return default configuration"""
        return {
            "provider": "ollama",
            "ollamaModel": "llama3.2",
            "ollamaUrl": "http://localhost:11434",
            "geminiModel": "gemini-2.0-flash",
            "temperature": 0.7,
            "maxTokens": 1024,
            "systemPrompt": "Bạn là SLIB AI Assistant - trợ lý thông minh của hệ thống Thư viện thông minh SLIB. Hãy trả lời ngắn gọn, thân thiện và chính xác bằng tiếng Việt.",
            "enableContext": True,
            "enableHistory": True,
            "autoSuggest": True,
            "responseLanguage": "vi"
        }
    
    def __del__(self):
        if hasattr(self, 'client'):
            self.client.close()


# Singleton instance
_java_client: Optional[JavaBackendClient] = None


def get_java_client() -> JavaBackendClient:
    """Get singleton JavaBackendClient instance"""
    global _java_client
    if _java_client is None:
        _java_client = JavaBackendClient()
    return _java_client
