#!/bin/bash

# ============================================
# SLIB - Khởi động toàn bộ hệ thống
# ============================================

echo "🚀 SLIB System Startup"
echo "======================"

cd /Users/hadi/Desktop/slib

# 1. Start PostgreSQL
echo ""
echo "1️⃣  Starting PostgreSQL Database..."
docker-compose up -d
sleep 2

# Check if database is running
if docker ps | grep -q slib-postgres; then
    echo "   ✅ PostgreSQL is running"
else
    echo "   ❌ PostgreSQL failed to start"
    exit 1
fi

# 2. Start Backend (in background)
echo ""
echo "2️⃣  Starting Spring Boot Backend (port 8080)..."
cd /Users/hadi/Desktop/slib/backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export MAVEN_OPTS="-Xmx2g -Xms512m"
mvn spring-boot:run -Dmaven.test.skip=true > /tmp/slib-backend.log 2>&1 &
BACKEND_PID=$!
echo "   🔄 Backend starting... (PID: $BACKEND_PID)"

# 3. Start AI Service (in background)
echo ""
echo "3️⃣  Starting Python AI Service (port 8001)..."
cd /Users/hadi/Desktop/slib/ai-service
./venv/bin/uvicorn app.main:app --port 8001 > /tmp/slib-ai.log 2>&1 &
AI_PID=$!
echo "   🔄 AI Service starting... (PID: $AI_PID)"

# 4. Start ngrok tunnel for Backend (in background)
echo ""
echo "4️⃣  Starting ngrok tunnel (port 8080)..."
ngrok http 8080 > /tmp/slib-ngrok.log 2>&1 &
NGROK_PID=$!
echo "   🔄 ngrok starting... (PID: $NGROK_PID)"

# 5. Start Frontend (in foreground so we can see output)
echo ""
echo "5️⃣  Starting React Frontend (port 5173)..."
cd /Users/hadi/Desktop/slib/frontend

# Wait a bit for backend services to start
sleep 5

echo ""
echo "============================================"
echo "✅ SLIB System Started!"
echo "============================================"
echo ""
echo "📍 Services:"
echo "   • Frontend:    http://localhost:5173"
echo "   • Backend:     http://localhost:8080"
echo "   • ngrok:       Check /tmp/slib-ngrok.log for public URL"
echo "   • AI Service:  http://localhost:8001"
echo "   • AI Docs:     http://localhost:8001/docs"
echo "   • Database:    localhost:5432"
echo ""
echo "📝 Logs:"
echo "   • Backend: /tmp/slib-backend.log"
echo "   • AI:      /tmp/slib-ai.log"
echo "   • ngrok:   /tmp/slib-ngrok.log"
echo ""
echo "🛑 To stop: ./stop-all.sh"
echo "============================================"
echo ""

# Start frontend in foreground
npm run dev
