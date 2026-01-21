#!/bin/bash

# ============================================
# SLIB - Dừng toàn bộ hệ thống
# ============================================

echo "🛑 Stopping SLIB System..."

# Stop Frontend (Ctrl+C in terminal usually)
echo "   Stopping Frontend..."
pkill -f "vite" 2>/dev/null

# Stop AI Service
echo "   Stopping AI Service..."
pkill -f "uvicorn app.main:app" 2>/dev/null

# Stop Backend
echo "   Stopping Backend..."
pkill -f "spring-boot:run" 2>/dev/null
kill $(lsof -t -i:8080) 2>/dev/null

# Stop Database
echo "   Stopping Database..."
cd /Users/hadi/Desktop/slib
docker-compose down

echo ""
echo "✅ All services stopped!"
