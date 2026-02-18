# AI Service Development Reference

Chi tiết về phát triển AI Service FastAPI cho SLIB.

## Tech Stack

- Python 3.11 + FastAPI
- LangChain
- Qdrant (Vector DB)
- Ollama (Self-hosted LLM)

## Router Pattern

```python
# routers/resource.py
from fastapi import APIRouter, HTTPException, Depends
from typing import List
from ..models.schemas import ResourceRequest, ResourceResponse
from ..services.resource_service import ResourceService

router = APIRouter(prefix="/api/resources", tags=["Resources"])

@router.get("", response_model=List[ResourceResponse])
async def get_all(service: ResourceService = Depends()):
    return await service.find_all()

@router.get("/{id}", response_model=ResourceResponse)
async def get_by_id(id: int, service: ResourceService = Depends()):
    result = await service.find_by_id(id)
    if not result:
        raise HTTPException(status_code=404, detail="Not found")
    return result

@router.post("", response_model=ResourceResponse, status_code=201)
async def create(
    request: ResourceRequest,
    service: ResourceService = Depends()
):
    return await service.create(request)
```

## Service Pattern

```python
# services/resource_service.py
from typing import List, Optional
from ..models.schemas import ResourceRequest, ResourceResponse
from ..core.database import get_db

class ResourceService:
    def __init__(self):
        self.db = get_db()
    
    async def find_all(self) -> List[ResourceResponse]:
        query = "SELECT * FROM resources"
        results = await self.db.fetch_all(query)
        return [ResourceResponse(**r) for r in results]
    
    async def find_by_id(self, id: int) -> Optional[ResourceResponse]:
        query = "SELECT * FROM resources WHERE id = :id"
        result = await self.db.fetch_one(query, {"id": id})
        return ResourceResponse(**result) if result else None
    
    async def create(self, request: ResourceRequest) -> ResourceResponse:
        query = """
            INSERT INTO resources (name, status)
            VALUES (:name, :status)
            RETURNING *
        """
        result = await self.db.fetch_one(query, request.dict())
        return ResourceResponse(**result)
```

## Schema Pattern

```python
# models/schemas.py
from pydantic import BaseModel, Field
from datetime import datetime
from enum import Enum

class ResourceStatus(str, Enum):
    AVAILABLE = "AVAILABLE"
    BOOKED = "BOOKED"
    MAINTENANCE = "MAINTENANCE"

class ResourceRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=255)
    status: ResourceStatus

class ResourceResponse(BaseModel):
    id: int
    name: str
    status: ResourceStatus
    created_at: datetime
    
    class Config:
        from_attributes = True
```

## RAG Chat Pattern

```python
# services/chat_service.py
from langchain_community.llms import Ollama
from langchain_community.embeddings import OllamaEmbeddings
from qdrant_client import QdrantClient
from ..config.settings import settings

class ChatService:
    def __init__(self):
        self.llm = Ollama(
            base_url=settings.OLLAMA_URL,
            model=settings.OLLAMA_MODEL
        )
        self.embeddings = OllamaEmbeddings(
            base_url=settings.OLLAMA_URL,
            model=settings.OLLAMA_EMBEDDING_MODEL
        )
        self.qdrant = QdrantClient(url=settings.QDRANT_URL)
    
    async def chat(self, query: str, user_id: int) -> dict:
        # 1. Embed query
        query_vector = self.embeddings.embed_query(query)
        
        # 2. Search similar documents
        results = self.qdrant.search(
            collection_name=settings.QDRANT_COLLECTION,
            query_vector=query_vector,
            limit=5
        )
        
        # 3. Build context
        context = "\n".join([r.payload["text"] for r in results])
        
        # 4. Generate response
        prompt = f"""Context: {context}
        
Question: {query}

Answer in Vietnamese:"""
        
        response = self.llm.invoke(prompt)
        
        # 5. Check confidence
        if results[0].score < settings.SIMILARITY_THRESHOLD:
            return {
                "response": response,
                "action": "ESCALATE_TO_LIBRARIAN",
                "confidence": results[0].score
            }
        
        return {
            "response": response,
            "sources": [r.payload["source"] for r in results],
            "confidence": results[0].score
        }
```

## Settings Pattern

```python
# config/settings.py
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # Ollama
    OLLAMA_URL: str = "http://localhost:11434"
    OLLAMA_MODEL: str = "llama3.2"
    OLLAMA_EMBEDDING_MODEL: str = "nomic-embed-text"
    
    # Qdrant
    QDRANT_URL: str = "http://localhost:6333"
    QDRANT_COLLECTION: str = "slib_knowledge"
    
    # Database
    DATABASE_URL: str
    
    # RAG
    SIMILARITY_THRESHOLD: float = 0.7
    MAX_CONTEXT_CHUNKS: int = 5
    
    class Config:
        env_file = ".env"

settings = Settings()
```

## Main Application

```python
# main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from .routers import chat, ingestion

app = FastAPI(title="SLIB AI Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat.router)
app.include_router(ingestion.router)

@app.get("/health")
async def health():
    return {"status": "healthy"}
```
