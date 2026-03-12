"""
SLIB AI Service - FastAPI Application
Main entry point for the AI microservice

Features:
- RAG Chat with Qdrant vector search
- Document ingestion (PDF, DOCX, Text)
- Strict guardrails with I_DO_NOT_KNOW detection
- Human handoff (ESCALATE_TO_LIBRARIAN)
- Ollama LLM & Embeddings (self-hosted)
"""

import uuid
import logging
from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from typing import Dict

from app.models.schemas import (
    GenerateRequest, 
    GeminiResponse, 
    TestConnectionResponse,
    ChatRequest,
    ChatResponse,
    AIConfig,
    ActionType
)
from app.routers import chat, ingestion, analytics
from app.services.java_backend_client import get_java_client
from app.services.knowledge_base import knowledge_base_service
from app.services.analytics_service import analytics_ai_service
from app.config.settings import get_settings

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan events"""
    # Startup
    settings = get_settings()
    print("=" * 60)
    print("🤖 SLIB AI Service Starting...")
    print(f"🔌 AI Provider: OLLAMA (RAG Mode)")
    print(f"📦 LLM Model: {settings.ollama_model}")
    print(f"🔢 Embedding Model: {settings.ollama_embedding_model}")
    print(f"🏠 Ollama URL: {settings.ollama_url}")
    print(f"🗄️  Vector DB: Qdrant at {settings.qdrant_url}")
    print(f"📊 Similarity Threshold: {settings.similarity_threshold}")
    print(f"🌐 Debug mode: {settings.debug}")
    print("")
    print("📊 Available AI Features:")
    print("   • RAG Chat with Vector Search")
    print("   • Document Ingestion (PDF, DOCX, Text)")
    print("   • Human Handoff (ESCALATE_TO_LIBRARIAN)")
    print("   • Peak Hours Analysis")
    print("   • Time Slot Recommendations")
    print("   • Usage Statistics")
    print("=" * 60)
    
    # Initialize database (optional - tables created by init_db.sql)
    try:
        from app.core.database import check_db_connection
        if check_db_connection():
            logger.info("✅ Database connection verified")
        else:
            logger.warning("⚠️ Database connection failed - some features may not work")
    except Exception as e:
        logger.warning(f"⚠️ Database check skipped: {e}")
    
    yield
    
    # Shutdown
    print("🛑 SLIB AI Service shutting down...")


# Create FastAPI app
app = FastAPI(
    title="SLIB AI Service",
    description="AI Assistant service for SLIB Smart Library - RAG Mode with Qdrant",
    version="2.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(chat.router)
app.include_router(ingestion.router)
app.include_router(analytics.router)

# In-memory session storage (use Redis in production)
chat_sessions: Dict[str, list] = {}


# ============== HEALTH CHECK ==============

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy", "service": "slib-ai-service", "version": "2.0.0"}


# ============== AI CONFIG ENDPOINTS ==============

@app.get("/api/ai/config")
async def get_config():
    """Get current AI configuration"""
    settings = get_settings()
    java_client = get_java_client()
    
    try:
        config = java_client.get_ai_config(force_refresh=True)
        return {
            "configured": True,
            "provider": "ollama",
            "model": settings.ollama_model,
            "embedding_model": settings.ollama_embedding_model,
            "temperature": config.get("temperature", 0.7),
            "max_tokens": config.get("maxTokens", 1024),
            "enable_context": True,
            "enable_history": True,
            "system_prompt": settings.default_system_prompt,
            "similarity_threshold": settings.similarity_threshold,
            "rag_enabled": True
        }
    except Exception as e:
        logger.error(f"Error getting config: {e}")
        return {
            "configured": True,
            "provider": "ollama",
            "model": settings.ollama_model,
            "rag_enabled": True
        }


@app.post("/api/ai/refresh")
async def refresh_config():
    """Force refresh AI configuration"""
    java_client = get_java_client()
    java_client.refresh_all()
    
    return {
        "success": True,
        "message": "Đã refresh cấu hình AI từ database"
    }


@app.post("/api/ai/test-connection", response_model=TestConnectionResponse)
async def test_api_connection():
    """Test AI API connection (RAG service)"""
    try:
        from app.services.chat_service import get_rag_chat_service
        rag_service = get_rag_chat_service()
        result = rag_service.test_connection()
        
        return TestConnectionResponse(
            success=result["success"],
            message=result["message"],
            model=get_settings().ollama_model
        )
    except Exception as e:
        return TestConnectionResponse(
            success=False,
            message=f"Lỗi: {str(e)}",
            model=None
        )


# ============== LEGACY CHAT ENDPOINT (Backward Compatibility) ==============

@app.post("/api/ai/chat", response_model=ChatResponse)
async def legacy_chat(request: ChatRequest):
    """
    Legacy chat endpoint - redirects to RAG chat
    Maintained for backward compatibility with existing frontend
    """
    from app.services.chat_service import get_rag_chat_service
    from app.services.escalation_service import escalation_service
    
    session_id = request.session_id or str(uuid.uuid4())
    
    # Check for explicit escalation request
    should_escalate, escalation_reason = escalation_service.should_escalate(
        request.message, ""
    )
    
    if should_escalate:
        escalation_message = (
            "Tôi sẽ chuyển bạn đến thủ thư ngay. "
            "Vui lòng chờ trong giây lát, thủ thư sẽ tiếp nhận và hỗ trợ bạn! 👋"
        )
        
        if request.conversation_id or request.student_id:
            await escalation_service.escalate_conversation(
                conversation_id=request.conversation_id,
                student_id=request.student_id,
                reason=escalation_reason
            )
        
        # Save escalation messages to MongoDB
        from app.services.mongo_service import get_mongo_service
        mongo_service = get_mongo_service()
        mongo_service.save_message(session_id, "user", request.message)
        mongo_service.save_message(session_id, "assistant", escalation_message,
                                   action=ActionType.ESCALATE_TO_LIBRARIAN.value)
        
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
    
    # Use RAG service
    rag_service = get_rag_chat_service()
    result = rag_service.query(request.message)
    
    needs_review = result["action"] == ActionType.ESCALATE_TO_LIBRARIAN
    
    reply = result["reply"]
    if needs_review:
        reply += (
            "\n\n💡 *Nếu bạn cần hỗ trợ thêm, hãy nói 'cho em gặp thủ thư' "
            "để được kết nối với nhân viên thư viện.*"
        )
    
    confidence_score = min(result["similarity_score"], 1.0) if not needs_review else 0.3
    
    # Save to MongoDB (để backend Java có thể đọc qua /api/v1/chat/history/{session_id})
    from app.services.mongo_service import get_mongo_service
    mongo_service = get_mongo_service()
    mongo_service.save_message(session_id, "user", request.message)
    mongo_service.save_message(session_id, "assistant", result["reply"],
                               action=result["action"].value if result["action"] else None)
    
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


@app.post("/api/ai/generate", response_model=GeminiResponse)
async def generate_response(request: GenerateRequest):
    """
    Legacy generate endpoint - uses RAG under the hood
    """
    from app.services.chat_service import get_rag_chat_service
    
    rag_service = get_rag_chat_service()
    result = rag_service.query(request.user_message)
    
    return GeminiResponse(
        content=result["reply"],
        confidence_score=min(result["similarity_score"], 1.0),
        needs_review=result["action"] == ActionType.ESCALATE_TO_LIBRARIAN
    )


# ============== KNOWLEDGE BASE ENDPOINTS ==============

@app.get("/api/ai/knowledge")
async def get_knowledge():
    """Get all knowledge base items (legacy - from Java backend)"""
    return knowledge_base_service.get_all_knowledge()


@app.post("/api/ai/knowledge")
async def add_knowledge(title: str, content: str, knowledge_type: str = "INFO"):
    """Add new knowledge item (legacy)"""
    knowledge_base_service.add_knowledge(title, content, knowledge_type)
    return {"success": True, "message": "Knowledge added successfully"}


# ============== PROMPTS ENDPOINTS (Stub) ==============

prompts_storage = []

@app.get("/api/ai/prompts")
async def get_prompts():
    """Get all prompt templates"""
    return prompts_storage


@app.post("/api/ai/prompts")
async def create_prompt(name: str, prompt: str, context: str = "GENERAL"):
    """Create new prompt template"""
    new_prompt = {
        "id": len(prompts_storage) + 1,
        "name": name,
        "prompt": prompt,
        "context": context
    }
    prompts_storage.append(new_prompt)
    return new_prompt


# ============== ANALYTICS ENDPOINTS ==============

@app.get("/api/ai/analytics/peak-hours")
async def get_peak_hours(area_id: str = None):
    """Analyze peak hours for library"""
    return analytics_ai_service.analyze_peak_hours(area_id)


@app.get("/api/ai/analytics/recommend-slots")
async def recommend_time_slots(duration_hours: int = 2):
    """Get AI-powered time slot recommendations"""
    return analytics_ai_service.recommend_time_slots(duration_hours=duration_hours)


@app.get("/api/ai/analytics/statistics")
async def get_statistics(period: str = "week", area_id: str = None):
    """Get usage statistics for librarian dashboard"""
    return analytics_ai_service.get_usage_statistics(period, area_id)


@app.get("/api/ai/analytics/predict-capacity")
async def predict_capacity(hours_ahead: int = 1, zone_id: str = None):
    """Predict library capacity at a future time"""
    from datetime import datetime, timedelta
    target_time = datetime.now() + timedelta(hours=hours_ahead)
    return analytics_ai_service.predict_capacity(target_time, zone_id)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
