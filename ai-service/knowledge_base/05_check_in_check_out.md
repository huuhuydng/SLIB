# Hướng dẫn Check-in và Check-out tại Thư viện SLIB

## Tổng quan

Check-in là bước bắt buộc sau khi đặt chỗ thành công. Sinh viên cần xác nhận sự có mặt tại ghế đã đặt bằng cách quét mã QR hoặc chạm NFC. Check-out là bước kết thúc ca sử dụng thư viện.

## Check-in bằng mã QR

### Cách thực hiện

1. Mở ứng dụng SLIB trên điện thoại
2. Vào mục "Đặt chỗ của tôi" để xem đặt chỗ hiện tại
3. Chọn đặt chỗ cần check-in (trạng thái BOOKED)
4. Nhấn nút "Check-in" hoặc "Quét QR"
5. Đưa camera đến mã QR trên ghế hoặc trên kiosk
6. Quét mã QR thành công
7. Hệ thống xác nhận check-in, đặt chỗ chuyển sang trạng thái CONFIRMED

### Check-in qua Kiosk

Sinh viên cũng có thể check-in thông qua kiosk đặt tại sảnh thư viện:

1. Đến kiosk tại sảnh thư viện
2. Kiosk hiển thị mã QR
3. Mở ứng dụng SLIB, quét mã QR trên kiosk
4. Xác nhận check-in trên ứng dụng
5. Check-in thành công

Mã QR trên kiosk có thời hạn sử dụng 10 phút và được tạo bằng thuật toán HMAC-SHA256 để đảm bảo an toàn.

## Check-in bằng NFC (HCE - Host Card Emulation)

### Giới thiệu NFC trong SLIB

SLIB sử dụng công nghệ HCE (Host Card Emulation) cho phép điện thoại của sinh viên hoạt động như một thẻ NFC ảo. Mỗi ghế trong thư viện có gắn một đầu đọc NFC (NFC tag) với mã UID riêng biệt.

### Yêu cầu để dùng NFC

- Điện thoại có hỗ trợ NFC (phần lớn điện thoại Android hiện đại và iPhone đời mới)
- Bật tính năng NFC trên điện thoại
- Bật tính năng HCE trong cài đặt ứng dụng SLIB (trong mục UserSetting, is_hce_enabled = true)

### Cách check-in bằng NFC

1. Đảm bảo NFC trên điện thoại đã bật
2. Đảm bảo tính năng HCE đã bật trong ứng dụng SLIB
3. Mở ứng dụng SLIB (hoặc để ứng dụng chạy nền)
4. Chạm mặt sau điện thoại vào đầu đọc NFC trên ghế đã đặt
5. Điện thoại rung và hiển thị thông báo check-in thành công
6. Đặt chỗ chuyển sang trạng thái CONFIRMED

### Lợi ích của check-in NFC

- Nhanh chóng: chỉ cần chạm điện thoại, không cần mở camera
- Chính xác: xác minh đúng ghế nhờ NFC tag UID riêng biệt trên mỗi ghế
- Tiện lợi: hoạt động ngay cả khi không mở ứng dụng (HCE chạy nền)
- An toàn: NFC tag UID được lưu dưới dạng mã hoá SHA-256

### Xử lý sự cố NFC

Nếu check-in NFC không hoạt động:

- Kiểm tra NFC trên điện thoại đã bật chưa
- Kiểm tra HCE đã bật trong cài đặt ứng dụng SLIB chưa
- Thử chạm lại ở vị trí khác trên đầu đọc NFC
- Đảm bảo không có vỏ ốp điện thoại quá dày cản trở NFC
- Nếu vẫn không được, sử dụng phương thức check-in QR thay thế
- Liên hệ thủ thư nếu cần hỗ trợ thêm

## Thời gian check-in

### Thời hạn check-in

- Sinh viên có 15 phút kể từ thời điểm bắt đầu ca để check-in
- Ví dụ: Nếu đặt ca 09:00 - 10:00, sinh viên cần check-in trước 09:15
- Sau 15 phút không check-in, đặt chỗ tự động chuyển sang EXPIRED

### Check-in sớm

Sinh viên có thể check-in trước giờ bắt đầu ca một khoảng thời gian ngắn. Hệ thống ghi nhận đây là CHECKIN_EARLY trong lịch sử hành vi.

### Check-in muộn

Nếu check-in sau thời điểm bắt đầu ca nhưng trước mốc 15 phút, hệ thống ghi nhận là CHECKIN_LATE. Sinh viên vẫn check-in thành công nhưng nên cố gắng đến đúng giờ.

### Hậu quả không check-in

Nếu không check-in trong 15 phút:

- Đặt chỗ tự động bị huỷ (trạng thái EXPIRED)
- Ghế trở lại trạng thái AVAILABLE cho sinh viên khác
- Sinh viên bị trừ 10 điểm uy tín (quy tắc NO_SHOW)
- Hệ thống ghi nhận hành vi BOOKING_NO_SHOW và BOOKING_EXPIRED

## Check-out

### Check-out thủ công

Khi muốn rời thư viện trước khi hết giờ, sinh viên có thể check-out thủ công:

1. Mở ứng dụng SLIB
2. Vào mục "Đặt chỗ của tôi"
3. Chọn đặt chỗ đang sử dụng (trạng thái CONFIRMED)
4. Nhấn nút "Check-out" hoặc "Trả chỗ"
5. Xác nhận check-out
6. Đặt chỗ chuyển sang COMPLETED
7. Ghế trở lại AVAILABLE

### Check-out tự động

Hệ thống tự động check-out trong các trường hợp:

- Hết thời gian ca đã đặt: Khi đến giờ kết thúc ca (end_time), hệ thống tự động check-out
- Rời ghế quá lâu: Nếu sinh viên rời khỏi ghế (không phát hiện sự có mặt) quá 30 phút, hệ thống có thể tự động huỷ đặt chỗ

### Nhắc nhở check-out

Trước khi hết giờ sử dụng, hệ thống gửi thông báo nhắc nhở để sinh viên chuẩn bị. Sinh viên nhận được cảnh báo sắp hết giờ qua thông báo đẩy trên điện thoại.

## Xử lý khi quên check-out

Nếu sinh viên quên check-out (rời thư viện mà không check-out):

### Trường hợp 1: Hết giờ ca

Hệ thống tự động check-out khi hết thời gian ca. Trong trường hợp này, sinh viên không bị ảnh hưởng gì nếu rời đi đúng giờ hoặc trước giờ kết thúc.

### Trường hợp 2: Check-out muộn

Nếu sinh viên vẫn ở lại thư viện sau giờ kết thúc ca mà không check-out, hệ thống ghi nhận là LATE_CHECKOUT và trừ 5 điểm uy tín. Sinh viên nên check-out đúng giờ hoặc đặt thêm ca tiếp theo nếu cần ở lại.

### Trường hợp 3: Rời đi giữa chừng không check-out

Nếu sinh viên rời khỏi ghế giữa chừng (phát hiện qua hệ thống theo dõi), hệ thống sẽ:

1. Gửi thông báo nhắc nhở quay lại
2. Chờ 30 phút
3. Nếu sinh viên không quay lại, tự động huỷ đặt chỗ

## Hệ thống cổng vào/ra (Entry/Exit Gate)

Thư viện SLIB trang bị hệ thống cổng NFC tại lối vào và lối ra:

### Cổng vào (Entry Gate)

- Thiết bị đọc NFC tại cổng vào thư viện
- Ghi nhận thời gian sinh viên vào thư viện (access log)

### Cổng ra (Exit Gate)

- Thiết bị đọc NFC tại cổng ra thư viện
- Ghi nhận thời gian sinh viên rời thư viện

### Đầu đọc ghế (Seat Reader)

- Thiết bị NFC gắn tại mỗi ghế
- Dùng để check-in xác nhận ngồi đúng ghế
- Mỗi đầu đọc có mã UID riêng được mã hoá

Tất cả thiết bị NFC đều có trạng thái hoạt động (ACTIVE, INACTIVE, MAINTENANCE) và được quản lý bởi Admin.

## Lưu ý quan trọng

1. Luôn mang theo điện thoại có cài ứng dụng SLIB khi đến thư viện
2. Đảm bảo pin điện thoại đủ để thực hiện check-in
3. Nếu điện thoại hết pin, liên hệ thủ thư để được hỗ trợ check-in thủ công
4. Bật thông báo nhắc nhở check-in và cảnh báo hết giờ
5. Check-in ngay khi đến ghế, không đợi đến phút cuối
6. Check-out khi rời thư viện để trả ghế cho sinh viên khác
7. Nếu gặp sự cố kỹ thuật khi check-in, liên hệ thủ thư ngay lập tức
