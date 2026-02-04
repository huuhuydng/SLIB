#!/bin/bash

# ============================================
# SLIB Dev Mode - Start Development Environment
# Chỉ chạy DB + Redis trong Docker, còn lại chạy trực tiếp
# ============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "🔧 SLIB Development Mode"
echo "========================"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check Docker
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker first.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker is running${NC}"

# Check Ollama (optional)
if curl -s http://localhost:11434/api/version > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Ollama is running${NC}"
else
    echo -e "${YELLOW}⚠️  Ollama is not running. AI features may not work.${NC}"
    echo -e "${YELLOW}   Start with: ollama serve${NC}"
fi

echo ""
echo "📦 Starting Database & Redis..."
docker-compose up -d slib-postgres slib-redis

# Wait for services
echo ""
echo "⏳ Waiting for services to be healthy..."
sleep 5

# Check PostgreSQL
if docker exec slib-postgres pg_isready -U postgres > /dev/null 2>&1; then
    echo -e "${GREEN}✅ PostgreSQL is ready${NC}"
else
    echo -e "${YELLOW}⏳ PostgreSQL starting...${NC}"
    sleep 5
fi

# Check Redis
if docker exec slib-redis redis-cli ping > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Redis is ready${NC}"
else
    echo -e "${YELLOW}⏳ Redis starting...${NC}"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}🚀 Dev Environment Ready!${NC}"
echo "=========================================="
echo ""
echo -e "${BLUE}Now start services in separate terminals:${NC}"
echo ""
echo -e "${YELLOW}Terminal 1 - Backend:${NC}"
echo "  cd $SCRIPT_DIR/backend"
echo "  mvn spring-boot:run -Dmaven.test.skip=true"
echo ""
echo -e "${YELLOW}Terminal 2 - AI Service:${NC}"
echo "  cd $SCRIPT_DIR/ai-service"
echo "  ./venv/bin/uvicorn app.main:app --reload --port 8001"
echo ""
echo -e "${YELLOW}Terminal 3 - Frontend:${NC}"
echo "  cd $SCRIPT_DIR/frontend"
echo "  npm run dev"
echo ""
echo -e "${YELLOW}Terminal 4 - Mobile (Optional):${NC}"
echo "  cd $SCRIPT_DIR/mobile"
echo "  flutter run"
echo ""
echo "=========================================="
echo -e "${BLUE}Quick Commands:${NC}"
echo "  • View DB logs:  docker-compose logs -f slib-postgres"
echo "  • Stop all:      docker-compose down"
echo "  • Reset DB:      docker-compose down -v && docker-compose up -d slib-postgres slib-redis"
echo "=========================================="
