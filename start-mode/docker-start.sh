#!/bin/bash

# ============================================
# SLIB Docker - Start All Services
# ============================================

set -e

echo "🐳 SLIB Docker Microservices Startup"
echo "====================================="

cd /Users/hadi/Desktop/slib

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Check if Ollama is running (optional but recommended for AI)
if curl -s http://localhost:11434/api/version > /dev/null 2>&1; then
    echo "✅ Ollama is running"
else
    echo "⚠️  Ollama is not running. AI features may not work without Ollama."
    echo "   Start Ollama with: ollama serve"
fi

# Build and start all containers
echo ""
echo "📦 Building and starting containers..."
docker-compose up --build -d

# Wait for services to be healthy
echo ""
echo "⏳ Waiting for services to start..."
sleep 5

# Check container status
echo ""
echo "📊 Container Status:"
docker-compose ps

# Wait a bit more for full startup
echo ""
echo "⏳ Waiting for all services to be healthy..."
sleep 10

# Test endpoints
echo ""
echo "🔍 Testing Services:"

# Test PostgreSQL
if docker exec slib-postgres pg_isready -U postgres > /dev/null 2>&1; then
    echo "   ✅ PostgreSQL: Ready"
else
    echo "   ⏳ PostgreSQL: Starting..."
fi

# Test Redis
if docker exec slib-redis redis-cli ping > /dev/null 2>&1; then
    echo "   ✅ Redis: Ready"
else
    echo "   ⏳ Redis: Starting..."
fi

# Test Backend (may take longer to start)
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "   ✅ Backend: Ready"
else
    echo "   ⏳ Backend: Starting (SpringBoot takes ~30-60s)..."
fi

# Test AI Service
if curl -s http://localhost:8001/health > /dev/null 2>&1; then
    echo "   ✅ AI Service: Ready"
else
    echo "   ⏳ AI Service: Starting..."
fi

echo ""
echo "============================================"
echo "🚀 SLIB Docker Started!"
echo "============================================"
echo ""
echo "📍 Services:"
echo "   • Backend:     http://localhost:8080"
echo "   • AI Service:  http://localhost:8001"
echo "   • AI Docs:     http://localhost:8001/docs"
echo "   • Database:    localhost:5432"
echo "   • Redis:       localhost:6379"
echo ""
echo "📝 Commands:"
echo "   • View logs:     docker-compose logs -f"
echo "   • View logs AI:  docker-compose logs -f slib-ai-service"
echo "   • View logs BE:  docker-compose logs -f slib-backend"
echo "   • Stop:          docker-compose down"
echo "   • Rebuild:       docker-compose up --build -d"
echo "============================================"
echo ""
echo "💡 Frontend: Run separately with 'cd frontend && npm run dev'"
echo ""
