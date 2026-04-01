"""
Configuration management for AI Service
Centralized settings with environment variable support
"""

import os
from typing import Optional
from functools import lru_cache
from pydantic_settings import BaseSettings
from app.core.env_loader import load_project_env


load_project_env()


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""
    
    # AI Provider: "ollama" or "gemini"
    ai_provider: str = os.getenv("AI_PROVIDER", "ollama")
    
    # Ollama Configuration (default - no API key needed!)
    ollama_url: str = os.getenv("OLLAMA_URL", "http://localhost:11434")
    ollama_model: str = os.getenv("OLLAMA_MODEL", "llama3.2")
    ollama_embedding_model: str = os.getenv("OLLAMA_EMBEDDING_MODEL", "nomic-embed-text")
    
    # Gemini API Configuration (fallback)
    gemini_api_key: str = os.getenv("GEMINI_API_KEY", "")
    gemini_model: str = os.getenv("GEMINI_MODEL", "gemini-2.0-flash")
    gemini_api_url: str = "https://generativelanguage.googleapis.com/v1beta/models/"
    
    # Database Configuration
    database_url: str = os.getenv(
        "DATABASE_URL", 
        "postgresql://postgres:Slib123@localhost:5432/slib"
    )
    
    # RAG Configuration
    similarity_threshold: float = float(os.getenv("SIMILARITY_THRESHOLD", "0.5"))  # Lowered from 0.75
    chunk_size: int = int(os.getenv("CHUNK_SIZE", "500"))
    chunk_overlap: int = int(os.getenv("CHUNK_OVERLAP", "100"))
    max_retrieved_chunks: int = int(os.getenv("MAX_RETRIEVED_CHUNKS", "5"))
    
    # Qdrant Vector Database Configuration
    qdrant_url: str = os.getenv("QDRANT_URL", "http://localhost:6333")
    qdrant_collection: str = os.getenv("QDRANT_COLLECTION", "slib_knowledge")
    
    # AI Default Settings
    default_temperature: float = 0.7
    default_max_tokens: int = 1024
    default_system_prompt: str = (
        "Bạn là **SLIB AI Assistant** - trợ lý thông minh của hệ thống Thư viện thông minh SLIB.\n"
        "Nhiệm vụ của bạn là hỗ trợ sinh viên và giảng viên tìm kiếm thông tin trong thư viện.\n"
        "Hãy trả lời câu hỏi dựa trên context được cung cấp dưới đây.\n"
        "Phong cách trả lời: Ngắn gọn, thân thiện và chính xác bằng tiếng Việt.\n"
        "**Quy tắc tối thượng:** TUYỆT ĐỐI KHÔNG sử dụng kiến thức bên ngoài (pre-trained knowledge). KHÔNG được suy đoán.\n"
        "Nếu context không chứa câu trả lời, hãy trả lời chính xác cụm từ: 'I_DO_NOT_KNOW'."
    )
    enable_context: bool = True
    enable_history: bool = True
    
    # Server Settings
    debug: bool = os.getenv("DEBUG", "false").lower() == "true"
    jwt_secret: str = os.getenv("JWT_SECRET", "")
    internal_api_key: str = os.getenv("INTERNAL_API_KEY", "")
    
    # Java Backend Integration
    java_backend_url: str = os.getenv("JAVA_BACKEND_URL", "http://localhost:8080/slib")
    
    class Config:
        env_file = ("../.env", ".env")
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    """Get cached settings instance"""
    return Settings()
