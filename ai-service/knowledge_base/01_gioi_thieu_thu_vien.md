# Giới thiệu Thư viện Thông minh SLIB

## Tổng quan

Thư viện Thông minh SLIB (Smart Library) là hệ thống quản lý thư viện thông minh dành cho sinh viên Đại học FPT. SLIB được xây dựng nhằm mang đến trải nghiệm sử dụng thư viện hiện đại, tiện lợi và hiệu quả cho toàn bộ sinh viên trong trường.

Hệ thống SLIB tích hợp nhiều công nghệ tiên tiến bao gồm trí tuệ nhân tạo (AI), công nghệ NFC (Near Field Communication), mã QR, và hệ thống thông báo đẩy theo thời gian thực để giúp sinh viên có thể đặt chỗ ngồi, check-in, và sử dụng thư viện một cách thuận tiện nhất.

## Giờ hoạt động

Thư viện SLIB hoạt động theo lịch sau:

- Giờ mở cửa: 07:00 sáng
- Giờ đóng cửa: 21:00 tối
- Ngày làm việc: Thứ Hai đến Thứ Bảy (không mở cửa Chủ Nhật)
- Mỗi ca học tại thư viện kéo dài 60 phút (1 tiếng)

Lưu ý: Giờ hoạt động có thể thay đổi theo quyết định của Ban quản lý thư viện. Trong trường hợp thư viện tạm đóng cửa (do sự kiện đặc biệt, bảo trì, hoặc lý do khác), sinh viên sẽ nhận được thông báo trên ứng dụng kèm theo lý do cụ thể.

## Các tính năng chính

### 1. Đặt chỗ trực tuyến

Sinh viên có thể đặt chỗ ngồi tại thư viện hoàn toàn trực tuyến thông qua ứng dụng di động SLIB. Hệ thống cho phép:

- Xem sơ đồ chỗ ngồi theo thời gian thực
- Chọn khu vực và ghế phù hợp
- Đặt trước tối đa 14 ngày
- Đặt tối đa 3 lượt mỗi ngày
- Tổng thời gian đặt tối đa 4 giờ mỗi ngày

### 2. Check-in thông minh bằng NFC và QR

Sau khi đặt chỗ thành công, sinh viên cần xác nhận sự có mặt tại thư viện bằng một trong hai cách:

- Quét mã QR tại kiosk hoặc trên ghế
- Chạm NFC trên điện thoại vào đầu đọc NFC tại ghế (công nghệ HCE - Host Card Emulation)

Sinh viên có 15 phút kể từ thời điểm bắt đầu ca để check-in. Nếu không check-in trong thời gian này, đặt chỗ sẽ tự động bị huỷ và sinh viên sẽ bị trừ điểm uy tín.

### 3. Trợ lý AI thông minh

SLIB tích hợp chatbot AI thông minh giúp sinh viên:

- Trả lời các câu hỏi thường gặp về thư viện
- Hướng dẫn cách đặt chỗ, check-in, check-out
- Gợi ý khu vực phù hợp dựa trên nhu cầu
- Hỗ trợ giải đáp thắc mắc về điểm uy tín
- Chuyển tiếp đến thủ thư khi cần hỗ trợ chuyên sâu

### 4. Hỗ trợ thủ thư trực tuyến

Khi chatbot AI không thể giải quyết vấn đề, sinh viên có thể yêu cầu được kết nối trực tiếp với thủ thư qua hệ thống chat trực tuyến. Thủ thư sẽ hỗ trợ giải quyết các vấn đề phức tạp hơn.

### 5. Hệ thống điểm uy tín

Mỗi sinh viên có một điểm uy tín bắt đầu từ 100 điểm. Điểm uy tín phản ánh mức độ tuân thủ nội quy thư viện. Sinh viên được cộng điểm khi check-in đúng giờ và bị trừ điểm khi vi phạm. Điểm uy tín thấp có thể dẫn đến hạn chế quyền đặt chỗ.

### 6. Hệ thống thông báo

Sinh viên nhận thông báo đẩy (push notification) về:

- Đặt chỗ thành công
- Nhắc nhở check-in trước khi hết thời gian
- Cảnh báo sắp hết giờ sử dụng
- Thông báo vi phạm và trừ điểm uy tín
- Tin tức và thông báo từ thư viện

Sinh viên có thể tuỳ chỉnh nhận hoặc tắt từng loại thông báo trong phần cài đặt.

### 7. Đặt chỗ qua Kiosk

Ngoài ứng dụng di động, thư viện còn trang bị các kiosk tại sảnh để sinh viên có thể:

- Xem sơ đồ thư viện và chỗ trống
- Đặt chỗ nhanh bằng cách quét mã QR trên kiosk
- Kiểm tra trạng thái đặt chỗ hiện tại

### 8. Tin tức và thông báo

Thư viện cập nhật tin tức, sự kiện, và thông báo quan trọng trực tiếp trên ứng dụng. Sinh viên có thể theo dõi:

- Lịch hoạt động đặc biệt
- Thông báo bảo trì
- Sách mới về thư viện
- Sự kiện và chương trình tại thư viện

## Đối tượng sử dụng

- Sinh viên FPT: Sử dụng ứng dụng mobile để đặt chỗ, check-in, nhận thông báo, chat AI
- Thủ thư (Librarian): Quản lý chỗ ngồi, hỗ trợ sinh viên, xử lý vi phạm qua web portal
- Quản trị viên (Admin): Cấu hình hệ thống, quản lý người dùng, theo dõi thống kê

## Yêu cầu sử dụng

Để sử dụng SLIB, sinh viên cần:

- Tài khoản email FPT (đuôi @fpt.edu.vn)
- Điện thoại thông minh có cài ứng dụng SLIB
- Kết nối internet
- (Tuỳ chọn) Điện thoại hỗ trợ NFC để sử dụng tính năng check-in NFC

## Liên hệ hỗ trợ

Nếu cần hỗ trợ, sinh viên có thể:

- Chat với trợ lý AI trên ứng dụng SLIB
- Yêu cầu kết nối với thủ thư qua chat
- Gửi yêu cầu hỗ trợ (support request) trên ứng dụng
- Gửi phản hồi (feedback) sau mỗi lần sử dụng thư viện
- Liên hệ email: slib.system@gmail.com
