@echo off
REM ============================================
REM SLIB - Khởi động toàn bộ hệ thống (Windows)
REM ============================================

echo.
echo ========================================
echo    SLIB System Startup (Windows)
echo ========================================
echo.

REM Lấy đường dẫn hiện tại
set SLIB_ROOT=%~dp0
cd /d %SLIB_ROOT%

REM 1. Start PostgreSQL
echo.
echo [1/5] Starting PostgreSQL Database...
docker-compose up -d
timeout /t 2 >nul

REM Check if database is running
docker ps | findstr "slib-postgres" >nul
if %errorlevel%==0 (
    echo       [OK] PostgreSQL is running
) else (
    echo       [FAIL] PostgreSQL failed to start
    pause
    exit /b 1
)

REM 2. Start Backend (in new window)
echo.
echo [2/5] Starting Spring Boot Backend (port 8080)...
cd /d %SLIB_ROOT%backend
start "SLIB Backend" cmd /c "mvn spring-boot:run -Dmaven.test.skip=true"
echo       [OK] Backend starting in new window...

REM 3. Start AI Service (in new window)
echo.
echo [3/5] Starting Python AI Service (port 8001)...
cd /d %SLIB_ROOT%ai-service
if exist "venv\Scripts\activate.bat" (
    start "SLIB AI Service" cmd /c "venv\Scripts\activate && uvicorn app.main:app --port 8001"
    echo       [OK] AI Service starting in new window...
) else (
    echo       [SKIP] AI Service venv not found, skipping...
)

REM 4. Start ngrok tunnel (optional, in new window)
echo.
echo [4/5] Starting ngrok tunnel (port 8080)...
where ngrok >nul 2>nul
if %errorlevel%==0 (
    start "SLIB ngrok" cmd /c "ngrok http 8080"
    echo       [OK] ngrok starting in new window...
) else (
    echo       [SKIP] ngrok not installed, skipping...
)

REM Wait for backend to start
echo.
echo Waiting for services to initialize...
timeout /t 5 >nul

REM 5. Start Frontend (in new window)
echo.
echo [5/5] Starting React Frontend (port 5173)...
cd /d %SLIB_ROOT%frontend
start "SLIB Frontend" cmd /c "npm run dev"

echo.
echo ============================================
echo    SLIB System Started!
echo ============================================
echo.
echo Services:
echo    * Frontend:    http://localhost:5173
echo    * Backend:     http://localhost:8080
echo    * AI Service:  http://localhost:8001
echo    * AI Docs:     http://localhost:8001/docs
echo    * Database:    localhost:5432
echo.
echo To stop all services: stop-all.bat
echo ============================================
echo.
pause
