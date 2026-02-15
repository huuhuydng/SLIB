"""
Pydantic models for AI Service
Request/Response schemas for all endpoints
"""

from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from enum import Enum
from datetime import datetime


# ============== ENUMS ==============

class ActionType(str, Enum):
    """Response action types"""
    NONE = "NONE"
    SHOW_ESCALATION_BUTTON = "SHOW_ESCALATION_BUTTON"  # User wants to talk to librarian - show button
    ESCALATE_TO_LIBRARIAN = "ESCALATE_TO_LIBRARIAN"    # Low confidence - suggest escalation


class DocumentType(str, Enum):
    """Supported document types for ingestion"""
    PDF = "pdf"
    DOCX = "docx"
    TEXT = "text"


# ============== CONFIG ==============

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


# ============== CHAT ==============

class ChatMessage(BaseModel):
    """Single chat message"""
    role: str = Field(..., description="'user' or 'assistant'")
    content: str = Field(..., description="Message content")


class ChatRequest(BaseModel):
    """Simple chat request"""
    message: str
    session_id: Optional[str] = None
    conversation_id: Optional[str] = None  # For tracking AI-to-Human escalation
    student_id: Optional[str] = None  # Student UUID for backend integration


class ChatResponse(BaseModel):
    """Chat response with RAG information"""
    success: bool
    reply: str
    session_id: str
    confidence_score: float
    needs_review: bool
    escalated: bool = False
    escalation_message: Optional[str] = None
    action: ActionType = ActionType.NONE
    sources: Optional[List[str]] = None  # Sources used for the answer


class RAGQueryRequest(BaseModel):
    """Request for RAG chat query endpoint"""
    message: str = Field(..., description="User's question")
    session_id: Optional[str] = None
    include_sources: bool = Field(default=True, description="Include source documents in response")


class RAGQueryResponse(BaseModel):
    """Response from RAG chat query"""
    success: bool
    reply: str
    action: ActionType = ActionType.NONE
    similarity_score: float = Field(..., description="Best similarity score from retrieval")
    sources: List[Dict[str, Any]] = Field(default=[], description="Retrieved source chunks")
    session_id: Optional[str] = None


class DebugChunk(BaseModel):
    """Debug info for a retrieved chunk"""
    rank: int = Field(..., description="Rank position (1-based)")
    score: float = Field(..., description="Similarity score")
    content: str = Field(..., description="Chunk content (truncated)")
    source: str = Field(..., description="Source name")
    category: str = Field(default="", description="Category")


class DebugRAGResponse(BaseModel):
    """Debug response with detailed RAG processing info"""
    success: bool
    reply: str
    action: ActionType = ActionType.NONE
    session_id: Optional[str] = None
    
    # Debug info
    debug: Dict[str, Any] = Field(default_factory=dict, description="Debug information")
    # Structure:
    # {
    #   "query_analysis": {
    #     "original": str,
    #     "normalized": str,
    #     "is_greeting": bool,
    #     "greeting_pattern": str | null
    #   },
    #   "retrieval": {
    #     "threshold": float,
    #     "best_score": float,
    #     "passed_threshold": bool,
    #     "chunks_found": int,
    #     "chunks_used": int,
    #     "chunks": [DebugChunk]
    #   },
    #   "generation": {
    #     "used_llm": bool,
    #     "llm_returned_idk": bool,
    #     "action_reason": str
    #   }
    # }


# ============== INGESTION ==============

class IngestTextRequest(BaseModel):
    """Request to ingest raw text"""
    content: str = Field(..., description="Text content to ingest")
    source: str = Field(..., description="Source name/identifier")
    category: str = Field(default="general", description="Category for the document")
    metadata: Optional[Dict[str, Any]] = Field(default=None)


class IngestResponse(BaseModel):
    """Response from ingestion endpoint"""
    success: bool
    message: str
    chunks_created: int = 0
    source: Optional[str] = None


class KnowledgeStatsResponse(BaseModel):
    """Statistics about the knowledge base"""
    total_chunks: int
    total_sources: int
    categories: List[str]
    last_updated: Optional[datetime] = None


# ============== LEGACY (for backward compatibility) ==============

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
  # Message shown when escalated

