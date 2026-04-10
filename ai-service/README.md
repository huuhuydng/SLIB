# SLIB AI Service

Microservice AI cho hệ thống **SLIB Smart Library** - RAG-based AI Assistant.

![Python](https://img.shields.io/badge/Python-3.11+-3776AB?style=flat-square&logo=python)
![FastAPI](https://img.shields.io/badge/FastAPI-0.109+-009688?style=flat-square&logo=fastapi)
![LangChain](https://img.shields.io/badge/LangChain-0.1+-121212?style=flat-square)
![Qdrant](https://img.shields.io/badge/Qdrant-Vector%20DB-DC382D?style=flat-square)

---

## Tổng quan

AI Service được xây dựng với **FastAPI** và **LangChain**, cung cấp:

- **RAG Chat**: AI chatbot với vector search trên knowledge base
- **Document Ingestion**: Import PDF, DOCX, Text vào knowledge base
- **Smart Escalation**: Tự động chuyển câu hỏi phức tạp cho thủ thư
- **Analytics**: Phân tích peak hours, recommend time slots
- **Self-hosted LLM**: Sử dụng Ollama cho privacy

---

## Cấu trúc dự án

```
ai-service/
├── app/
│   ├── config/
│   │   └── settings.py          # Pydantic settings
│   ├── core/
│   │   └── database.py          # Database connection
│   ├── models/
│   │   └── schemas.py           # Pydantic models
│   ├── routers/
│   │   ├── chat.py              # Chat endpoints
│   │   └── ingestion.py         # Document ingestion
│   ├── services/
│   │   ├── chat_service.py      # RAG chat logic
│   │   ├── escalation_service.py # Human handoff
│   │   ├── knowledge_base.py    # Knowledge management
│   │   ├── analytics_service.py # Analytics logic
│   │   └── java_backend_client.py # Backend integration
│   ├── __init__.py
│   └── main.py                  # FastAPI app
├── .env                         # Environment variables
├── .env.docker                  # Docker environment template
├── Dockerfile                   # Docker build
├── requirements.txt             # Python dependencies
├── start.sh                     # Startup script
└── init_db.sql                  # Database init
```

---

## Tech Stack

| Thành phần | Công nghệ |
|------------|-----------|
| **Framework** | FastAPI 0.109+ |
| **Language** | Python 3.11+ |
| **LLM Framework** | LangChain |
| **Vector DB** | Qdrant |
| **LLM Provider** | Ollama (self-hosted) |
| **Database** | PostgreSQL + SQLAlchemy |
| **Document Processing** | PyPDF2, python-docx |
| **HTTP Client** | HTTPX |
| **Async** | AsyncPG |

---

## Cài đặt và Chạy

### Yêu cầu
- **Python 3.11+**
- **Ollama** (với model như `llama3.2`, `mistral`)
- **Qdrant** (vector database)
- **PostgreSQL 15+**

### Cài đặt Ollama

```bash
# macOS
brew install ollama

# Hoặc download từ https://ollama.ai

# Pull model
ollama pull llama3.2
ollama pull nomic-embed-text
```

### Khởi động Qdrant

```bash
# Docker
docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant
```

### Cài đặt dependencies

```bash
cd ai-service

# Tạo virtual environment
python -m venv venv
source venv/bin/activate  # Linux/Mac
# hoặc: venv\Scripts\activate  # Windows

# Cài dependencies
pip install -r requirements.txt
```

### Chạy Service

```bash
cp .env.example .env
# Điền các giá trị thật trong .env

# Development
uvicorn app.main:app --reload --host 0.0.0.0 --port 8001

# Hoặc dùng script
./start.sh
```

### Chạy với Docker

```bash
# Build
docker build -t slib-ai-service .

# Run
docker run -p 8001:8001 \
  --env-file .env.docker \
  slib-ai-service
```

Lưu ý:
- `.env.docker` chỉ nên là file mẫu, không được commit API key hoặc mật khẩu thật.
- Giá trị thật nên cấp qua `.env` riêng trên máy chạy, VM, hoặc GitHub Secrets.

---

## Environment Variables

```env
# Ollama Config
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2
OLLAMA_EMBEDDING_MODEL=nomic-embed-text

# Qdrant Config
QDRANT_URL=http://localhost:6333
QDRANT_COLLECTION=slib_knowledge

# Database
DATABASE_URL=postgresql://postgres:password@localhost:5434/slib

# Java Backend
JAVA_BACKEND_URL=http://localhost:8080/slib
INTERNAL_API_KEY=your_internal_api_key

# RAG Settings
SIMILARITY_THRESHOLD=0.5
MAX_RETRIEVED_CHUNKS=5

# Debug
DEBUG=true
```

---

## API Endpoints

### Health & Config
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/health` | Health check |
| GET | `/api/ai/config` | Lấy cấu hình AI |
| POST | `/api/ai/refresh` | Refresh config |
| POST | `/api/ai/test-connection` | Test AI connection |

### Chat
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/ai/chat` | Chat với AI (legacy) |
| POST | `/api/ai/generate` | Generate response |
| POST | `/api/chat/rag` | RAG-based chat |
| POST | `/api/chat/session` | Chat với session |

### Knowledge Base
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/ai/knowledge` | Lấy tất cả knowledge |
| POST | `/api/ai/knowledge` | Thêm knowledge mới |

### Document Ingestion
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/ingestion/upload` | Upload document |
| POST | `/api/ingestion/text` | Ingest text |
| GET | `/api/ingestion/documents` | List documents |
| DELETE | `/api/ingestion/{id}` | Xóa document |

### Analytics
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/ai/analytics/peak-hours` | Phân tích peak hours |
| GET | `/api/ai/analytics/recommend-slots` | Gợi ý time slots |
| GET | `/api/ai/analytics/statistics` | Thống kê sử dụng |
| GET | `/api/ai/analytics/predict-capacity` | Dự đoán capacity |

### Prompts
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/ai/prompts` | Lấy prompt templates |
| POST | `/api/ai/prompts` | Tạo prompt mới |

---

## RAG Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   User      │────>│  FastAPI    │────>│   Ollama    │
│   Query     │     │  Service    │     │   LLM       │
└─────────────┘     └──────┬──────┘     └─────────────┘
                           │
                    ┌──────▼──────┐
                    │   Qdrant    │
                    │ Vector DB   │
                    │  (Search)   │
                    └─────────────┘
```

**Workflow:**
1. User gửi câu hỏi
2. Embedding query với `nomic-embed-text`
3. Vector search trong Qdrant để tìm context
4. Kết hợp context + query gửi cho LLM
5. Nếu confidence thấp -> Escalate to librarian
6. Return response với sources

---

## Document Ingestion

### Supported Formats
- PDF files
- DOCX files
- Plain text

### Process
1. Upload file qua `/api/ingestion/upload`
2. Text extraction
3. Chunking với overlap
4. Embedding generation
5. Store vào Qdrant

```bash
# Upload via curl
curl -X POST "http://localhost:8001/api/ingestion/upload" \
  -F "file=@document.pdf" \
  -F "metadata={\"source\":\"library_rules\"}"
```

---

## Escalation Logic

AI tự động chuyển câu hỏi cho thủ thư khi:

- **Confidence thấp**: Similarity score < threshold
- **Không có context**: Không tìm thấy documents liên quan
- **User yêu cầu**: "cho em gặp thủ thư", "cần người hỗ trợ"
- **Câu hỏi phức tạp**: Ngoài phạm vi knowledge base

Response sẽ có `action: "ESCALATE_TO_LIBRARIAN"` để frontend xử lý.

---

## Testing

```bash
# Chạy tests
pytest

# Với coverage
pytest --cov=app --cov-report=html

# Test specific file
pytest tests/test_chat.py -v
```

---

## API Documentation

Khi service đang chạy:

- **Swagger UI**: http://localhost:8001/docs
- **ReDoc**: http://localhost:8001/redoc
- **OpenAPI JSON**: http://localhost:8001/openapi.json

---

## Integration với Backend

AI Service tương tác với Java Backend qua:

- **GET** `/slib/config/ai` - Lấy cấu hình AI
- **GET** `/slib/knowledge-base` - Sync knowledge base
- **POST** `/slib/conversations/{id}/escalate` - Escalate conversation

---

## License

© 2024 SLIB Team. All rights reserved.
