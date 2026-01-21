#!/bin/bash

# ============================================
# SLIB AI Service - Startup Script (macOS/Linux)
# ============================================

echo "🤖 Starting SLIB AI Service..."

# Navigate to ai-service directory
cd "$(dirname "$0")"

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "📦 Creating virtual environment..."
    python3 -m venv venv
    echo "📥 Installing dependencies..."
    ./venv/bin/pip install -r requirements.txt
fi

# Check if .env exists
if [ ! -f ".env" ]; then
    echo "⚠️  Warning: .env file not found!"
    echo "📝 Creating from .env.example..."
    cp .env.example .env
    echo "❗ Please edit .env and add your GEMINI_API_KEY"
fi

# Activate virtual environment and run
echo "🚀 Starting server on http://localhost:8001"
echo "📖 Swagger docs: http://localhost:8001/docs"
echo ""
./venv/bin/uvicorn app.main:app --reload --port 8001
