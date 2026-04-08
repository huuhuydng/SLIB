# Phát Hành SLIB NFC Bridge Cho Máy Mới

Tài liệu này dùng cho team kỹ thuật khi cần phát hành `SLIB NFC Bridge` cho máy thủ thư hoặc kiosk mới, theo hướng bộ cài sẵn thay vì source zip + `npm install`.

## Mục tiêu

- Máy mới chỉ cần tải và cài app.
- Web `admin/nfc-management` có thể mở bridge qua protocol `slib-nfc-bridge://`.
- Giảm phụ thuộc vào Node, Python và toolchain native trên máy vận hành.

## Mã nguồn liên quan

- `nfc-bridge/`: bridge server gốc
- `nfc-bridge-app/`: app Electron đóng gói bridge thành desktop app
- `frontend/src/pages/admin/NfcManagement/NfcManagement.jsx`: giao diện web tải app

## Chuẩn build

Khuyến nghị máy build:

- Node `20 LTS`
- npm đi kèm Node 20
- Windows 10/11

Nếu build Windows lần đầu và native module bị rebuild:

- cài Python 3.11
- cài Visual Studio Build Tools 2022

## Build Windows installer

```bash
cd nfc-bridge-app
npm install
npm run dist:win
```

Artifact sẽ nằm trong:

```txt
nfc-bridge-app/release/
```

## Chuẩn tên file phát hành trên web

Để frontend admin dùng link ổn định, sau khi build xong hãy chép file vào:

```txt
frontend/public/downloads/slib-nfc-bridge-app/
```

và đổi tên theo quy ước:

- `SLIB-NFC-Bridge-Setup.exe`
- `SLIB-NFC-Bridge.dmg`

## Deploy frontend sau khi cập nhật bộ cài

Sau khi chép file vào `frontend/public/downloads/slib-nfc-bridge-app/`, build/deploy lại frontend như bình thường.

## Workflow tự động đã có

Repo đã có workflow:

```txt
.github/workflows/nfc_bridge_release.yml
```

Workflow này sẽ:

1. Build installer Windows từ `nfc-bridge-app`
2. Build DMG macOS từ `nfc-bridge-app`
3. Upload artifact vào GitHub Actions
4. Nếu bật `deploy_to_vm`, tự chép file lên:

```txt
/var/www/slib/downloads/slib-nfc-bridge-app/
```

Như vậy team không cần copy thủ công lên VM nữa.

## Lưu ý về các workflow deploy frontend hiện tại

Các workflow deploy VM đã được vá để **giữ lại installer NFC Bridge** trong thư mục downloads khi frontend deploy mới.

Nếu không có bước này, `rsync --delete` có thể xóa mất file `.exe/.dmg` đã upload trước đó.

## Hướng dẫn cho máy mới

1. Vào `Admin > Quản lý NFC Tag`.
2. Bấm `Tải app Windows`.
3. Chạy file cài đặt.
4. Cắm đầu đọc `ACR122U`.
5. Bấm `Mở công cụ NFC`.
6. Bấm `Kiểm tra kết nối`.
7. Khi trạng thái là `Sẵn sàng`, có thể quét/gán NFC UID.

## Khi nào dùng source zip?

Chỉ dùng `slib-nfc-bridge.zip` khi:

- chưa có installer mới
- cần debug local
- máy dev cần chạy bridge thủ công

Flow này không phải lựa chọn mặc định cho máy mới.
