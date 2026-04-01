@echo off
setlocal
cd /d "%~dp0"

where node >nul 2>&1
if errorlevel 1 (
  echo Node.js chua duoc cai tren may nay.
  echo Hay cai Node.js tai https://nodejs.org/en/download roi chay lai.
  pause
  exit /b 1
)

if not exist node_modules (
  echo Dang cai dependencies cho NFC Bridge...
  call npm install
  if errorlevel 1 (
    echo Cai dependencies that bai.
    pause
    exit /b 1
  )
)

echo Dang khoi dong SLIB NFC Bridge...
call npm start
pause
