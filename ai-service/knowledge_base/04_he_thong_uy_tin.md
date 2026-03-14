# Hệ thống điểm uy tín SLIB

## Tổng quan

Hệ thống điểm uy tín là một phần quan trọng của thư viện SLIB, nhằm khuyến khích sinh viên tuân thủ nội quy và sử dụng thư viện có trách nhiệm. Mỗi sinh viên có một điểm uy tín phản ánh mức độ tuân thủ quy định của thư viện.

## Điểm uy tín mặc định

Khi sinh viên đăng ký tài khoản SLIB lần đầu, điểm uy tín được khởi tạo ở mức 100 điểm. Đây là mức điểm chuẩn, phản ánh một sinh viên sử dụng thư viện bình thường, tuân thủ đầy đủ nội quy.

## Cách xem điểm uy tín

Sinh viên có thể xem điểm uy tín hiện tại của mình tại:

1. Trang hồ sơ cá nhân (Profile) trên ứng dụng SLIB
2. Phần thông tin sinh viên trong mục cài đặt

Ngoài điểm uy tín, sinh viên còn có thể xem:

- Tổng số giờ học tại thư viện (total_study_hours)
- Số lần vi phạm (violation_count)
- Lịch sử thay đổi điểm (tăng/giảm) kèm lý do

## Quy tắc cộng điểm (Thưởng)

### Quét NFC đúng giờ (CHECK_IN_BONUS): +2 điểm

Khi sinh viên check-in (quét NFC hoặc QR) đúng giờ theo đặt chỗ, hệ thống tự động cộng 2 điểm uy tín. Đây là cách khuyến khích sinh viên đến đúng giờ và xác nhận sự có mặt.

### Tuần hoàn hảo (WEEKLY_PERFECT): +5 điểm

Nếu trong một tuần sinh viên không có bất kỳ vi phạm nào, hệ thống sẽ tự động thưởng 5 điểm uy tín vào cuối tuần. Đây là phần thưởng dành cho những sinh viên sử dụng thư viện có trách nhiệm.

## Quy tắc trừ điểm (Vi phạm)

### Không quét NFC - NO_SHOW: -10 điểm

Đây là vi phạm nghiêm trọng nhất. Khi sinh viên đặt chỗ nhưng không đến check-in (quét NFC hoặc QR) trong thời gian quy định (15 phút kể từ thời điểm bắt đầu ca), hệ thống tự động trừ 10 điểm uy tín.

Tình huống này xảy ra khi:

- Sinh viên quên đã đặt chỗ
- Sinh viên bận việc đột xuất nhưng không huỷ đặt chỗ trước
- Sinh viên đến muộn quá 15 phút

Cách tránh: Nếu không thể đến, hãy huỷ đặt chỗ trước khi ca bắt đầu. Huỷ trước sẽ không bị trừ điểm.

### Trả chỗ muộn - LATE_CHECKOUT: -5 điểm

Khi sinh viên trả ghế muộn hơn thời gian kết thúc ca đã đặt, hệ thống trừ 5 điểm uy tín. Việc trả chỗ muộn ảnh hưởng đến sinh viên đặt ca tiếp theo.

### Gây ồn ào - NOISE_VIOLATION: -10 điểm

Vi phạm quy định về tiếng ồn trong thư viện. Thư viện là nơi học tập yên tĩnh, sinh viên cần giữ trật tự.

### Ăn uống trong thư viện - FOOD_DRINK: -8 điểm

Mang đồ ăn hoặc nước uống (trừ nước lọc) vào khu vực cấm. Thư viện có khu vực riêng cho ăn uống, sinh viên không được ăn uống tại bàn học.

### Sử dụng điện thoại gây ồn - PHONE_NOISE: -5 điểm

Nghe gọi điện thoại trong khu vực yên tĩnh. Nếu cần nghe gọi, sinh viên nên ra ngoài khu vực đọc sách.

### Ngủ trong thư viện - SLEEPING: -5 điểm

Ngủ tại bàn học quá 30 phút. Thư viện là nơi học tập, không phải nơi ngủ. Nếu mệt, sinh viên nên check-out và về nghỉ ngơi.

### Gác chân lên ghế/bàn - FEET_ON_SEAT: -5 điểm

Gác chân lên ghế hoặc bàn học. Đây là hành vi không phù hợp trong môi trường học tập chung.

### Sử dụng ghế không đúng - UNAUTHORIZED_SEAT: -8 điểm

Ngồi ghế không đúng theo đặt chỗ. Sinh viên phải ngồi đúng ghế mình đã đặt. Nếu muốn đổi ghế, cần huỷ đặt chỗ cũ và đặt lại ghế mới.

### Để đồ giữ chỗ - LEFT_BELONGINGS: -5 điểm

Để đồ đạc giữ chỗ khi không có mặt tại ghế. Ghế được đặt cho từng ca cụ thể, sinh viên không được để đồ chiếm chỗ khi đi ra ngoài.

### Vi phạm khác - OTHER_VIOLATION: -5 điểm

Các vi phạm khác do thủ thư ghi nhận trong quá trình quản lý thư viện.

## Hậu quả khi điểm uy tín thấp

### Hạn chế đặt chỗ

Admin có thể thiết lập ngưỡng điểm uy tín tối thiểu để đặt chỗ. Khi điểm uy tín của sinh viên thấp hơn ngưỡng này, sinh viên sẽ không thể đặt chỗ cho đến khi cải thiện điểm.

Ví dụ: Nếu Admin thiết lập ngưỡng là 30 điểm, sinh viên có điểm uy tín dưới 30 sẽ không được phép đặt chỗ.

### Cảnh báo

Khi điểm uy tín giảm xuống mức thấp, sinh viên sẽ nhận được thông báo cảnh báo trên ứng dụng.

### Ghi nhận vi phạm

Mỗi lần bị trừ điểm, hệ thống ghi nhận thành một bản ghi hành vi (student behavior) bao gồm:

- Loại hành vi (vi phạm cụ thể)
- Mô tả chi tiết
- Số điểm bị trừ
- Thời gian xảy ra
- Đặt chỗ liên quan (nếu có)
- Ghế và khu vực liên quan (nếu có)

Lịch sử vi phạm được lưu trữ và thủ thư có thể xem xét khi cần.

## Cách cải thiện điểm uy tín

### 1. Check-in đúng giờ

Mỗi lần check-in đúng giờ sẽ được cộng 2 điểm. Đây là cách đơn giản và hiệu quả nhất để cải thiện điểm.

### 2. Giữ tuần hoàn hảo

Không vi phạm bất kỳ quy định nào trong một tuần để nhận thưởng 5 điểm vào cuối tuần.

### 3. Tuân thủ nội quy

- Luôn ngồi đúng ghế đã đặt
- Giữ trật tự, không gây ồn ào
- Không mang đồ ăn vào khu vực cấm
- Check-out đúng giờ
- Huỷ đặt chỗ nếu không thể đến

### 4. Khiếu nại khi bị trừ oan

Nếu sinh viên cho rằng mình bị trừ điểm oan, có thể gửi khiếu nại (complaint) để yêu cầu xem xét lại. Xem thêm phần "Hỗ trợ và khiếu nại" để biết cách gửi khiếu nại.

## Hệ thống theo dõi hành vi

SLIB theo dõi toàn bộ hành vi của sinh viên tại thư viện để phục vụ việc đánh giá uy tín, bao gồm:

- Hành vi đặt chỗ: tạo, xác nhận, huỷ, không đến, hết hạn, hoàn thành
- Hành vi check-in: đúng giờ, sớm, muộn
- Hành vi check-out: đúng giờ, muộn
- Vi phạm: bị báo cáo, được xác nhận, khiếu nại
- Điểm uy tín: cộng điểm, trừ điểm

Dữ liệu hành vi này cũng được sử dụng bởi AI để phân tích xu hướng và đưa ra gợi ý phù hợp cho sinh viên.

## Lưu ý quan trọng

- Điểm uy tín không thể tăng quá mức trần do Admin thiết lập
- Điểm uy tín bị trừ tự động bởi hệ thống khi phát hiện vi phạm
- Thủ thư có thể trừ điểm thủ công khi phát hiện vi phạm trực tiếp
- Lịch sử điểm uy tín được lưu trữ vĩnh viễn
- Sinh viên có quyền xem toàn bộ lịch sử tăng/giảm điểm của mình
