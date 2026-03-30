# SLIB IoT – Gateway Scanner

Trạm quét HCE cho hệ thống thư viện SLIB, chạy trên **Raspberry Pi 4** kết hợp **ACR122U NFC reader**.

## Phần cứng yêu cầu

- Raspberry Pi 4 (hoặc 3B+)
- ACR122U USB NFC Reader
- ILI9341 TFT Display (SPI)
- Buzzer (GPIO 17)
- LED RGB (GPIO 27/22/23)

## Cài đặt

### 1. Cài đặt thư viện Python

```bash
sudo apt update
sudo apt install -y python3-pip python3-pyscard pcscd
pip3 install requests python-dotenv Pillow adafruit-circuitpython-rgb-display RPi.GPIO
```

### 2. Cấu hình

```bash
cp .env.example .env
nano .env
```

Cập nhật các giá trị trong `.env`:

| Biến | Mô tả | Mặc định |
|------|--------|----------|
| `SLIB_API_URL` | URL backend API | `https://api.slibsystem.site` |
| `SLIB_GATE_ID` | Mã trạm quét (phải đăng ký trên admin UI trước) | `GATE_01` |
| `SLIB_GATE_API_KEY` | API key xác thực | `SLIB_SECRET_GATE_FPT_123` |
| `SLIB_HEARTBEAT_INTERVAL` | Chu kỳ gửi heartbeat (giây) | `30` |

> **Quan trọng:** Giá trị `SLIB_GATE_ID` phải trùng khớp với `deviceId` đã đăng ký trên trang quản trị (Admin UI → Trạm quét HCE → Thêm trạm quét).

### 3. Đăng ký trạm quét trên Admin UI

1. Mở Admin UI → **Trạm quét HCE**
2. Nhấn **Thêm trạm quét**
3. Nhập `Mã trạm` trùng với `SLIB_GATE_ID` (VD: `GATE_01`)
4. Chọn loại trạm và vị trí
5. Lưu → trạm sẵn sàng nhận heartbeat và check-in

### 4. Chạy thủ công

```bash
python3 slib_gate.py
```

### 5. Chạy tự động khi khởi động (systemd)

```bash
# Copy service file
sudo cp slib-gate.service.example /etc/systemd/system/slib-gate.service

# Chỉnh sửa đường dẫn nếu cần
sudo nano /etc/systemd/system/slib-gate.service

# Kích hoạt và khởi động
sudo systemctl daemon-reload
sudo systemctl enable slib-gate
sudo systemctl start slib-gate

# Kiểm tra trạng thái
sudo systemctl status slib-gate

# Xem logs
sudo journalctl -u slib-gate -f
```

## Troubleshooting

| Vấn đề | Giải pháp |
|--------|-----------|
| `No readers found` | Kiểm tra ACR122U đã cắm USB, chạy `sudo systemctl start pcscd` |
| `Heartbeat Failed` | Kiểm tra URL và API key trong `.env`, đảm bảo server online |
| `Trạm quét không tồn tại` | Đăng ký trạm trên admin UI với đúng `SLIB_GATE_ID` |
| `Trạm đang bị vô hiệu hóa` | Vào admin UI → Chuyển trạng thái trạm sang **Hoạt động** |
| Font lỗi | `sudo apt install fonts-dejavu` |
