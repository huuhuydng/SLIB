# SLIB NFC Bridge

Công cụ đọc NFC tag UID qua đầu đọc ACR122U, phục vụ trang quản lý NFC trên admin frontend.

## Yêu cầu

- Node.js 18+
- Đầu đọc NFC: **ACR122U** (USB)
- Driver: `libnfc` hoặc driver mặc định của ACR122U

## Cài đặt

```bash
cd tools/nfc-bridge
npm install
```

## Chạy

```bash
npm start
```

Server sẽ chạy tại `http://localhost:5050`.

## API Endpoints

### `GET /scan-uid`
Chờ quét thẻ NFC, trả về UID.

**Response (200):**
```json
{ "uid": "04A23C91" }
```

**Response (408 — timeout 30s):**
```json
{ "error": "timeout", "message": "Timeout — không phát hiện thẻ NFC" }
```

**Response (503 — reader not connected):**
```json
{ "error": "NFC reader not connected", "message": "Vui lòng kết nối đầu đọc NFC (ACR122U)" }
```

### `GET /status`
Kiểm tra trạng thái đầu đọc NFC.

```json
{
  "readerConnected": true,
  "readerName": "ACS ACR122U PICC Interface",
  "lastError": null,
  "port": 5050
}
```

### `GET /health`
Health check.

```json
{ "status": "ok", "service": "slib-nfc-bridge" }
```

## Tích hợp

- **Admin frontend** (`NfcManagement.jsx`) gọi `GET http://localhost:5050/scan-uid` khi admin click "Quét NFC".
- Admin đặt thẻ NFC lên đầu đọc → bridge trả UID → frontend gọi backend `PUT /slib/seats/{seatId}/nfc-uid` để gán.
