SLIB NFC Bridge

Mục đích:
- Công cụ này chạy trên máy đang cắm đầu đọc ACR122U.
- Web admin sẽ gọi bridge local ở http://127.0.0.1:5050 để quét thẻ NFC.

Yêu cầu:
- Node.js 18 trở lên
- Đầu đọc ACR122U
- Driver ACR122U nếu dùng Windows

Cách cài:
1. Giải nén file zip này.
2. Cắm đầu đọc ACR122U vào máy.
3. Chạy file start-nfc-bridge.bat trên Windows
   hoặc start-nfc-bridge.command trên macOS/Linux.
4. Quay lại trang Quản lý NFC của SLIB và bấm "Kiểm tra kết nối".

Nếu máy chưa có Node.js:
- Tải tại: https://nodejs.org/en/download

Nếu bridge đã chạy đúng:
- Truy cập http://127.0.0.1:5050/health
- readerConnected = true nghĩa là đầu đọc đã sẵn sàng

Lưu ý:
- Mỗi máy cần quét NFC phải tự chạy bridge riêng.
- Bridge này không cần chạy trên VPS.
