"""
Ollama AI Service
Uses local Ollama server with Llama3.2 model - no rate limits!
"""

import httpx
import json
import logging
from typing import List, Optional, Dict, Any

from app.config.settings import get_settings
from app.models.schemas import GeminiResponse, ChatMessage
from app.services.knowledge_base import knowledge_base_service

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class OllamaService:
    """
    Service to interact with local Ollama server
    No API key needed, no rate limits!
    """
    
    OLLAMA_API_URL = "http://localhost:11434/api"
    UNCERTAINTY_MARKER = "[KHÔNG CHẮC CHẮN]"
    
    def __init__(self, model: str = "llama3.2"):
        """Initialize OllamaService with model name"""
        settings = get_settings()
        
        self.model = model
        self.temperature = settings.default_temperature
        self.system_prompt = settings.default_system_prompt
        self.enable_context = settings.enable_context
        self.enable_history = settings.enable_history
        
        # HTTP client with longer timeout for local LLM
        self.client = httpx.Client(timeout=120.0)
    
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
        # Build full prompt
        full_prompt = self._build_full_prompt(user_message, chat_history)
        
        try:
            # Build request body for Ollama
            request_body = {
                "model": self.model,
                "prompt": full_prompt,
                "stream": False,
                "options": {
                    "temperature": self.temperature
                }
            }
            
            url = f"{self.OLLAMA_API_URL}/generate"
            
            logger.info(f"[OllamaService] Calling Ollama with model: {self.model}")
            
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
                content="Lỗi: Không thể kết nối Ollama. Hãy chạy 'ollama serve' trước.",
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
        """
        Test Ollama connection
        
        Returns:
            Dict with success status and message
        """
        logger.info("[OllamaService] Testing connection...")
        
        try:
            # Simple test - list models
            response = self.client.get(f"{self.OLLAMA_API_URL}/tags")
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
                "message": "Không thể kết nối Ollama. Chạy: ollama serve",
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
        """Build complete prompt with system prompt, knowledge, and history"""
        prompt_parts = []
        
        # System prompt (Vietnamese-focused for Llama)
        prompt_parts.append(f"### Hướng dẫn:\n{self.system_prompt}")
        prompt_parts.append("")
        
        # Knowledge context (if enabled)
        if self.enable_context:
            knowledge_context = knowledge_base_service.build_knowledge_context()
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

def get_ollama_service(model: str = "llama3.2") -> OllamaService:
    """Factory function to get OllamaService instance"""
    global _ollama_service
    if _ollama_service is None:
        _ollama_service = OllamaService(model)
    return _ollama_service
