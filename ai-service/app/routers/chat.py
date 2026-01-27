"""
Chat Router
Handles RAG chat endpoints
"""

import uuid
import logging
from typing import Dict
from fastapi import APIRouter, HTTPException

from app.models.schemas import (
    RAGQueryRequest,
    RAGQueryResponse,
    ChatRequest,
    ChatResponse,
    ActionType
)
from app.services.chat_service import get_rag_chat_service
from app.services.escalation_service import escalation_service

# Configure logging
logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/chat", tags=["Chat"])

# In-memory session storage (use Redis in production)
chat_sessions: Dict[str, list] = {}


@router.post("/query", response_model=RAGQueryResponse)
async def rag_query(request: RAGQueryRequest):
    """
    RAG Chat Query Endpoint
    
    Process:
    1. Generate embedding for query
    2. Search vector database for relevant chunks
    3. Check similarity threshold (>= 0.75)
    4. Generate response with LLM if threshold met
    5. Return ESCALATE_TO_LIBRARIAN if not
    
    Example request:
    ```json
    {
        "message": "Thư viện mở cửa lúc mấy giờ?",
        "session_id": "optional-session-id"
    }
    ```
    """
    try:
        session_id = request.session_id or str(uuid.uuid4())
        
        # Get RAG service and process query
        rag_service = get_rag_chat_service()
        result = rag_service.query(request.message)
        
        # Save to session history
        if session_id not in chat_sessions:
            chat_sessions[session_id] = []
        chat_sessions[session_id].append({
            "role": "user",
            "content": request.message
        })
        chat_sessions[session_id].append({
            "role": "assistant",
            "content": result["reply"]
        })
        
        # Limit history size
        if len(chat_sessions[session_id]) > 20:
            chat_sessions[session_id] = chat_sessions[session_id][-20:]
        
        return RAGQueryResponse(
            success=result["success"],
            reply=result["reply"],
            action=result["action"],
            similarity_score=result["similarity_score"],
            sources=result["sources"] if request.include_sources else [],
            session_id=session_id
        )
        
    except Exception as e:
        logger.error(f"[ChatRouter] Error processing RAG query: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """
    Simple chat endpoint (backward compatible)
    Uses RAG under the hood
    
    Example request:
    ```json
    {
        "message": "Thư viện mở cửa lúc mấy giờ?",
        "session_id": "optional-session-id"
    }
    ```
    """
    try:
        session_id = request.session_id or str(uuid.uuid4())
        
        # Check for explicit escalation request first
        from app.services.escalation_service import escalation_service
        should_escalate, escalation_reason = escalation_service.should_escalate(
            request.message, ""
        )
        
        if should_escalate:
            # User explicitly wants to talk to human
            escalation_message = (
                "Tôi sẽ chuyển bạn đến thủ thư ngay. "
                "Vui lòng chờ trong giây lát, thủ thư sẽ tiếp nhận và hỗ trợ bạn! 👋"
            )
            
            # Call backend to escalate if we have conversation/student info
            if request.conversation_id or request.student_id:
                await escalation_service.escalate_conversation(
                    conversation_id=request.conversation_id,
                    student_id=request.student_id,
                    reason=escalation_reason
                )
            
            return ChatResponse(
                success=True,
                reply=escalation_message,
                session_id=session_id,
                confidence_score=1.0,
                needs_review=False,
                escalated=True,
                escalation_message=escalation_reason,
                action=ActionType.ESCALATE_TO_LIBRARIAN
            )
        
        # Use RAG service for query
        rag_service = get_rag_chat_service()
        result = rag_service.query(request.message)
        
        # Determine if needs review based on action
        needs_review = result["action"] == ActionType.ESCALATE_TO_LIBRARIAN
        
        # Add escalation hint if needed
        reply = result["reply"]
        if needs_review:
            reply += (
                "\n\n💡 *Nếu bạn cần hỗ trợ thêm, hãy nói 'cho em gặp thủ thư' "
                "để được kết nối với nhân viên thư viện.*"
            )
        
        # Calculate confidence score based on similarity
        confidence_score = min(result["similarity_score"], 1.0)
        if needs_review:
            confidence_score = max(0.3, confidence_score)
        
        return ChatResponse(
            success=True,
            reply=reply,
            session_id=session_id,
            confidence_score=confidence_score,
            needs_review=needs_review,
            escalated=False,
            action=result["action"],
            sources=[s["source"] for s in result.get("sources", [])]
        )
        
    except Exception as e:
        logger.error(f"[ChatRouter] Error processing chat: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/test")
async def test_rag_service():
    """Test RAG service connection"""
    rag_service = get_rag_chat_service()
    return rag_service.test_connection()


@router.delete("/session/{session_id}")
async def clear_session(session_id: str):
    """Clear chat session history"""
    if session_id in chat_sessions:
        del chat_sessions[session_id]
        return {"success": True, "message": f"Session {session_id} cleared"}
    return {"success": False, "message": "Session not found"}
