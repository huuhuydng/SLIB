# Tài khoản và Bảo mật trong Thư viện SLIB

## Đăng ký và Đăng nhập

### Đăng nhập bằng Google (FPT)

Phương thức đăng nhập chính của SLIB dành cho sinh viên là Google OAuth, giới hạn cho tài khoản email FPT (đuôi @fpt.edu.vn):

1. Mở ứng dụng SLIB trên điện thoại
2. Nhấn nút "Đăng nhập bằng Google"
3. Chọn tài khoản Google có email @fpt.edu.vn
4. Xác nhận quyền truy cập
5. Đăng nhập thành công

Lưu ý quan trọng:

- Chỉ tài khoản email có đuôi @fpt.edu.vn mới được phép đăng nhập
- Email cá nhân (Gmail thường, Yahoo, v.v.) không được hỗ trợ
- Tài khoản sinh viên phải được quản trị viên tạo sẵn trong hệ thống trước khi đăng nhập lần đầu

### Đăng nhập bằng tài khoản username/password

Ngoài Google OAuth, sinh viên cũng có thể đăng nhập bằng tài khoản username và mật khẩu do hệ thống cấp:

1. Mở ứng dụng SLIB
2. Chọn "Đăng nhập bằng tài khoản"
3. Nhập username (mã sinh viên) và mật khẩu
4. Nhấn "Đăng nhập"

Tài khoản username/password được quản trị viên tạo khi import danh sách sinh viên. Mật khẩu mặc định được cung cấp khi tạo tài khoản.

## Thông tin tài khoản

### Hồ sơ cá nhân (Profile)

Sinh viên có thể xem và quản lý các thông tin cá nhân sau:

- Mã sinh viên (user_code): Mã định danh, không thể thay đổi
- Họ và tên (full_name): Tên đầy đủ của sinh viên
- Email: Email FPT, không thể thay đổi
- Ngày sinh (dob): Ngày sinh
- Số điện thoại (phone): Số điện thoại liên hệ
- Ảnh đại diện (avatar): Có thể thay đổi bằng cách tải ảnh mới lên
- Điểm uy tín: Điểm uy tín hiện tại
- Vai trò: STUDENT (sinh viên)

### Cách cập nhật thông tin cá nhân

1. Mở ứng dụng SLIB
2. Vào mục "Tài khoản" hoặc "Hồ sơ cá nhân"
3. Chỉnh sửa các thông tin cho phép
4. Nhấn "Lưu thay đổi"

Lưu ý: Một số thông tin như mã sinh viên, email, vai trò không thể thay đổi bởi sinh viên.

### Ảnh đại diện

Sinh viên có thể thay đổi ảnh đại diện:

1. Vào trang hồ sơ cá nhân
2. Nhấn vào ảnh đại diện hiện tại
3. Chọn ảnh mới từ thư viện ảnh hoặc chụp ảnh mới
4. Cắt và điều chỉnh ảnh
5. Xác nhận thay đổi

Ảnh đại diện được lưu trữ trên hệ thống Cloudinary đảm bảo chất lượng và tốc độ tải.

## Đổi mật khẩu

### Đổi mật khẩu trong ứng dụng

Sinh viên sử dụng đăng nhập bằng username/password có thể đổi mật khẩu:

1. Mở ứng dụng SLIB
2. Vào mục "Cài đặt" hoặc "Tài khoản"
3. Chọn "Đổi mật khẩu"
4. Nhập mật khẩu hiện tại
5. Nhập mật khẩu mới (tối thiểu 8 ký tự)
6. Xác nhận mật khẩu mới
7. Nhấn "Đổi mật khẩu"

### Quên mật khẩu

Nếu quên mật khẩu:

1. Tại màn hình đăng nhập, chọn "Quên mật khẩu"
2. Nhập email FPT đã đăng ký
3. Hệ thống gửi mã OTP đến email
4. Nhập mã OTP để xác thực
5. Đặt mật khẩu mới
6. Đăng nhập với mật khẩu mới

Lưu ý: Nếu đăng nhập bằng Google OAuth, bạn không cần mật khẩu SLIB. Mật khẩu Google được quản lý riêng bởi Google.

## Cài đặt thông báo

### Các loại thông báo

Sinh viên có thể tuỳ chỉnh nhận hoặc tắt từng loại thông báo:

#### Thông báo đặt chỗ (notify_booking)

- Thông báo khi đặt chỗ thành công
- Thông báo khi đặt chỗ bị huỷ
- Mặc định: Bật

#### Thông báo nhắc nhở (notify_reminder)

- Nhắc nhở check-in trước khi hết thời gian
- Cảnh báo sắp hết giờ sử dụng
- Mặc định: Bật

#### Thông báo tin tức (notify_news)

- Tin tức mới từ thư viện
- Thông báo sự kiện, bảo trì
- Mặc định: Bật

### Cách thay đổi cài đặt thông báo

1. Mở ứng dụng SLIB
2. Vào "Cài đặt"
3. Chọn "Thông báo"
4. Bật/tắt từng loại thông báo theo nhu cầu
5. Thay đổi được lưu tự động

### Thông báo đẩy (Push Notification)

SLIB sử dụng Firebase Cloud Messaging (FCM) để gửi thông báo đẩy đến điện thoại sinh viên. Để nhận thông báo đẩy:

- Cho phép ứng dụng SLIB gửi thông báo trong cài đặt điện thoại
- Đảm bảo ứng dụng được phép chạy nền
- Kiểm tra kết nối internet

## Cài đặt ứng dụng

### Ngôn ngữ

Ứng dụng SLIB hỗ trợ tiếng Việt (mặc định). Sinh viên có thể thay đổi ngôn ngữ trong phần cài đặt nếu hệ thống hỗ trợ nhiều ngôn ngữ.

### Giao diện

Ứng dụng hỗ trợ hai chế độ giao diện:

- Sáng (Light mode): Giao diện nền trắng, mặc định
- Tối (Dark mode): Giao diện nền tối, dễ nhìn trong điều kiện thiếu sáng

Thay đổi trong: Cài đặt -> Giao diện -> Chọn Light/Dark

### AI gợi ý

Sinh viên có thể bật/tắt tính năng AI gợi ý (is_ai_recommend_enabled):

- Bật: AI sẽ gợi ý khu vực và khung giờ phù hợp dựa trên thói quen sử dụng
- Tắt: Không hiển thị gợi ý AI

### Nhắc nhở đặt chỗ

Tính năng nhắc nhở đặt chỗ (is_booking_remind_enabled):

- Bật: Nhận nhắc nhở trước khi ca bắt đầu
- Tắt: Không nhắc nhở

## NFC Card (HCE)

### Giới thiệu

Tính năng HCE (Host Card Emulation) cho phép điện thoại của sinh viên hoạt động như một thẻ NFC ảo để check-in tại thư viện.

### Bật/Tắt NFC HCE

1. Mở ứng dụng SLIB
2. Vào "Cài đặt"
3. Chọn "NFC / HCE"
4. Bật hoặc tắt tính năng HCE

Khi HCE được bật:

- Điện thoại có thể hoạt động như thẻ NFC
- Check-in bằng cách chạm điện thoại vào đầu đọc NFC tại ghế
- Hoạt động ngay cả khi ứng dụng chạy nền

### Yêu cầu kỹ thuật

- Điện thoại phải hỗ trợ NFC
- NFC phải được bật trong cài đặt điện thoại
- Ứng dụng SLIB phải được cấp quyền NFC

### Lưu ý bảo mật NFC

- Mỗi điện thoại có một mã HCE riêng biệt
- Mã HCE được mã hoá để đảm bảo an toàn
- Không chia sẻ thông tin NFC với người khác
- Nếu mất điện thoại, liên hệ thủ thư để vô hiệu hoá HCE ngay

## Bảo mật tài khoản

### Xác thực JWT

Hệ thống SLIB sử dụng JWT (JSON Web Token) để xác thực:

- Access Token: Có hiệu lực 1 giờ, dùng cho các thao tác thông thường
- Refresh Token: Có hiệu lực 7 ngày, dùng để gia hạn access token

Khi access token hết hạn, ứng dụng tự động sử dụng refresh token để lấy access token mới mà không cần đăng nhập lại.

### Bảo vệ tài khoản

Để bảo vệ tài khoản SLIB:

1. Không chia sẻ mật khẩu với người khác
2. Đăng xuất khi sử dụng thiết bị công cộng
3. Đổi mật khẩu định kỳ (nếu dùng username/password)
4. Bật xác thực 2 lớp cho tài khoản Google (quản lý bởi Google)
5. Liên hệ thủ thư ngay nếu phát hiện tài khoản bị truy cập trái phép

### Đăng xuất

Để đăng xuất khỏi ứng dụng SLIB:

1. Mở ứng dụng SLIB
2. Vào "Cài đặt" hoặc "Tài khoản"
3. Cuộn xuống và chọn "Đăng xuất"
4. Xác nhận đăng xuất

Khi đăng xuất, refresh token sẽ bị thu hồi và thiết bị sẽ không nhận thông báo đẩy cho đến khi đăng nhập lại.

## Quản lý thiết bị

### Thông báo đẩy và thiết bị

Mỗi tài khoản liên kết với một device token để nhận thông báo đẩy. Khi đăng nhập trên thiết bị mới, device token sẽ được cập nhật tự động.

### Vấn đề đăng nhập nhiều thiết bị

Hệ thống hỗ trợ đăng nhập trên một thiết bị tại một thời điểm. Nếu đăng nhập trên thiết bị mới, thiết bị cũ sẽ cần đăng nhập lại.
