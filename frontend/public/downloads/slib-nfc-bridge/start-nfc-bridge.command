#!/bin/bash
set -e

cd "$(dirname "$0")"

if ! command -v node >/dev/null 2>&1; then
  echo "Node.js chua duoc cai tren may nay."
  echo "Hay cai Node.js tai https://nodejs.org/en/download roi chay lai."
  read -r -p "Nhan Enter de dong..."
  exit 1
fi

if [ ! -d node_modules ]; then
  echo "Dang cai dependencies cho NFC Bridge..."
  npm install
fi

echo "Dang khoi dong SLIB NFC Bridge..."
npm start
