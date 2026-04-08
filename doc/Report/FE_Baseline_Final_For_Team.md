# FE Baseline Final For Team

Ngày chốt đề xuất: 08/04/2026

## Mục tiêu

Tài liệu này dùng để chốt nội bộ với team về:

1. Bộ mã chức năng chuẩn vẫn là `FE-01` đến `FE-127`
2. Những FE cần đổi tên để khớp hệ thống hiện tại
3. Những FE mới cần bổ sung cho các tính năng đã phát sinh và đang chạy thật

---

## 1. Nguyên tắc chốt

- Giữ nguyên toàn bộ mã `FE-01..127`
- Không đổi số FE cũ
- Chỉ chỉnh tên FE cũ nếu mô tả hiện tại không còn khớp nghiệp vụ thực tế
- Các tính năng mới phát sinh sau này sẽ được bổ sung tiếp từ `FE-128` trở đi

---

## 2. Các FE cũ cần chỉnh tên

### FE-73

- Tên cũ: `Check-in/Check-out library via QR code`
- Tên đề xuất: `Check-in/Check-out library via kiosk QR code`
- Lý do: hệ thống hiện tại đang dùng luồng QR của kiosk, không phải QR tự do chung chung

### FE-81

- Tên cũ: `Create complaint`
- Tên đề xuất: `Create violation complaint/appeal`
- Lý do: nghiệp vụ hiện tại đang bám vào khiếu nại/kháng cáo vi phạm

### FE-82

- Tên cũ: `View history of sending complaint`
- Tên đề xuất: `View history of violation complaints/appeals`

### FE-83

- Tên cũ: `View list of complaints`
- Tên đề xuất: `View list of violation complaints/appeals`

### FE-84

- Tên cũ: `View complaint details`
- Tên đề xuất: `View violation complaint/appeal details`

### FE-85

- Tên cũ: `Verify complaint`
- Tên đề xuất: `Process violation complaint/appeal`

### FE-120

- Tên cũ: `Response to user manually`
- Tên đề xuất: `Respond to support request manually`
- Lý do: luồng hiện tại đang gắn với support request, không phải mọi loại phản hồi thủ công chung chung

### FE-121

- Tên cũ: `View general analytics dashboard`
- Tên đề xuất: `View analytics dashboard`
- Lý do: dashboard hiện tại là dashboard thống kê tổng hợp, có cả AI analytics và panel mở rộng

---

## 3. Các FE cũ không cần đổi tên

Các FE còn lại trong bộ `FE-01..127` có thể tiếp tục giữ nguyên như danh sách đã chốt trước đó.

---

## 4. Các tính năng mới nên thêm FE riêng

Đây là các tính năng đã có trong hệ thống hoặc đã triển khai xong, nhưng chưa có mã FE riêng trong bộ cũ.

### FE-128

- Tên đề xuất: `Leave seat via NFC`
- Mô tả: sinh viên quét NFC lần 2 để rời ghế và kết thúc phiên sử dụng ghế

### FE-129

- Tên đề xuất: `Release occupied seat by librarian`
- Mô tả: thủ thư trả chỗ ngồi từ web khi ghế đang ở trạng thái `CONFIRMED`

### FE-130

- Tên đề xuất: `View actual seat end time`
- Mô tả: hiển thị phân biệt giữa rời ghế sớm thủ công và kết thúc tự động khi hết giờ

### FE-131

- Tên đề xuất: `View booking restriction status by reputation`
- Mô tả: sinh viên xem trạng thái hạn chế đặt chỗ theo điểm uy tín và thời gian còn lại nếu đang bị khóa

### FE-132

- Tên đề xuất: `View AI prioritized students`
- Mô tả: thủ thư hoặc admin xem danh sách sinh viên cần lưu ý theo risk score trên dashboard AI

### FE-133

- Tên đề xuất: `Send warning to student from AI analytics dashboard`
- Mô tả: gửi cảnh báo hoặc nhắc nhở trực tiếp cho sinh viên từ panel AI trên dashboard

---

## 5. Danh sách FE mới đề xuất

| Mã FE | Tên chức năng |
|---|---|
| FE-128 | Leave seat via NFC |
| FE-129 | Release occupied seat by librarian |
| FE-130 | View actual seat end time |
| FE-131 | View booking restriction status by reputation |
| FE-132 | View AI prioritized students |
| FE-133 | Send warning to student from AI analytics dashboard |

---

## 6. Kết luận đề xuất chốt với team

Team nên chốt theo hướng:

1. Bộ chuẩn chính thức là `FE-01..127`
2. Chỉnh tên các FE:
   - `FE-73`
   - `FE-81`
   - `FE-82`
   - `FE-83`
   - `FE-84`
   - `FE-85`
   - `FE-120`
   - `FE-121`
3. Bổ sung FE mới:
   - `FE-128`
   - `FE-129`
   - `FE-130`
   - `FE-131`
   - `FE-132`
   - `FE-133`

---

## 7. Gợi ý hành động cho team

### BA hoặc PM

- Chốt lại wording cuối cùng cho các FE đổi tên
- Chốt việc đưa `FE-128..133` vào baseline chính thức

### Backend hoặc QA

- Đồng bộ tên trong unit test report và test metadata theo baseline mới
- Giữ bộ test controller bám đúng FE code đã chốt

### Frontend và Mobile

- Chỉ đổi wording UI nếu cần đồng nhất với baseline
- Không cần đổi logic nếu nghiệp vụ hiện tại đã đúng

### Documentation

- Cập nhật SRS
- Cập nhật Function Report
- Cập nhật Unit Test Report
- Cập nhật Test Report nếu team đang dùng bộ FE này làm source of truth
