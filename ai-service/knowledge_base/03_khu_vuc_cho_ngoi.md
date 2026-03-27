# Khu vực và chỗ ngồi tại Thư viện SLIB

## Cấu trúc không gian thư viện

Thư viện SLIB được tổ chức theo cấu trúc phân cấp gồm ba lớp:

- Tầng/Khu vực lớn (Area): Đơn vị không gian lớn nhất, đại diện cho một tầng hoặc một khu vực chính của thư viện
- Khu vực nhỏ (Zone): Các vùng con bên trong mỗi Area, mỗi zone có đặc điểm và tiện ích riêng
- Chỗ ngồi (Seat): Từng ghế cụ thể trong mỗi zone, được đánh mã riêng biệt

## Khu vực lớn (Area)

Mỗi Area đại diện cho một khu vực rộng trong thư viện. Mỗi Area có:

- Tên khu vực riêng biệt
- Kích thước và vị trí trên sơ đồ tổng thể
- Trạng thái hoạt động (active/inactive)
- Trạng thái khoá (locked): Khi bị khoá, sinh viên không thể đặt chỗ trong khu vực này

Sinh viên có thể xem sơ đồ tổng thể các Area trên ứng dụng mobile khi bắt đầu đặt chỗ.

## Khu vực nhỏ (Zone)

Mỗi Zone nằm trong một Area và có các đặc điểm sau:

- Tên zone (ví dụ: Zone A, Zone B, Khu yên tĩnh, Khu nhóm...)
- Mô tả zone: thông tin về tiện ích, đặc điểm không gian
- Vị trí trên sơ đồ của Area cha
- Kích thước (số hàng, số cột ghế)
- Trạng thái khoá (is_locked): Khi zone bị khoá, không thể đặt chỗ trong zone này
- Tiện ích (amenities): ổ cắm điện, đèn bàn, wifi mạnh, máy lạnh, v.v.

Mỗi zone có thể được trang bị các tiện ích (amenities) khác nhau. Sinh viên nên đọc mô tả zone để chọn nơi phù hợp với nhu cầu học tập.

## Chỗ ngồi (Seat)

### Thông tin ghế

Mỗi ghế trong thư viện SLIB có:

- Mã ghế (seat_code): Mã định danh duy nhất, ví dụ A1-01, B2-05
- Vị trí: Hàng (row) và cột (column) trong zone
- Trạng thái hoạt động (is_active): Ghế có đang sử dụng được hay không
- NFC Tag: Mỗi ghế có thể gắn thẻ NFC để phục vụ check-in bằng NFC

### Trạng thái ghế

Ghế trong thư viện SLIB có các trạng thái sau:

#### AVAILABLE (Trống)

- Ghế đang trống và sẵn sàng để đặt
- Hiển thị màu xanh lá trên sơ đồ
- Sinh viên có thể chọn ghế này để đặt chỗ

#### HOLDING (Đang giữ tạm)

- Ghế đang được một sinh viên giữ tạm thời trong quá trình đặt chỗ
- Thời gian giữ tạm là vài phút
- Hiển thị màu vàng trên sơ đồ
- Sinh viên khác không thể đặt ghế đang ở trạng thái này
- Nếu sinh viên không hoàn tất đặt chỗ trong thời gian giữ, ghế sẽ tự động trở lại AVAILABLE

#### BOOKED (Đã đặt)

- Ghế đã được đặt thành công bởi một sinh viên
- Hiển thị màu đỏ trên sơ đồ
- Sinh viên đã đặt cần check-in trong vòng 15 phút kể từ thời điểm bắt đầu ca
- Sinh viên khác không thể đặt ghế này trong khung giờ đã đặt

#### CONFIRMED (Đã xác nhận / Đang sử dụng)

- Sinh viên đã check-in thành công và đang ngồi tại ghế
- Hiển thị màu xanh dương trên sơ đồ
- Ghế đang được sử dụng

#### UNAVAILABLE (Không khả dụng)

- Ghế tạm thời không sử dụng được
- Có thể do bảo trì, hỏng hóc, hoặc bị Admin vô hiệu hoá
- Ghế bị đánh dấu is_active = false sẽ không hiển thị cho sinh viên đặt

## Cách xem sơ đồ chỗ ngồi

### Trên ứng dụng mobile

1. Mở ứng dụng SLIB
2. Chọn "Đặt chỗ"
3. Chọn ngày và khung giờ muốn đặt
4. Hệ thống hiển thị sơ đồ tổng thể các Area
5. Chọn Area muốn xem
6. Chọn Zone bên trong Area
7. Xem sơ đồ ghế với màu sắc thể hiện trạng thái
8. Chạm vào ghế trống (xanh lá) để đặt

### Trên Kiosk

Kiosk tại sảnh thư viện cũng hiển thị sơ đồ bản đồ thư viện bao gồm các zone và tình trạng ghế, giúp sinh viên có cái nhìn tổng quan trước khi đặt chỗ.

## Hướng dẫn chọn ghế phù hợp

### Nếu cần học yên tĩnh

Chọn các zone có mô tả "Khu yên tĩnh" hoặc zone ở vị trí xa cửa ra vào. Những khu vực này thường có quy định nghiêm ngặt về tiếng ồn, phù hợp cho việc đọc sách và tự học.

### Nếu cần làm việc nhóm

Chọn zone có mô tả dành cho hoạt động nhóm. Những zone này cho phép trao đổi ở mức âm lượng vừa phải.

### Nếu cần ổ cắm điện

Kiểm tra tiện ích (amenities) của zone trước khi đặt. Các zone có tiện ích "ổ cắm điện" sẽ có ổ cắm tại mỗi bàn.

### Nếu cần không gian rộng

Chọn các ghế ở hàng ngoài hoặc góc zone để có nhiều không gian hơn.

## Quy định về chỗ ngồi

### Ngồi đúng ghế đã đặt

Sinh viên phải ngồi đúng ghế đã đặt chỗ. Ngồi ghế không đúng theo đặt chỗ sẽ bị trừ 8 điểm uy tín (quy tắc UNAUTHORIZED_SEAT).

### Không để đồ giữ chỗ

Không được để đồ đạc giữ chỗ khi không có mặt. Vi phạm sẽ bị trừ 5 điểm uy tín (quy tắc LEFT_BELONGINGS).

### Giữ vệ sinh

Sinh viên có trách nhiệm giữ gìn vệ sinh khu vực ghế ngồi. Không mang đồ ăn, nước uống vào khu vực cấm.

### Không gác chân lên ghế hoặc bàn

Vi phạm sẽ bị trừ 5 điểm uy tín (quy tắc FEET_ON_SEAT).

## Báo cáo vấn đề chỗ ngồi

Nếu phát hiện ghế hỏng, bẩn, hoặc có vấn đề, sinh viên có thể:

1. Sử dụng tính năng "Báo cáo ghế" trên ứng dụng
2. Chọn loại vấn đề (ghế hỏng, bẩn, thiếu tiện ích...)
3. Mô tả chi tiết vấn đề
4. Chụp ảnh làm bằng chứng (nếu có)
5. Gửi báo cáo

Thủ thư sẽ xem xét và xử lý báo cáo. Ghế có vấn đề sẽ được đánh dấu bảo trì (UNAVAILABLE) cho đến khi được sửa chữa.

## Báo cáo vi phạm chỗ ngồi

Nếu phát hiện sinh viên khác vi phạm quy định về chỗ ngồi, bạn có thể báo cáo vi phạm:

1. Mở ứng dụng SLIB
2. Chọn "Báo cáo vi phạm"
3. Chọn ghế liên quan
4. Chọn loại vi phạm:
   - Sử dụng ghế trái phép (UNAUTHORIZED_USE)
   - Để đồ giữ chỗ (LEFT_BELONGINGS)
   - Gây ồn ào (NOISE)
   - Gác chân lên ghế/bàn (FEET_ON_SEAT)
   - Ăn uống (FOOD_DRINK)
   - Ngủ tại bàn (SLEEPING)
   - Vi phạm khác (OTHER)
5. Mô tả chi tiết
6. Đính kèm ảnh bằng chứng (nếu có)
7. Gửi báo cáo

Thủ thư sẽ xem xét và xác minh báo cáo. Nếu vi phạm được xác nhận, sinh viên vi phạm sẽ bị trừ điểm uy tín theo quy định.
