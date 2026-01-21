"""
Pydantic models for AI Service
Equivalent to Java DTOs/Entities
"""

from pydantic import BaseModel, Field
from typing import Optional, List, Dict
from enum import Enum


class AIConfig(BaseModel):
    """AI Configuration - equivalent to AIConfigEntity"""
    api_key: str = Field(default="", description="Gemini API Key")
    model: str = Field(default="gemini-2.0-flash", description="Gemini model name")
    temperature: float = Field(default=0.7, ge=0.0, le=2.0)
    max_tokens: int = Field(default=1024, ge=1, le=8192)
    system_prompt: str = Field(
        default="Bạn là SLIB AI Assistant - trợ lý thông minh của hệ thống Thư viện thông minh SLIB. "
                "Hãy trả lời ngắn gọn, thân thiện và chính xác bằng tiếng Việt."
    )
    enable_context: bool = Field(default=True, description="Use knowledge base context")
    enable_history: bool = Field(default=True, description="Use chat history")
    response_language: str = Field(default="vi")


class ChatMessage(BaseModel):
    """Single chat message"""
    role: str = Field(..., description="'user' or 'assistant'")
    content: str = Field(..., description="Message content")


class GenerateRequest(BaseModel):
    """Request for generate_response endpoint"""
    user_message: str = Field(..., description="User's question")
    chat_history: Optional[List[ChatMessage]] = Field(default=[], description="Previous messages")
    config: Optional[AIConfig] = Field(default=None, description="Override default config")


class GeminiResponse(BaseModel):
    """AI Response - equivalent to Java GeminiResponse class"""
    content: str = Field(..., description="AI generated response")
    confidence_score: float = Field(default=0.9, ge=0.0, le=1.0)
    needs_review: bool = Field(default=False, description="Needs librarian review")


class TestConnectionResponse(BaseModel):
    """Response for test_connection endpoint"""
    success: bool
    message: str
    model: Optional[str] = None


class ChatRequest(BaseModel):
    """Simple chat request"""
    message: str
    session_id: Optional[str] = None


class ChatResponse(BaseModel):
    """Chat response"""
    success: bool
    reply: str
    session_id: str
    confidence_score: float
    needs_review: bool
