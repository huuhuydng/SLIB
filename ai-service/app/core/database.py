"""
Database connection module for SLIB AI Service
Simple PostgreSQL connection for metadata operations (not used for vectors - see Qdrant)
"""

import os
import logging
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker, Session
from sqlmodel import SQLModel
from app.core.env_loader import load_project_env


load_project_env()

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Database configuration from environment
DATABASE_URL = os.getenv(
    "DATABASE_URL", 
    "postgresql://postgres:@localhost:5434/slib"
)

# Create SQLAlchemy engine
engine = create_engine(
    DATABASE_URL,
    echo=os.getenv("DEBUG", "false").lower() == "true",
    pool_pre_ping=True,
    pool_size=5,
    max_overflow=10
)

# Session factory
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def get_db():
    """
    Dependency to get database session
    Usage: db: Session = Depends(get_db)
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def init_db():
    """
    Initialize database connection
    Note: Vector storage is handled by Qdrant, not PostgreSQL
    """
    try:
        # Just verify connection
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        
        logger.info("✅ Database connection verified")
        
    except Exception as e:
        logger.error(f"❌ Database connection failed: {e}")
        raise


def check_db_connection() -> bool:
    """Check if database is accessible"""
    try:
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        return True
    except Exception as e:
        logger.error(f"Database connection failed: {e}")
        return False
