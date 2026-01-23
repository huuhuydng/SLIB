@echo off
REM ============================================
REM SLIB - Dừng toàn bộ hệ thống (Windows)
REM ============================================

echo.
echo ========================================
echo    Stopping SLIB System (Windows)
echo ========================================
echo.

REM Lấy đường dẫn hiện tại
set SLIB_ROOT=%~dp0
cd /d %SLIB_ROOT%

REM Stop Frontend (Node.js)
echo [1/5] Stopping Frontend...
taskkill /F /IM node.exe /FI "WINDOWTITLE eq SLIB Frontend*" >nul 2>&1
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":5173"') do taskkill /F /PID %%a >nul 2>&1
echo       Done.

REM Stop AI Service (Python)
echo [2/5] Stopping AI Service...
taskkill /F /IM python.exe /FI "WINDOWTITLE eq SLIB AI*" >nul 2>&1
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8001"') do taskkill /F /PID %%a >nul 2>&1
echo       Done.

REM Stop ngrok
echo [3/5] Stopping ngrok...
taskkill /F /IM ngrok.exe >nul 2>&1
echo       Done.

REM Stop Backend (Java)
echo [4/5] Stopping Backend...
taskkill /F /IM java.exe /FI "WINDOWTITLE eq SLIB Backend*" >nul 2>&1
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8080"') do taskkill /F /PID %%a >nul 2>&1
echo       Done.

REM Stop Database
echo [5/5] Stopping Database...
cd /d %SLIB_ROOT%
docker-compose down
echo       Done.

echo.
echo ============================================
echo    All SLIB services stopped!
echo ============================================
echo.
pause
