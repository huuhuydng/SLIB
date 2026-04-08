# SLIB NFC Bridge App

Ứng dụng desktop cho phép web quản trị SLIB mở và kiểm tra `NFC Bridge` bằng app cài sẵn, thay vì bắt từng máy mới tự chạy source zip và `npm install`.

## Mục tiêu

- Gói bridge Node hiện tại thành app desktop chạy nền.
- Tự chạy cùng hệ điều hành sau khi cài đặt.
- Đăng ký custom protocol `slib-nfc-bridge://`.
- Giữ local API ở `http://127.0.0.1:5050` để frontend cũ vẫn tái sử dụng được.
- Chuẩn hóa bộ cài cho máy thủ thư/kiosk mới.

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

## Chuẩn môi trường build

Khuyến nghị:

- Node `20 LTS`
- npm đi kèm Node 20
- Windows build: Visual Studio Build Tools nếu cần rebuild native dependency

File [.nvmrc](./.nvmrc) đã khóa phiên bản Node khuyến nghị là `20`.

## Chạy thử local

```bash
cd nfc-bridge-app
npm install
npm start
```

## Build bộ cài

### Windows

```bash
cd nfc-bridge-app
npm install
npm run dist:win
```

### macOS

```bash
cd nfc-bridge-app
npm install
npm run dist:mac
```

### Build tất cả target đã khai báo

```bash
cd nfc-bridge-app
npm install
npm run dist
```

Electron Builder sẽ sinh artifact trong thư mục `release/`.

Artifact name đã được chuẩn hóa theo mẫu:

```txt
SLIB NFC Bridge-<version>-<os>-<arch>.<ext>
```

## Phát hành cho web admin

Luồng khuyến nghị:

1. Build bộ cài Windows từ `nfc-bridge-app`.
2. Đổi tên file phát hành ổn định theo convention nội bộ, ví dụ:
   - `SLIB-NFC-Bridge-Setup.exe`
   - `SLIB-NFC-Bridge.dmg`
3. Chép file vào:
   - `frontend/public/downloads/slib-nfc-bridge-app/`
4. Deploy lại frontend để trang `admin/nfc-management` tải đúng bộ cài.

Hoặc dùng workflow tự động:

```txt
.github/workflows/nfc_bridge_release.yml
```

Workflow này sẽ build installer và có thể đẩy thẳng file lên thư mục downloads trên VM.

## Khi nào dùng source zip cũ?

Chỉ dùng `frontend/public/downloads/slib-nfc-bridge.zip` khi:

- team kỹ thuật cần debug bridge
- chưa kịp phát hành bộ cài mới
- cần chạy thủ công trên máy dev

Không khuyến nghị dùng flow này cho máy thủ thư mới.

## Ghi chú triển khai

- `nfc-pcsc` là native module, nên sau `npm install` cần giữ bước `electron-builder install-app-deps`.
- App sẽ mở protocol:

```txt
slib-nfc-bridge://open
slib-nfc-bridge://scan
```

- Frontend web có thể dùng:

```js
window.location.href = "slib-nfc-bridge://open";
```

để yêu cầu bật app trên máy đã cài.
