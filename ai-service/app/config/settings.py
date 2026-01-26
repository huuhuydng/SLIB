"""
Configuration management for AI Service
Uses environment variables for API key (production-ready)
"""

import os
from typing import Optional
from functools import lru_cache
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""
    
    # AI Provider: "ollama" or "gemini"
    ai_provider: str = os.getenv("AI_PROVIDER", "ollama")
    
    # Ollama Configuration (default - no API key needed!)
    ollama_url: str = os.getenv("OLLAMA_URL", "http://localhost:11434")
    ollama_model: str = os.getenv("OLLAMA_MODEL", "llama3.2")
    
    # Gemini API Configuration (fallback)
    gemini_api_key: str = os.getenv("GEMINI_API_KEY", "")
    gemini_model: str = os.getenv("GEMINI_MODEL", "gemini-2.0-flash")
    gemini_api_url: str = "https://generativelanguage.googleapis.com/v1beta/models/"
    
    # AI Default Settings
    default_temperature: float = 0.7
    default_max_tokens: int = 1024
    default_system_prompt: str = (
        "Bạn là SLIB AI Assistant - trợ lý thông minh của hệ thống Thư viện thông minh SLIB. "
        "Hãy trả lời ngắn gọn, thân thiện và chính xác bằng tiếng Việt."
    )
    enable_context: bool = True
    enable_history: bool = True
    
    # Server Settings
    debug: bool = os.getenv("DEBUG", "false").lower() == "true"
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    """Get cached settings instance"""
    return Settings()

