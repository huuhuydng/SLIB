#!/bin/bash

# ============================================
# SLIB Docker - Stop All Services
# ============================================

echo "🛑 Stopping SLIB Docker Services..."

cd /Users/hadi/Desktop/slib

# Stop all containers
docker-compose down

echo ""
echo "✅ All containers stopped"
echo ""
echo "📝 Notes:"
echo "   • Data is preserved in Docker volumes"
echo "   • To remove volumes: docker-compose down -v"
echo "   • To remove images:  docker-compose down --rmi all"
