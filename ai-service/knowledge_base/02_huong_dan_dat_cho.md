# Hướng dẫn đặt chỗ ngồi tại Thư viện SLIB

## Tổng quan về đặt chỗ

Hệ thống đặt chỗ SLIB cho phép sinh viên đặt trước chỗ ngồi tại thư viện một cách thuận tiện thông qua ứng dụng di động hoặc kiosk tại thư viện. Việc đặt chỗ giúp đảm bảo sinh viên luôn có chỗ ngồi khi đến thư viện, tránh tình trạng đông đúc và phải chờ đợi.

## Quy định đặt chỗ

### Giới hạn đặt chỗ

- Số lượt đặt tối đa mỗi ngày: 3 lượt
- Số giờ đặt tối đa mỗi ngày: 4 giờ
- Thời lượng mỗi ca: 60 phút (1 tiếng)
- Số ngày có thể đặt trước: tối đa 14 ngày
- Giờ đặt chỗ: từ 07:00 đến 21:00 (theo giờ hoạt động của thư viện)
- Ngày hoạt động: Thứ Hai đến Thứ Bảy

### Điều kiện đặt chỗ

- Sinh viên phải có tài khoản SLIB đang hoạt động (active)
- Điểm uy tín phải đạt mức tối thiểu do hệ thống quy định (mặc định không giới hạn, nhưng Admin có thể thiết lập ngưỡng tối thiểu)
- Thư viện phải đang mở cửa (không trong trạng thái tạm đóng)
- Không được đặt chồng thời gian với các đặt chỗ đang hoạt động

## Quy trình đặt chỗ qua ứng dụng mobile

### Bước 1: Mở ứng dụng và chọn đặt chỗ

Mở ứng dụng SLIB trên điện thoại, đăng nhập bằng tài khoản Google FPT (@fpt.edu.vn), sau đó chọn mục "Đặt chỗ" trên màn hình chính.

### Bước 2: Chọn ngày và khung giờ

Chọn ngày muốn đến thư viện (có thể đặt trước tối đa 14 ngày). Sau đó chọn khung giờ phù hợp. Mỗi khung giờ kéo dài 60 phút. Bạn có thể chọn nhiều khung giờ liên tiếp nhau nhưng tổng không quá 4 giờ trong ngày.

### Bước 3: Chọn khu vực

Xem sơ đồ thư viện và chọn khu vực (zone) muốn ngồi. Mỗi khu vực có thông tin mô tả về tiện ích và đặc điểm để sinh viên lựa chọn phù hợp. Các khu vực bị khoá (locked) sẽ không thể chọn.

### Bước 4: Chọn ghế

Xem sơ đồ chỗ ngồi trong khu vực đã chọn. Ghế hiển thị các trạng thái bằng màu sắc khác nhau:

- Xanh lá (AVAILABLE): Ghế trống, có thể đặt
- Vàng (HOLDING): Đang được người khác giữ tạm, không thể đặt
- Đỏ (BOOKED): Đã được đặt, không thể đặt
- Xanh dương (CONFIRMED): Đã có người check-in ngồi
- Xám: Ghế không hoạt động hoặc đang bảo trì

Chọn một ghế có trạng thái AVAILABLE (trống) để đặt.

### Bước 5: Xác nhận đặt chỗ

Kiểm tra lại thông tin đặt chỗ bao gồm: ngày, giờ bắt đầu, giờ kết thúc, khu vực, mã ghế. Nhấn "Xác nhận đặt chỗ" để hoàn tất.

### Bước 6: Nhận thông báo

Sau khi đặt chỗ thành công, sinh viên sẽ nhận được thông báo xác nhận trên ứng dụng. Ghế sẽ chuyển sang trạng thái HOLDING (giữ chỗ tạm thời) trong vài phút, sau đó chuyển sang BOOKED.

## Đặt chỗ qua Kiosk

Thư viện có trang bị các kiosk tại sảnh để sinh viên đặt chỗ nhanh:

1. Đến kiosk tại sảnh thư viện
2. Mở ứng dụng SLIB trên điện thoại
3. Quét mã QR hiển thị trên kiosk bằng ứng dụng
4. Chọn ghế và xác nhận đặt chỗ trên điện thoại
5. Đặt chỗ thành công

Mã QR trên kiosk có thời hạn 10 phút, sau đó sẽ tự động tạo mã mới.

## Các trạng thái đặt chỗ (Booking Status)

Mỗi đặt chỗ sẽ trải qua các trạng thái sau:

### PROCESSING (Đang xử lý)

Trạng thái ban đầu khi sinh viên vừa tạo đặt chỗ. Hệ thống đang xử lý yêu cầu và kiểm tra tính hợp lệ.

### BOOKED (Đã đặt)

Đặt chỗ đã được xác nhận thành công. Ghế được giữ cho sinh viên. Sinh viên cần check-in trong vòng 15 phút kể từ thời điểm bắt đầu ca.

### CONFIRMED (Đã xác nhận / Đang sử dụng)

Sinh viên đã check-in thành công (quét NFC hoặc QR). Đang ngồi tại ghế và sử dụng thư viện.

### COMPLETED (Hoàn thành)

Ca sử dụng đã kết thúc bình thường. Sinh viên đã check-out hoặc hết thời gian sử dụng.

### CANCELLED (Đã huỷ)

Đặt chỗ đã bị huỷ bởi sinh viên hoặc bởi hệ thống.

### EXPIRED (Hết hạn)

Sinh viên không check-in trong thời gian quy định (15 phút), đặt chỗ tự động hết hạn. Sinh viên bị trừ điểm uy tín do không đến (no-show).

## Cách huỷ đặt chỗ

Sinh viên có thể huỷ đặt chỗ trước khi ca bắt đầu:

1. Mở ứng dụng SLIB
2. Vào mục "Đặt chỗ của tôi" hoặc "Lịch sử đặt chỗ"
3. Chọn đặt chỗ muốn huỷ (trạng thái BOOKED)
4. Nhấn "Huỷ đặt chỗ"
5. Xác nhận huỷ

Lưu ý về huỷ đặt chỗ:

- Huỷ trước khi ca bắt đầu: Không bị trừ điểm uy tín
- Không check-in và để hệ thống tự huỷ (EXPIRED): Bị trừ 10 điểm uy tín (quy tắc NO_SHOW)

## Thời gian giữ chỗ và check-in

- Khi đặt chỗ thành công, ghế sẽ được giữ cho sinh viên
- Sinh viên có 15 phút kể từ thời điểm bắt đầu ca để check-in
- Nếu không check-in trong 15 phút, đặt chỗ tự động bị huỷ (EXPIRED)
- Sau khi check-in, nếu rời khỏi ghế quá 30 phút, đặt chỗ có thể bị tự động huỷ

## Mẹo đặt chỗ hiệu quả

1. Đặt chỗ trước ít nhất 1 ngày để có nhiều lựa chọn ghế hơn
2. Chọn khu vực phù hợp với nhu cầu (yên tĩnh, nhóm, có ổ cắm điện...)
3. Nhớ check-in đúng giờ để không bị trừ điểm uy tín
4. Nếu không thể đến, hãy huỷ đặt chỗ sớm để nhường ghế cho sinh viên khác
5. Kiểm tra điểm uy tín thường xuyên để đảm bảo đủ điều kiện đặt chỗ
6. Bật thông báo nhắc nhở check-in để không quên

## Câu hỏi thường gặp về đặt chỗ

### Tôi có thể đặt chỗ cho bạn bè không?

Không. Mỗi tài khoản chỉ có thể đặt chỗ cho chính mình. Mỗi sinh viên cần tự đặt chỗ bằng tài khoản cá nhân.

### Tôi đã đặt chỗ nhưng muốn đổi sang ghế khác?

Bạn cần huỷ đặt chỗ hiện tại trước, sau đó đặt lại ghế mới. Lưu ý kiểm tra ghế mới còn trống hay không trước khi huỷ.

### Tôi có thể đặt nhiều ca liên tiếp không?

Có, bạn có thể đặt nhiều ca liên tiếp nhau miễn là tổng thời gian không vượt quá 4 giờ trong ngày và không quá 3 lượt đặt.

### Thư viện đông quá, không còn ghế trống?

Khi thư viện hết chỗ, bạn có thể:

- Thử đặt ở khung giờ khác
- Kiểm tra các khu vực khác
- Đặt trước cho ngày tiếp theo
- Liên hệ thủ thư qua chat để được hỗ trợ
