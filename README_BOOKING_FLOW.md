# README_BOOKING_FLOW

Tài liệu này tóm tắt luồng đặt chỗ (booking) của sinh viên để demo nhanh, kèm các tình huống lỗi, vi phạm và auto-cancel theo đúng logic hệ thống hiện tại.

## 1) Mục tiêu demo

- Trình bày luồng đặt chỗ từ chọn ghế đến xác nhận chỗ ngồi bằng NFC.
- Chứng minh hệ thống tự động xử lý trạng thái và kỷ luật (auto-cancel, phạt uy tín).
- Minh họa các tình huống lỗi phổ biến để giảng viên hiểu rõ quy tắc nghiệp vụ.

## 2) Trạng thái booking và ý nghĩa

- PROCESSING: Sinh viên vừa tạo booking, đang trong 2 phút xác nhận.
- BOOKED: Sinh viên đã xác nhận, chờ check-in bằng NFC trong khung giờ.
- CONFIRMED: Sinh viên đã check-in tại ghế bằng NFC.
- COMPLETED: Kết thúc phiên học (tự động hoặc trả chỗ sớm).
- CANCEL/CANCELLED: Bị hủy bởi người dùng hoặc hệ thống.
- EXPIRED: Quá hạn check-in, bị auto-cancel và áp dụng phạt.

## 3) Luồng demo chuẩn (happy path)

1) Chọn khu vực, chọn khung giờ, chạm ghế trống.
2) Hệ thống tạo booking ở trạng thái PROCESSING và giữ ghế (HOLDING).
3) Màn hình xác nhận hiện 2 phút đếm ngược. Sinh viên bấm xác nhận.
4) Booking chuyển sang BOOKED.
5) Đến gần giờ bắt đầu (từ 15 phút trước giờ), sinh viên quét NFC tại ghế.
6) Booking chuyển sang CONFIRMED, ghi nhận đã check-in.
7) Khi rời ghế, sinh viên quét NFC lần nữa để trả chỗ, booking thành COMPLETED.

## 4) Luồng API chính (để nói khi demo)
 
- Tạo booking: POST /slib/bookings/create
- Xác nhận booking: PUT /slib/bookings/updateStatusReserv/{reservationId}?status=BOOKED
- Check-in NFC: POST /slib/bookings/confirm-nfc-uid/{reservationId}
- Trả chỗ NFC: POST /slib/bookings/leave-seat-nfc/{reservationId}
- Hủy booking: PUT /slib/bookings/cancel/{reservationId}
- Lịch sử booking: GET /slib/bookings/user/{userId}
- Booking sắp tới: GET /slib/bookings/upcoming/{userId}

## 5) Quy tắc nghiệp vụ bắt buộc nêu khi demo

- Thư viện đóng cửa: không cho đặt chỗ.
- Giới hạn lượt đặt trong ngày và tổng giờ đặt trong ngày.
- Mỗi người chỉ được đặt 1 ghế trong cùng khung giờ.
- Điểm uy tín thấp sẽ bị hạn chế đặt trước, giới hạn booking đang hoạt động, hoặc bị khóa tạm thời.
- Hủy booking của sinh viên phải trước 12 giờ so với giờ bắt đầu (trừ trạng thái PROCESSING).

## 6) Luồng lỗi và vi phạm (để demo)

### 6.1 Lỗi khi tạo booking

- Ghế đã có người giữ hoặc đặt: báo lỗi "Ghế đã được đặt hoặc đang chờ xác nhận".
- Đặt trùng khung giờ: báo lỗi chỉ được 1 ghế mỗi khung giờ.
- Vượt quá số lần đặt trong ngày hoặc tổng giờ đặt trong ngày.
- Thư viện đang tạm đóng: báo lý do đóng cửa.
- Điểm uy tín dưới ngưỡng: không cho đặt và giải thích lý do.

### 6.2 Lỗi khi xác nhận trong 2 phút

- Nếu không xác nhận trong 2 phút, hệ thống tự hủy booking PROCESSING.
- Nếu bấm hủy, booking chuyển sang CANCEL và ghế trả lại.

### 6.3 Lỗi khi check-in NFC

- Quét NFC trước thời gian cho phép (trước 15 phút): báo chưa đến giờ check-in.
- Quét sau khi hết khung giờ: báo đã hết thời gian check-in.
- NFC không khớp ghế đã đặt: báo ghế không khớp.
- Thẻ NFC chưa gán cho ghế: báo không tìm thấy ghế tương ứng.

### 6.4 Hủy booking muộn

- Nếu đã BOOKED/CONFIRMED và còn dưới 12 giờ so với giờ bắt đầu, sinh viên không được hủy.
- Thủ thư vẫn có thể hủy trước giờ bắt đầu nhưng bắt buộc nhập lý do.

## 7) Auto-cancel và xử lý vi phạm

Hệ thống chạy tự động theo chu kỳ và áp dụng các quy tắc sau:

- PROCESSING quá 2 phút: tự chuyển CANCEL.
- BOOKED không check-in sau autoCancelMinutes (theo cấu hình): chuyển EXPIRED và áp dụng phạt điểm uy tín.
- CONFIRMED đến giờ kết thúc: hệ thống nhắc rời chỗ. Sau 5 phút không xác nhận rời chỗ, tự COMPLETED và có thể áp dụng phạt nếu quá hạn.

## 8) Gợi ý kịch bản demo nhanh 5-7 phút

1) Vào sơ đồ ghế, chọn khung giờ, đặt ghế -> thấy trạng thái HOLDING.
2) Xác nhận trong 2 phút -> chuyển BOOKED.
3) Quét NFC -> chuyển CONFIRMED.
4) Quét NFC lần nữa -> COMPLETED.
5) Thử hủy muộn -> hệ thống từ chối (quy tắc 12 giờ).
6) Giải thích auto-cancel khi không check-in đúng hạn.

## 9) Ghi chú trình bày

- Nhấn mạnh hệ thống bảo vệ công bằng: giới hạn slot, điểm uy tín, auto-cancel.
- Nhấn mạnh luồng NFC giúp xác nhận sinh viên có mặt thật.
- Nêu rõ trạng thái thay đổi theo thời gian và theo hành động của sinh viên.

“Bây giờ em sẽ demo luồng đặt chỗ của sinh viên từ đầu đến cuối. 
-Đầu tiên, khi ở màn hình chính, em mở màn hình sơ đồ thư viện, sau đó chọn khu vực và khung giờ còn trống, rồi chạm vào một ghế màu xanh. Hệ thống tạo booking ở trạng thái chờ xác nhận và ghế sẽ chuyển sang trạng thái giữ chỗ để người khác không đặt trùng. 
-Ngay sau đó, em bấm Xác nhận trong 2 phút để hoàn tất, booking chuyển sang trạng thái đã đặt. Nếu không nhấn xác nhận hoặc chủ động nhấn hủy thì ghế đó sẽ trả về trạng thái bình thường để người khác đặt. 
-Đến gần giờ bắt đầu nhận ghế, em đưa điện thoại quét NFC tại ghế, hệ thống xác nhận đúng ghế và chuyển sang trạng thái đã check‑in. 
-Khi rời ghế, em quét NFC lần nữa để trả chỗ, booking hoàn thành và ghế trở lại sẵn sàng. Nếu em thử hủy sát giờ hoặc không check‑in đúng hạn, hệ thống sẽ tự từ chối hoặc auto‑cancel và áp dụng quy tắc uy tín, đảm bảo công bằng cho mọi sinh viên.”

1) “Nếu đặt ghế lúc gần hết khung giờ (vd 8h50) thì sao?”

Hệ thống vẫn cho đặt trong đúng khung giờ đó, nhưng app sẽ cảnh báo là khung giờ đã bắt đầu và chỉ còn ít phút sử dụng.
Nếu sinh viên vẫn xác nhận, booking giữ đúng khung giờ đã chọn, không tự chuyển sang khung giờ khác.
2) “Nếu đã check‑in mà không checkout thì sao?”

Khi đến giờ kết thúc, hệ thống nhắc rời chỗ.
Sau 5 phút không checkout, hệ thống tự hoàn tất (COMPLETED) và có thể xử lý kỷ luật nếu quá hạn.
3) “Nếu không xác nhận trong 2 phút thì sao?”

Booking tự hủy (CANCEL), ghế trả lại cho người khác.
4) “Nếu đã BOOKED mà không check‑in đúng hạn?”

Sau thời gian autoCancelMinutes (theo cấu hình), booking bị EXPIRED và có phạt điểm uy tín.
5) “Nếu hủy sát giờ bắt đầu?”

Sinh viên chỉ được hủy trước 12 giờ.
Nếu dưới 12 giờ, hệ thống từ chối hủy (trừ trạng thái PROCESSING).
6) “Nếu quét NFC sai ghế?”

Hệ thống báo ghế không khớp (không cho check‑in/checkout).
7) “Nếu điểm uy tín thấp?”

Có thể bị giới hạn đặt trước, giới hạn số booking đang hoạt động, hoặc bị khóa tạm thời.
8) “Nếu thư viện đóng cửa?”

Không cho đặt, kèm lý do đóng.










• Với 30 phút, anh không nên demo theo kiểu đi hết module. Nên demo theo 1 câu chuyện end-to-end, vì SLIB mạnh nhất ở chỗ: quản lý không gian thư viện theo thời gian
  thực, check-in/check-out bằng HCE/QR, AI hỗ trợ có escalation sang thủ thư, và cơ chế reputation để quản trị hành vi. Đây cũng là các điểm khác biệt rõ nhất so với
  các hệ thống tham chiếu trong Report 1.

  Nên ưu tiên 4 điểm mạnh này

  - HCE/NFC + QR check-in/check-out: đây là điểm khác biệt rất mạnh, vì Report 1 cũng nêu các hệ thống tham chiếu chưa có native HCE.
  - Bản đồ ghế realtime + booking thông minh: sinh viên thấy ghế trống, đặt chỗ, trạng thái cập nhật ngay cho thủ thư.
  - AI chatbot RAG + chuyển tiếp sang thủ thư: vừa có AI, vừa có human handoff, rất hợp để chứng minh tính thực tế.
  - Reputation + violation + analytics: cho hội đồng thấy đây không chỉ là app đặt ghế, mà là hệ sinh thái quản trị thư viện.

  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.

    Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.  Kịch bản demo 30 phút khuyến nghị

  1. 0-3 phút: Mở đầu bằng bài toán.
     Nói rất ngắn: thư viện hiện tại bị tự do ra vào, không kiểm soát chỗ ngồi, có no-show, thiếu dữ liệu vận hành. SLIB giải bằng 4 trụ cột: access control, smart
     booking, AI support, reputation governance.
  2. 3-7 phút: Demo nhanh phía Admin.
     Show 3 thứ thôi:
      - cấu hình khu vực/zone/ghế trên bản đồ thư viện
      - cấu hình rule đặt chỗ
      - cấu hình rule reputation
        Câu chốt nên là: “Hệ thống không hard-code, thư viện có thể tự vận hành và đổi chính sách.”
  3. 7-15 phút: Demo phía Sinh viên trên mobile.
     Luồng đẹp nhất:
      - đăng nhập
      - vào màn hình đặt chỗ
      - xem sơ đồ ghế realtime, filter khu vực
      - hỏi AI gợi ý chỗ ngồi phù hợp
      - bấm đặt chỗ ngay từ gợi ý AI hoặc chọn ghế trực tiếp
      - show booking card/QR/upcoming booking
        Đây là đoạn nên dành nhiều thời gian nhất, vì vừa đẹp mắt vừa dễ hiểu.
  4. 15-20 phút: Demo check-in/check-out và realtime sync.
     Luồng:
      - dùng HCE hoặc QR để check-in
      - ngay lập tức màn hình librarian/admin đổi trạng thái ghế từ BOOKED sang OCCUPIED
      - nếu có notification thì show luôn
        Đây là đoạn “wow” nhất. Nếu HCE live hơi rủi ro, nên chuẩn bị fallback bằng QR hoặc video ngắn quay sẵn.
  5. 20-25 phút: Demo AI chat + escalation.
     Làm 2 câu hỏi:
      - 1 câu AI trả lời tốt từ knowledge base, ví dụ giờ mở cửa, quy định đặt chỗ, cách check-in
      - 1 câu khó hoặc yêu cầu người thật, để hệ thống chuyển sang chat với thủ thư
        Sau đó mở màn hình Librarian:
      - thấy hàng đợi chat
      - mở hội thoại
      - dùng AI suggestion để trả lời nhanh
        Đoạn này giúp hội đồng thấy AI của anh không “làm màu”, mà có cơ chế fallback thực tế.
  6. 25-28 phút: Demo governance.
     Show thật nhanh:
      - điểm reputation của sinh viên
      - lịch sử vi phạm hoặc complaint
      - dashboard thống kê: peak hours, occupancy, khu vực dùng nhiều nhất
        Câu chốt: “SLIB không chỉ phục vụ sinh viên, mà còn giúp thư viện ra quyết định dựa trên dữ liệu.”
  7. 28-30 phút: Tổng kết.
     Chốt 3 ý:
      - quản lý thư viện theo thời gian thực
      - kiểm soát hành vi và tối ưu vận hành
      - AI hỗ trợ nhưng vẫn giữ human-in-the-loop

  Thứ tự này hợp lý vì

  - mở bằng admin config để chứng minh tính hệ thống
  - đẩy mạnh student journey để dễ nhìn, dễ cảm
  - lên đỉnh bằng HCE/realtime và AI escalation
  - kết bằng analytics + governance để tăng chiều sâu học thuật và giá trị quản trị

  Những thứ nên cắt hoặc lướt rất nhanh

  - forgot password
  - CRUD user cơ bản
  - news/announcement
  - profile/account settings
  - import file nếu không bị hỏi

  Chuẩn bị trước khi demo

  - 1 tài khoản Admin, 1 Librarian, 1 Student
  - 1 booking data đã sẵn sàng để check-in
  - 1 câu hỏi AI chắc chắn trả lời đúng
  - 1 câu hỏi để chắc chắn escalation sang thủ thư
  - 1 tình huống vi phạm/no-show đã seed sẵn để show reputation
  - fallback video cho HCE/NFC nếu live demo không ổn

  Nếu anh muốn, tôi có thể viết luôn cho anh một demo script 30 phút dạng từng câu phải nói, từng màn hình phải mở, và từng thao tác bấm theo đúng thứ tự để anh tập
  trước buổi bảo vệ.

Mục tiêu Demo

  Trong 30 phút, anh không demo “nhiều chức năng”, mà demo 1 câu chuyện hoàn chỉnh:

  Cấu hình thư viện -> Sinh viên đặt chỗ thông minh -> Check-in thực tế -> Thủ thư giám sát realtime -> AI hỗ trợ + escalation -> Reputation + analytics

  Đây là narrative mạnh nhất của SLIB.

  Bố trí Demo

  Chuẩn bị 3 màn hình là đẹp nhất:

  - Laptop 1: Admin hoặc Librarian web
  - Điện thoại: app sinh viên
  - Laptop 2 hoặc tab khác: màn hình Librarian chat/dashboard

  Chuẩn bị sẵn:

  - 1 tài khoản Admin
  - 1 tài khoản Librarian
  - 1 tài khoản Student
  - 1 booking có thể check-in ngay
  - 1 câu hỏi AI trả lời tốt
  - 1 câu hỏi để escalate sang thủ thư
  - 1 case vi phạm/no-show đã có dữ liệu
  - 1 video backup HCE/NFC dài 20-30 giây nếu live lỗi

  Script 30 Phút

  1. Phút 0-2: Mở bài
     Câu nói:
     “Bài toán của thư viện hiện tại là không kiểm soát tốt không gian sử dụng, khó xử lý tình trạng giữ chỗ ảo, khó theo dõi hành vi và thiếu dữ liệu vận hành. SLIB giải quyết bằng 4 trụ cột: đặt chỗ thông minh,
     check-in không chạm bằng HCE/QR, AI hỗ trợ, và reputation để quản trị hành vi.”
  2. Phút 2-6: Admin cấu hình hệ thống
     Mở web Admin và show:

  - bản đồ khu vực, zone, seat
  - rule đặt chỗ
  - rule reputation
    Câu nói:
    “Điểm quan trọng là hệ thống không hard-code. Thư viện có thể tự cấu hình khu vực, ghế, thời gian hoạt động, luật đặt chỗ và luật trừ điểm.”

  3. Phút 6-12: Sinh viên đặt chỗ trên mobile
     Mở app sinh viên và đi theo luồng:

  - đăng nhập
  - mở sơ đồ ghế realtime
  - filter khu vực yên tĩnh hoặc có ổ điện
  - hỏi AI gợi ý chỗ ngồi
  - đặt chỗ ngay
  - mở booking detail hoặc QR
    Câu nói:
    “Ở đây sinh viên không chỉ đặt chỗ thủ công, mà còn có AI hỗ trợ chọn chỗ phù hợp theo nhu cầu học tập.”

  4. Phút 12-17: Check-in/check-out và realtime
     Thực hiện:

  - check-in bằng HCE nếu ổn
  - nếu không ổn thì dùng QR ngay
  - đồng thời mở màn hình Librarian/Admin để thấy trạng thái ghế đổi realtime từ BOOKED sang OCCUPIED
    Câu nói:
    “Đây là điểm khác biệt lớn của SLIB. Hệ thống không dừng ở booking, mà gắn booking với hành vi sử dụng thực tế tại thư viện.”
    Nếu HCE lỗi:
    “Để tránh phụ thuộc thiết bị trong buổi demo, tôi chuyển sang luồng QR, còn đây là video ngắn thể hiện HCE hoạt động thực tế.”

  5. Phút 17-22: Librarian giám sát và xử lý
     Mở màn hình Librarian:

  - danh sách sinh viên đang sử dụng thư viện
  - trạng thái chỗ ngồi realtime
  - booking detail hoặc monitoring table
    Câu nói:
    “Thủ thư có thể nhìn thấy ai đang ở trong thư viện, đang ngồi khu vực nào, và trạng thái ghế được cập nhật theo thời gian thực.”

  6. Phút 22-26: AI chat và escalation
     Demo 2 tình huống:

  - câu 1: “Thư viện mở cửa mấy giờ?” hoặc “Cách check-in thế nào?” để AI trả lời tốt
  - câu 2: “Tôi muốn gặp thủ thư” hoặc một câu phức tạp để chuyển sang người thật
    Sau đó mở màn hình Librarian chat:
  - thấy conversation vào queue
  - mở hội thoại
  - dùng AI suggestion hoặc trả lời trực tiếp
    Câu nói:
    “AI trong SLIB không thay con người hoàn toàn. Khi đủ dữ liệu thì AI trả lời nhanh, khi không chắc chắn thì chuyển đúng cho thủ thư. Đây là cách làm thực tế hơn.”

  7. Phút 26-29: Reputation và analytics
     Show nhanh:

  - điểm reputation của sinh viên
  - lịch sử vi phạm hoặc khiếu nại
  - dashboard: mật độ sử dụng, giờ cao điểm, khu vực dùng nhiều nhất
    Câu nói:
    “SLIB không chỉ là app tiện ích cho sinh viên, mà còn là hệ thống quản trị giúp thư viện kiểm soát hành vi và ra quyết định dựa trên dữ liệu.”

  8. Phút 29-30: Chốt
     Câu nói:
     “Tóm lại, SLIB có 3 giá trị chính: số hóa trải nghiệm sinh viên, kiểm soát vận hành thư viện theo thời gian thực, và bổ sung AI cùng cơ chế reputation để thư viện vận hành công bằng và hiệu quả hơn.”

  3 Chức Năng Phải Làm Thật Mượt

  Nếu thời gian gấp, ưu tiên tuyệt đối:

  - Seat map realtime + booking
  - HCE/QR check-in đồng bộ realtime
  - AI chat + escalation sang thủ thư

  Những thứ chỉ nên lướt

  - quên mật khẩu
  - chỉnh profile
  - news
  - CRUD user cơ bản
  - import file

  Câu Hỏi Hội Đồng Dễ Hỏi

  - “Điểm khác biệt của em so với hệ thống khác là gì?”
    Trả lời: HCE/native check-in, governance bằng reputation, AI có escalation, realtime monitoring.
  - “Nếu AI trả lời sai thì sao?”
    Trả lời: AI chỉ hỗ trợ; khi không chắc chắn sẽ chuyển thủ thư.
  - “Nếu thiết bị NFC lỗi thì sao?”
    Trả lời: Có fallback bằng QR.
  - “Giá trị cho thư viện là gì?”
    Trả lời: giảm giữ chỗ ảo, tăng kiểm soát, có dữ liệu peak hour, tối ưu vận hành.

  Nếu anh muốn, tôi làm tiếp cho anh bản lời thoại từng phút, kiểu đúng format để anh chỉ cần đứng lên nói và bấm theo.