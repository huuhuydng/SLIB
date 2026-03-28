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
    DebugRAGResponse,
    ChatRequest,
    ChatResponse,
    ActionType
)
from app.services.chat_service import get_rag_chat_service
from app.services.escalation_service import escalation_service
from app.services.mongo_service import get_mongo_service

# Configure logging
logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/chat", tags=["Chat"])

# Note: Session storage moved to MongoDB (see mongo_service.py)


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
        
        # Save to MongoDB (replaces in-memory storage)
        mongo_service = get_mongo_service()
        mongo_service.save_message(session_id, "user", request.message)
        mongo_service.save_message(session_id, "assistant", result["reply"], action=result["action"].value if result["action"] else None)
        
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


@router.post("/debug", response_model=DebugRAGResponse)
async def rag_debug_query(request: RAGQueryRequest):
    """
    RAG Debug Query Endpoint - For Admin Testing
    
    Returns detailed debug information about the RAG processing:
    - Query analysis (normalization, greeting detection)
    - Retrieval info (threshold, scores, chunks)
    - Generation details (LLM usage, action reason)
    
    Example request:
    ```json
    {
        "message": "Bị trừ bao nhiêu điểm nếu không check-in?",
        "session_id": "optional-session-id"
    }
    ```
    """
    try:
        session_id = request.session_id or str(uuid.uuid4())
        
        # Get RAG service and process with debug
        rag_service = get_rag_chat_service()
        result = rag_service.query_with_debug(request.message)
        
        # Save to MongoDB with debug info
        mongo_service = get_mongo_service()
        mongo_service.save_message(session_id, "user", request.message)
        mongo_service.save_message(
            session_id, "assistant", result["reply"],
            debug=result["debug"],
            action=result["action"].value if result["action"] else None
        )
        
        return DebugRAGResponse(
            success=result["success"],
            reply=result["reply"],
            action=result["action"],
            session_id=session_id,
            debug=result["debug"]
        )
        
    except Exception as e:
        logger.error(f"[ChatRouter] Error processing debug query: {e}")
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
        
        # Load recent chat history for context
        mongo_service = get_mongo_service()
        raw_history = mongo_service.get_session_history(session_id, limit=6)
        chat_history = [
            {"role": msg.get("role", "user"), "content": msg.get("content", "")}
            for msg in raw_history
        ] if raw_history else None

        # Save user message BEFORE query (so next call sees it in history)
        mongo_service.save_message(session_id, "user", request.message)

        # Use RAG service for query with history
        rag_service = get_rag_chat_service()
        result = rag_service.query(request.message, chat_history=chat_history)

        # Save AI reply
        mongo_service.save_message(session_id, "assistant", result["reply"])

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
    """Clear chat session history from MongoDB"""
    mongo_service = get_mongo_service()
    deleted = mongo_service.clear_session(session_id)
    return {"success": True, "message": f"Đã xóa {deleted} tin nhắn", "deleted_count": deleted}


@router.get("/history/{session_id}")
async def get_chat_history(session_id: str, limit: int = 50):
    """
    Get chat history for a session from MongoDB
    Returns messages ordered by timestamp
    """
    mongo_service = get_mongo_service()
    messages = mongo_service.get_session_history(session_id, limit)
    
    # Format for frontend
    formatted = []
    for msg in messages:
        formatted.append({
            "role": msg.get("role"),
            "content": msg.get("content"),
            "timestamp": msg.get("created_at").isoformat() if msg.get("created_at") else None,
            "debug": msg.get("debug"),
            "action": msg.get("action")
        })
    
    return {
        "success": True,
        "session_id": session_id,
        "messages": formatted,
        "count": len(formatted)
    }


@router.get("/stats")
async def get_chat_stats():
    """Get chat storage statistics"""
    mongo_service = get_mongo_service()
    return mongo_service.get_stats()
