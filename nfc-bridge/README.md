# SLIB NFC Bridge Server

Local server that bridges the ACR122U USB NFC reader with the SLIB Admin Portal.

## Requirements

- Node.js 16+
- ACR122U USB NFC Reader
- On macOS: PC/SC driver (usually pre-installed)
- On Windows: ACR122U driver from ACS website
- On Linux: `pcscd` service and `libnfc`

## Installation

```bash
cd nfc-bridge
npm install
```

### Linux Additional Setup

```bash
sudo apt-get install pcscd libpcsclite1 libpcsclite-dev
sudo systemctl start pcscd
```

## Usage

1. Connect ACR122U reader to your PC via USB
2. Start the server:

```bash
npm start
```

3. The server will run on `http://localhost:5050`

## Docker (Linux Only)

NFC Bridge can run in Docker on **Linux hosts** with USB passthrough:

```bash
# Edit docker-compose.yml and uncomment USB passthrough lines
# Then run:
docker compose up -d slib-nfc-bridge
```

> **Note:** Docker Desktop on macOS/Windows does not support USB passthrough.
> For macOS/Windows, run locally: `cd nfc-bridge && npm start`

## API Endpoints

### GET /health

Health check endpoint.

**Response:**
```json
{
  "status": "ok",
  "readerConnected": true,
  "readerName": "ACS ACR122U PICC Interface",
  "timestamp": "2024-01-15T10:30:00.000Z"
}
```

### GET /scan-uid

Initiates NFC tag scan. Waits up to 15 seconds for a card.

**Success Response:**
```json
{
  "success": true,
  "uid": "04A23C91",
  "message": "Quet the thanh cong!"
}
```

**Error Responses:**

Reader not found (503):
```json
{
  "success": false,
  "error": "Khong tim thay dau doc NFC...",
  "errorCode": "READER_NOT_FOUND"
}
```

Timeout (408):
```json
{
  "success": false,
  "error": "Het thoi gian cho. Vui long quet lai.",
  "errorCode": "TIMEOUT"
}
```

### GET /reader-status

Check if reader is connected.

**Response:**
```json
{
  "connected": true,
  "readerName": "ACS ACR122U PICC Interface",
  "message": "Dau doc NFC da san sang"
}
```

## UID Format

The UID is returned as an **uppercase hexadecimal string** without separators:
- 4-byte UID: `04A23C91`
- 7-byte UID: `04A23C91B5D301`

This format matches the Flutter mobile app's NFC UID reading format.

## Troubleshooting

### Reader not detected

1. Check USB connection
2. Try different USB port
3. On Linux, ensure `pcscd` is running: `sudo systemctl status pcscd`
4. On Windows, reinstall ACR122U driver

### Card not reading

1. Ensure card is NFC (13.56 MHz), not RFID (125 kHz)
2. Hold card still on the reader
3. Try different card orientation
4. Some cards may not be compatible (check card type)
