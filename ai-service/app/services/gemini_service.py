"""
Gemini AI Service
Equivalent to Java GeminiService - handles all Gemini API interactions
"""

import httpx
import json
import logging
from typing import List, Optional, Dict, Any

from app.config.settings import get_settings
from app.models.schemas import AIConfig, GeminiResponse, ChatMessage
from app.services.knowledge_base import knowledge_base_service

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class GeminiService:
    """
    Service to interact with Google Gemini AI API
    Equivalent to Java GeminiService class
    """
    
    GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    UNCERTAINTY_MARKER = "[KHÔNG CHẮC CHẮN]"
    
    def __init__(self, config: Optional[AIConfig] = None):
        """Initialize GeminiService with optional config override"""
        settings = get_settings()
        
        self.api_key = config.api_key if config and config.api_key else settings.gemini_api_key
        self.model = config.model if config and config.model else settings.gemini_model
        self.temperature = config.temperature if config else settings.default_temperature
        self.max_tokens = config.max_tokens if config else settings.default_max_tokens
        self.system_prompt = config.system_prompt if config else settings.default_system_prompt
        self.enable_context = config.enable_context if config else settings.enable_context
        self.enable_history = config.enable_history if config else settings.enable_history
        
        # HTTP client with timeout
        self.client = httpx.Client(timeout=30.0)
    
    def generate_response(
        self, 
        user_message: str, 
        chat_history: Optional[List[ChatMessage]] = None
    ) -> GeminiResponse:
        """
        Generate response from Gemini AI
        
        Args:
            user_message: The user's question
            chat_history: Previous messages for context
            
        Returns:
            GeminiResponse with content, confidence_score, needs_review
        """
        # Validate API key
        if not self.api_key:
            logger.error("API key is not configured")
            return GeminiResponse(
                content="Lỗi: Chưa cấu hình API key cho AI.",
                confidence_score=0.0,
                needs_review=True
            )
        
        # Build full prompt
        full_prompt = self._build_full_prompt(user_message, chat_history)
        
        try:
            # Build request body
            request_body = self._build_request_body(full_prompt)
            
            # Build full URL
            url = f"{self.GEMINI_API_URL}{self.model}:generateContent?key={self.api_key}"
            
            logger.info(f"[GeminiService] Calling Gemini API with model: {self.model}")
            
            # Make HTTP request
            response = self.client.post(
                url,
                json=request_body,
                headers={"Content-Type": "application/json"}
            )
            
            # Parse response
            return self._parse_gemini_response(response)
            
        except httpx.HTTPStatusError as e:
            logger.error(f"HTTP error: {e.response.status_code} - {e.response.text}")
            return GeminiResponse(
                content="Xin lỗi, tôi đang gặp sự cố kỹ thuật. Vui lòng thử lại sau.",
                confidence_score=0.0,
                needs_review=True
            )
        except Exception as e:
            logger.error(f"Unexpected error: {str(e)}")
            return GeminiResponse(
                content="Xin lỗi, tôi đang gặp sự cố kỹ thuật. Vui lòng thử lại sau hoặc liên hệ thủ thư để được hỗ trợ.",
                confidence_score=0.0,
                needs_review=True
            )
    
    def test_connection(self) -> Dict[str, Any]:
        """
        Test API connection
        
        Returns:
            Dict with success status and message
        """
        logger.info("[GeminiService] Testing connection...")
        logger.info(f"[GeminiService] API Key present: {bool(self.api_key)}")
        
        if not self.api_key:
            logger.error("[GeminiService] ERROR: API key is null or empty")
            return {
                "success": False,
                "message": "API key chưa được cấu hình",
                "model": None
            }
        
        logger.info(f"[GeminiService] API Key length: {len(self.api_key)}")
        logger.info(f"[GeminiService] API Key preview: {self.api_key[:10]}...")
        logger.info(f"[GeminiService] Using model: {self.model}")
        
        try:
            # Simple test request
            request_body = {
                "contents": [
                    {"parts": [{"text": "Hello, respond with just 'OK'"}]}
                ]
            }
            
            url = f"{self.GEMINI_API_URL}{self.model}:generateContent?key={self.api_key}"
            logger.info(f"[GeminiService] Request URL: {self.GEMINI_API_URL}{self.model}:generateContent?key=***")
            
            response = self.client.post(
                url,
                json=request_body,
                headers={"Content-Type": "application/json"}
            )
            
            response.raise_for_status()
            data = response.json()
            
            # Check for error in response
            if "error" in data:
                logger.error(f"[GeminiService] API returned error: {data['error']}")
                return {
                    "success": False,
                    "message": f"Lỗi API: {data['error'].get('message', 'Unknown error')}",
                    "model": self.model
                }
            
            logger.info(f"[GeminiService] Response received successfully")
            logger.info(f"[GeminiService] Connection test result: True")
            
            return {
                "success": True,
                "message": "Kết nối thành công với Gemini API",
                "model": self.model
            }
            
        except httpx.HTTPStatusError as e:
            logger.error(f"[GeminiService] HTTP error: {e.response.status_code}")
            return {
                "success": False,
                "message": f"Lỗi HTTP {e.response.status_code}: {e.response.text[:200]}",
                "model": self.model
            }
        except Exception as e:
            logger.error(f"[GeminiService] ERROR: Connection test failed with exception: {str(e)}")
            return {
                "success": False,
                "message": f"Lỗi kết nối: {str(e)}",
                "model": self.model
            }
    
    def _build_full_prompt(
        self, 
        user_message: str, 
        chat_history: Optional[List[ChatMessage]] = None
    ) -> str:
        """Build complete prompt with system prompt, knowledge, and history"""
        prompt_parts = []
        
        # System prompt
        prompt_parts.append(self.system_prompt)
        prompt_parts.append("")
        
        # Knowledge context (if enabled)
        if self.enable_context:
            knowledge_context = knowledge_base_service.build_knowledge_context()
            prompt_parts.append(knowledge_context)
        
        # Chat history (if enabled)
        if self.enable_history and chat_history:
            prompt_parts.append("\n--- LỊCH SỬ HỘI THOẠI ---")
            for msg in chat_history:
                role = "Sinh viên" if msg.role == "user" else "AI"
                prompt_parts.append(f"{role}: {msg.content}")
            prompt_parts.append("--- HẾT LỊCH SỬ ---\n")
        
        # User's current question
        prompt_parts.append(f"Sinh viên hỏi: {user_message}")
        prompt_parts.append("")
        prompt_parts.append(
            "Hãy trả lời câu hỏi trên. Nếu câu hỏi không liên quan đến thư viện "
            f"hoặc bạn không chắc chắn, hãy bắt đầu câu trả lời bằng '{self.UNCERTAINTY_MARKER}'."
        )
        
        return "\n".join(prompt_parts)
    
    def _build_request_body(self, prompt: str) -> Dict[str, Any]:
        """Build Gemini API request body"""
        return {
            "contents": [
                {"parts": [{"text": prompt}]}
            ],
            "generationConfig": {
                "temperature": self.temperature,
                "maxOutputTokens": self.max_tokens
            }
        }
    
    def _parse_gemini_response(self, response: httpx.Response) -> GeminiResponse:
        """Parse Gemini API response using proper JSON parsing"""
        try:
            response.raise_for_status()
            data = response.json()
            
            # Check for error
            if "error" in data:
                error_msg = data["error"].get("message", "Unknown error")
                logger.error(f"Gemini API error: {error_msg}")
                return GeminiResponse(
                    content=f"Lỗi khi gọi AI: {error_msg}",
                    confidence_score=0.0,
                    needs_review=True
                )
            
            # Extract text from response
            # Response format: {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
            candidates = data.get("candidates", [])
            if not candidates:
                return GeminiResponse(
                    content="Không nhận được phản hồi từ AI.",
                    confidence_score=0.0,
                    needs_review=True
                )
            
            content = candidates[0].get("content", {})
            parts = content.get("parts", [])
            if not parts:
                return GeminiResponse(
                    content="Không nhận được phản hồi từ AI.",
                    confidence_score=0.0,
                    needs_review=True
                )
            
            text = parts[0].get("text", "")
            
            # Check if AI is uncertain
            needs_review = self.UNCERTAINTY_MARKER in text
            confidence_score = 0.3 if needs_review else 0.9
            
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


# Singleton instance for dependency injection
def get_gemini_service(config: Optional[AIConfig] = None) -> GeminiService:
    """Factory function to create GeminiService instance"""
    return GeminiService(config)
