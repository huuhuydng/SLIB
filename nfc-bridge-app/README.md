# SLIB NFC Bridge App

Ứng dụng desktop cho phép web quản trị SLIB mở và kiểm tra `NFC Bridge` chỉ bằng một nút, thay vì yêu cầu người dùng tự chạy script thủ công.

## Mục tiêu

- Gói bridge Node hiện tại thành app desktop chạy nền.
- Tự chạy cùng hệ điều hành sau khi cài đặt.
- Đăng ký custom protocol `slib-nfc-bridge://`.
- Giữ local API ở `http://127.0.0.1:5050` để frontend cũ vẫn tái sử dụng được.

## Kiến trúc

- `src/main.js`: process chính của Electron
  - tray icon
  - auto-start
  - custom protocol
  - khởi động bridge cục bộ từ package `slib-nfc-bridge`
- `src/preload.js`: cầu nối an toàn giữa renderer và main process
- `src/status.html`: cửa sổ trạng thái đơn giản cho thủ thư/admin

Bridge đọc ACR122U vẫn dùng lại code ở package gốc:

- `../nfc-bridge/src/bridgeServer.js`

## Chạy thử local

```bash
cd nfc-bridge-app
npm install
npm start
```

## Build installer

```bash
cd nfc-bridge-app
npm install
npm run dist
```

Electron Builder sẽ sinh installer trong thư mục `release/`.

## Ghi chú triển khai

- `nfc-pcsc` là native module, nên sau `npm install` cần giữ bước `electron-builder install-app-deps`.
- App sẽ mở protocol:

```txt
slib-nfc-bridge://open
slib-nfc-bridge://scan
```

- Frontend web có thể dùng `window.location.href = 'slib-nfc-bridge://open'` để yêu cầu bật app trên máy đã cài.
