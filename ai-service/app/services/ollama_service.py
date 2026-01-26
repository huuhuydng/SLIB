"""
Ollama AI Service
Uses local Ollama server with configuration from Java backend
"""

import httpx
import json
import logging
from typing import List, Optional, Dict, Any

from app.models.schemas import GeminiResponse, ChatMessage
from app.services.knowledge_base import knowledge_base_service
from app.services.java_backend_client import get_java_client

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class OllamaService:
    """
    Service to interact with local Ollama server
    Configuration loaded from Java backend database
    """
    
    UNCERTAINTY_MARKER = "[KHÔNG CHẮC CHẮN]"
    
    def __init__(self):
        """Initialize OllamaService with config from Java backend"""
        self.java_client = get_java_client()
        self._load_config()
        
        # HTTP client with longer timeout for local LLM
        self.client = httpx.Client(timeout=120.0)
    
    def _load_config(self):
        """Load configuration from Java backend"""
        config = self.java_client.get_ai_config()
        
        self.ollama_url = config.get("ollamaUrl", "http://localhost:11434")
        self.model = config.get("ollamaModel", "llama3.2")
        self.temperature = config.get("temperature", 0.7)
        self.max_tokens = config.get("maxTokens", 1024)
        self.system_prompt = config.get("systemPrompt", 
            "Bạn là SLIB AI Assistant - trợ lý thông minh của hệ thống Thư viện thông minh SLIB. "
            "Hãy trả lời ngắn gọn, thân thiện và chính xác bằng tiếng Việt."
        )
        self.enable_context = config.get("enableContext", True)
        self.enable_history = config.get("enableHistory", True)
        
        logger.info(f"[OllamaService] Loaded config: model={self.model}, url={self.ollama_url}")
    
    def refresh_config(self):
        """Refresh configuration from Java backend"""
        self.java_client.refresh_all()
        self._load_config()
    
    def generate_response(
        self, 
        user_message: str, 
        chat_history: Optional[List[ChatMessage]] = None
    ) -> GeminiResponse:
        """
        Generate response from Ollama
        
        Args:
            user_message: The user's question
            chat_history: Previous messages for context
            
        Returns:
            GeminiResponse with content, confidence_score, needs_review
        """
        # Refresh config to get latest settings
        self._load_config()
        
        # Build full prompt
        full_prompt = self._build_full_prompt(user_message, chat_history)
        
        try:
            # Build request body for Ollama
            request_body = {
                "model": self.model,
                "prompt": full_prompt,
                "stream": False,
                "options": {
                    "temperature": self.temperature,
                    "num_predict": self.max_tokens
                }
            }
            
            url = f"{self.ollama_url}/api/generate"
            
            logger.info(f"[OllamaService] Calling Ollama: model={self.model}, url={self.ollama_url}")
            
            # Make HTTP request
            response = self.client.post(
                url,
                json=request_body,
                headers={"Content-Type": "application/json"}
            )
            
            # Parse response
            return self._parse_ollama_response(response)
            
        except httpx.ConnectError as e:
            logger.error(f"Connection error: Ollama không đang chạy? {str(e)}")
            return GeminiResponse(
                content=f"Lỗi: Không thể kết nối Ollama tại {self.ollama_url}. Hãy chạy 'ollama serve' trước.",
                confidence_score=0.0,
                needs_review=True
            )
        except Exception as e:
            logger.error(f"Unexpected error: {str(e)}")
            return GeminiResponse(
                content="Xin lỗi, tôi đang gặp sự cố kỹ thuật. Vui lòng thử lại sau.",
                confidence_score=0.0,
                needs_review=True
            )
    
    def test_connection(self) -> Dict[str, Any]:
        """Test Ollama connection"""
        # Refresh config first
        self._load_config()
        
        logger.info(f"[OllamaService] Testing connection to {self.ollama_url}...")
        
        try:
            # Simple test - list models
            response = self.client.get(f"{self.ollama_url}/api/tags")
            response.raise_for_status()
            data = response.json()
            
            models = [m["name"] for m in data.get("models", [])]
            
            if self.model not in [m.split(":")[0] for m in models] and f"{self.model}:latest" not in models:
                return {
                    "success": False,
                    "message": f"Model '{self.model}' chưa được tải. Chạy: ollama pull {self.model}",
                    "model": self.model,
                    "available_models": models
                }
            
            logger.info(f"[OllamaService] Connection test successful")
            return {
                "success": True,
                "message": f"Kết nối thành công với Ollama. Model: {self.model}",
                "model": self.model,
                "available_models": models
            }
            
        except httpx.ConnectError:
            logger.error("[OllamaService] Cannot connect to Ollama")
            return {
                "success": False,
                "message": f"Không thể kết nối Ollama tại {self.ollama_url}. Chạy: ollama serve",
                "model": None
            }
        except Exception as e:
            logger.error(f"[OllamaService] Error: {str(e)}")
            return {
                "success": False,
                "message": f"Lỗi: {str(e)}",
                "model": self.model
            }
    
    def _build_full_prompt(
        self, 
        user_message: str, 
        chat_history: Optional[List[ChatMessage]] = None
    ) -> str:
        """Build complete prompt with system prompt, knowledge, prompts, and history"""
        prompt_parts = []
        
        # System prompt from config
        prompt_parts.append(f"### Hướng dẫn:\n{self.system_prompt}")
        prompt_parts.append("")
        
        # Add prompt templates from database
        prompts = self.java_client.get_prompts()
        if prompts:
            active_prompts = [p for p in prompts if p.get("isActive", True)]
            if active_prompts:
                prompt_parts.append("### Hướng dẫn bổ sung:")
                for p in active_prompts:
                    prompt_parts.append(f"- [{p.get('context', 'GENERAL')}] {p.get('prompt', '')}")
                prompt_parts.append("")
        
        # Knowledge context from database (if enabled)
        if self.enable_context:
            knowledge_context = knowledge_base_service.build_knowledge_context()
            if knowledge_context:
                prompt_parts.append(knowledge_context)
        
        # Chat history (if enabled)
        if self.enable_history and chat_history:
            prompt_parts.append("\n### Lịch sử hội thoại:")
            for msg in chat_history[-6:]:  # Limit to last 6 messages
                role = "Sinh viên" if msg.role == "user" else "AI"
                prompt_parts.append(f"{role}: {msg.content}")
            prompt_parts.append("")
        
        # User's current question
        prompt_parts.append(f"### Câu hỏi:\nSinh viên hỏi: {user_message}")
        prompt_parts.append("")
        prompt_parts.append("### Trả lời:")
        prompt_parts.append(
            f"Hãy trả lời ngắn gọn bằng tiếng Việt. "
            f"Nếu không chắc chắn, bắt đầu bằng '{self.UNCERTAINTY_MARKER}'."
        )
        
        return "\n".join(prompt_parts)
    
    def _parse_ollama_response(self, response: httpx.Response) -> GeminiResponse:
        """Parse Ollama API response"""
        try:
            response.raise_for_status()
            data = response.json()
            
            text = data.get("response", "").strip()
            
            if not text:
                return GeminiResponse(
                    content="Không nhận được phản hồi từ AI.",
                    confidence_score=0.0,
                    needs_review=True
                )
            
            # Check if AI is uncertain
            needs_review = self.UNCERTAINTY_MARKER in text
            confidence_score = 0.3 if needs_review else 0.85
            
            # Remove uncertainty marker from response
            text = text.replace(self.UNCERTAINTY_MARKER, "").strip()
            
            return GeminiResponse(
                content=text,
                confidence_score=confidence_score,
                needs_review=needs_review
            )
            
        except json.JSONDecodeError as e:
            logger.error(f"JSON decode error: {str(e)}")
            return GeminiResponse(
                content="Lỗi xử lý phản hồi AI.",
                confidence_score=0.0,
                needs_review=True
            )
        except httpx.HTTPStatusError as e:
            logger.error(f"HTTP error: {e.response.status_code}")
            return GeminiResponse(
                content=f"Lỗi HTTP: {e.response.status_code}",
                confidence_score=0.0,
                needs_review=True
            )
    
    def __del__(self):
        """Cleanup HTTP client"""
        if hasattr(self, 'client'):
            self.client.close()


# Singleton instance
_ollama_service = None

def get_ollama_service() -> OllamaService:
    """Factory function to get OllamaService instance"""
    global _ollama_service
    if _ollama_service is None:
        _ollama_service = OllamaService()
    return _ollama_service
