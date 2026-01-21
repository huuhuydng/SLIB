# SLIB AI Service 🤖

Microservice AI độc lập cho hệ thống Thư viện thông minh SLIB, sử dụng Python FastAPI và Google Gemini API.

## 📊 Tính năng AI

| Feature | Endpoint | Mô tả |
|---------|----------|-------|
| **Chat Assistant** | `/api/ai/chat` | Chatbot trả lời câu hỏi sinh viên |
| **Peak Hours Analysis** | `/api/ai/analytics/peak-hours` | Phân tích giờ cao điểm |
| **Time Recommendations** | `/api/ai/analytics/recommend-slots` | Đề xuất khung giờ tối ưu |
| **Usage Statistics** | `/api/ai/analytics/statistics` | Thống kê sử dụng cho thủ thư |
| **Capacity Prediction** | `/api/ai/analytics/predict-capacity` | Dự đoán mức độ đông |

## 📁 Cấu trúc

```
ai-service/
├── app/
│   ├── main.py                    # FastAPI entry point
│   ├── config/settings.py         # Configuration (ENV)
│   ├── models/schemas.py          # Pydantic models
│   └── services/
│       ├── gemini_service.py      # Chat AI (Gemini)
│       ├── analytics_service.py   # Analytics AI
│       └── knowledge_base.py      # Knowledge context
├── start.sh                       # Startup script
├── requirements.txt
├── .env                           # API keys (gitignore)
└── README.md
```

## 🚀 Khởi động nhanh

```bash
# Cách 1: Dùng script (khuyến nghị)
./start.sh

# Cách 2: Thủ công
cd ai-service
source venv/bin/activate
uvicorn app.main:app --reload --port 8001
```

## ⚙️ Cấu hình

Tạo file `.env`:
```env
GEMINI_API_KEY=your_api_key_here
GEMINI_MODEL=gemini-2.0-flash
DEBUG=false
```

## 📖 API Documentation

- **Swagger UI**: http://localhost:8001/docs
- **ReDoc**: http://localhost:8001/redoc
- **Health Check**: http://localhost:8001/health

## 🔌 Tích hợp với Frontend

Frontend gọi trực tiếp đến AI Service:

```javascript
// frontend/src/services/admin/ai/pythonAiApi.js
const api = axios.create({
    baseURL: "http://localhost:8001/api/ai"
});
```

## 🏗️ Kiến trúc hệ thống

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Frontend   │────▶│  AI Service  │────▶│  Gemini API  │
│   (React)    │     │  (FastAPI)   │     │  (Google)    │
│   :5173      │     │   :8001      │     └──────────────┘
└──────────────┘     └──────────────┘
        │                   │
        │            ┌──────▼──────┐
        │            │  Analytics  │
        │            │   Engine    │
        │            └─────────────┘
        │
        └───────────▶┌──────────────┐
                     │   Backend    │
                     │ (Spring Boot)│
                     │    :8080     │
                     └──────────────┘
                            │
                     ┌──────▼──────┐
                     │  PostgreSQL │
                     │   Database  │
                     └─────────────┘
```

## 🔮 Roadmap

- [ ] Tích hợp ML model cho dự đoán capacity
- [ ] RAG với knowledge base thực từ database
- [ ] Caching với Redis
- [ ] Rate limiting
- [ ] Authentication với JWT
- [ ] Logging và monitoring

## 📄 License

MIT
