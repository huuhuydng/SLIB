"""
SLIB AI Service - FastAPI Application
Main entry point for the AI microservice
Supports both Ollama (local) and Gemini (cloud) AI providers
"""

import uuid
from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from typing import Dict

from app.models.schemas import (
    GenerateRequest, 
    GeminiResponse, 
    TestConnectionResponse,
    ChatRequest,
    ChatResponse,
    AIConfig
)
from app.services.gemini_service import GeminiService, get_gemini_service
from app.services.ollama_service import OllamaService, get_ollama_service
from app.services.knowledge_base import knowledge_base_service
from app.services.analytics_service import analytics_ai_service
from app.services.java_backend_client import get_java_client
from app.config.settings import get_settings


def get_ai_service() -> OllamaService:
    """Get the configured AI service (uses config from Java backend)"""
    return get_ollama_service()


# Create FastAPI app
app = FastAPI(
    title="SLIB AI Service",
    description="AI Assistant service for SLIB Smart Library - Supports Ollama & Gemini",
    version="1.1.0",
    docs_url="/docs",
    redoc_url="/redoc"

)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# In-memory session storage (use Redis in production)
chat_sessions: Dict[str, list] = {}


# ============== HEALTH CHECK ==============

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy", "service": "slib-ai-service"}


# ============== AI CONFIG ENDPOINTS ==============

@app.get("/api/ai/config")
async def get_config():
    """Get current AI configuration from Java backend"""
    java_client = get_java_client()
    config = java_client.get_ai_config(force_refresh=True)
    return {
        "configured": True,
        "provider": config.get("provider", "ollama"),
        "model": config.get("ollamaModel", "llama3.2"),
        "temperature": config.get("temperature", 0.7),
        "max_tokens": config.get("maxTokens", 1024),
        "enable_context": config.get("enableContext", True),
        "enable_history": config.get("enableHistory", True),
        "system_prompt": config.get("systemPrompt", "")
    }


@app.post("/api/ai/refresh")
async def refresh_config():
    """Force refresh AI configuration from Java backend"""
    java_client = get_java_client()
    java_client.refresh_all()
    
    service = get_ai_service()
    service.refresh_config()
    
    return {
        "success": True,
        "message": "Đã refresh cấu hình AI từ database"
    }


@app.post("/api/ai/test-connection", response_model=TestConnectionResponse)
async def test_api_connection():
    """Test AI API connection (Ollama or Gemini)"""
    service = get_ai_service()
    result = service.test_connection()
    return TestConnectionResponse(
        success=result["success"],
        message=result["message"],
        model=result.get("model")
    )


# ============== CHAT ENDPOINTS ==============

@app.post("/api/ai/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """
    Simple chat endpoint for students with AI-to-Human escalation support
    
    Example request:
    ```json
    {
        "message": "Thư viện mở cửa lúc mấy giờ?",
        "session_id": "optional-session-id",
        "conversation_id": "optional-uuid",
        "student_id": "optional-student-uuid"
    }
    ```
    """
    from app.services.escalation_service import escalation_service
    
    # Get or create session
    session_id = request.session_id or str(uuid.uuid4())
    
    # Get chat history for this session
    history = chat_sessions.get(session_id, [])
    
    # Convert to ChatMessage format
    from app.models.schemas import ChatMessage
    chat_history = [ChatMessage(role=h["role"], content=h["content"]) for h in history]
    
    # Check if user is requesting escalation BEFORE generating AI response
    should_escalate, escalation_reason = escalation_service.should_escalate(request.message, "")
    
    if should_escalate:
        # User explicitly wants to talk to human
        escalation_message = "Tôi sẽ chuyển bạn đến thủ thư ngay. Vui lòng chờ trong giây lát, thủ thư sẽ tiếp nhận và hỗ trợ bạn! 👋"
        
        # Call backend to escalate if we have conversation/student info
        if request.conversation_id or request.student_id:
            await escalation_service.escalate_conversation(
                conversation_id=request.conversation_id,
                student_id=request.student_id,
                reason=escalation_reason
            )
        
        # Save escalation message to history
        if session_id not in chat_sessions:
            chat_sessions[session_id] = []
        chat_sessions[session_id].append({"role": "user", "content": request.message})
        chat_sessions[session_id].append({"role": "assistant", "content": escalation_message})
        
        return ChatResponse(
            success=True,
            reply=escalation_message,
            session_id=session_id,
            confidence_score=1.0,
            needs_review=False,
            escalated=True,
            escalation_message=escalation_reason
        )
    
    # Generate AI response
    service = get_ai_service()
    response = service.generate_response(request.message, chat_history)
    
    # Check if AI response indicates uncertainty (needs escalation)
    ai_needs_escalation, ai_escalation_reason = escalation_service.should_escalate(
        request.message, response.content
    )
    
    # Save to history
    if session_id not in chat_sessions:
        chat_sessions[session_id] = []
    chat_sessions[session_id].append({"role": "user", "content": request.message})
    chat_sessions[session_id].append({"role": "assistant", "content": response.content})
    
    # Limit history size
    if len(chat_sessions[session_id]) > 20:
        chat_sessions[session_id] = chat_sessions[session_id][-20:]
    
    # If AI response indicates escalation is needed, notify user
    if ai_needs_escalation and response.needs_review:
        escalation_note = "\n\n💡 *Nếu bạn cần hỗ trợ thêm, hãy nói 'cho em gặp thủ thư' để được kết nối với nhân viên thư viện.*"
        response_content = response.content + escalation_note
    else:
        response_content = response.content
    
    return ChatResponse(
        success=True,
        reply=response_content,
        session_id=session_id,
        confidence_score=response.confidence_score,
        needs_review=response.needs_review,
        escalated=False
    )



@app.post("/api/ai/generate", response_model=GeminiResponse)
async def generate_response(request: GenerateRequest):
    """
    Full generate response endpoint with config override
    
    Example request:
    ```json
    {
        "user_message": "Cách đặt chỗ ngồi?",
        "chat_history": [
            {"role": "user", "content": "Xin chào"},
            {"role": "assistant", "content": "Chào bạn!"}
        ],
        "config": {
            "model": "gemini-2.5-flash",
            "temperature": 0.5
        }
    }
    ```
    """
    service = get_ai_service()
    return service.generate_response(request.user_message, request.chat_history)


# ============== KNOWLEDGE BASE ENDPOINTS ==============

@app.get("/api/ai/knowledge")
async def get_knowledge():
    """Get all knowledge base items"""
    return knowledge_base_service.get_all_knowledge()


@app.post("/api/ai/knowledge")
async def add_knowledge(title: str, content: str, knowledge_type: str = "INFO"):
    """Add new knowledge item"""
    knowledge_base_service.add_knowledge(title, content, knowledge_type)
    return {"success": True, "message": "Knowledge added successfully"}


# ============== PROMPTS ENDPOINTS (Stub) ==============

# In-memory prompts storage
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
    """
    Analyze peak hours for library
    
    Returns peak hours, quiet hours, and recommendations
    """
    return analytics_ai_service.analyze_peak_hours(area_id)


@app.get("/api/ai/analytics/recommend-slots")
async def recommend_time_slots(duration_hours: int = 2):
    """
    Get AI-powered time slot recommendations
    
    Args:
        duration_hours: Desired study duration (default: 2)
    """
    return analytics_ai_service.recommend_time_slots(duration_hours=duration_hours)


@app.get("/api/ai/analytics/statistics")
async def get_statistics(period: str = "week", area_id: str = None):
    """
    Get usage statistics for librarian dashboard
    
    Args:
        period: "day", "week", or "month"
        area_id: Optional specific area
    """
    return analytics_ai_service.get_usage_statistics(period, area_id)


@app.get("/api/ai/analytics/predict-capacity")
async def predict_capacity(hours_ahead: int = 1, zone_id: str = None):
    """
    Predict library capacity at a future time
    
    Args:
        hours_ahead: Hours from now to predict
        zone_id: Optional specific zone
    """
    from datetime import datetime, timedelta
    target_time = datetime.now() + timedelta(hours=hours_ahead)
    return analytics_ai_service.predict_capacity(target_time, zone_id)


# ============== STARTUP EVENT ==============

@app.on_event("startup")
async def startup_event():
    """Run on application startup"""
    settings = get_settings()
    print("=" * 50)
    print("🤖 SLIB AI Service Starting...")
    print(f"🔌 AI Provider: {settings.ai_provider.upper()}")
    if settings.ai_provider.lower() == "ollama":
        print(f"📦 Model: {settings.ollama_model}")
        print(f"🏠 Ollama URL: {settings.ollama_url}")
    else:
        print(f"📦 Model: {settings.gemini_model}")
        print(f"🔑 API Key configured: {bool(settings.gemini_api_key)}")
    print(f"🌐 Debug mode: {settings.debug}")
    print("")
    print("📊 Available AI Features:")
    print(f"   • Chat Assistant ({settings.ai_provider.title()})")
    print("   • Peak Hours Analysis")
    print("   • Time Slot Recommendations")
    print("   • Usage Statistics")
    print("   • Capacity Prediction")
    print("=" * 50)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)

