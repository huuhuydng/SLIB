# README_ThongDV

Tài liệu này được viết lại theo hướng tra cứu dự án SLIB, ưu tiên thông tin có thể dùng khi onboarding, code review, debug và triển khai tính năng mới.

## 1. Tóm tắt nhanh
- SLIB là monorepo cho hệ thống thư viện thông minh với backend Spring Boot, frontend React, mobile Flutter và AI service FastAPI.
- API chính dùng tiền tố /slib/, realtime dùng STOMP WebSocket tại /ws, AI service đứng riêng và được gọi qua các endpoint /api/ai/*.
- PostgreSQL là nguồn dữ liệu nghiệp vụ chính; MongoDB phục vụ lịch sử chat; Qdrant dùng cho vector search; Redis hỗ trợ cache hoặc realtime support.
- Vai trò chính trong hệ thống là STUDENT, LIBRARIAN, ADMIN; ngoài ra có luồng KIOSK cho thiết bị kiosk.
- Domain production hiện tại là slibsystem.site cho frontend và api.slibsystem.site cho backend.
- Khi làm full-stack nên đi theo thứ tự backend trước, sau đó AI service nếu cần, cuối cùng mới nối frontend hoặc mobile.

## 2. Bản đồ repo ở mức cao
- Tệp: .env
- Tệp: .env.example
- Thư mục: .git
- Thư mục: .github
- Tệp: .gitignore
- Tệp: .metadata
- Tệp: AGENTS.md
- Thư mục: ai-service
- Thư mục: backend
- Thư mục: doc
- Tệp: docker-compose.yml
- Tệp: fpt-library-detail-23894.png
- Thư mục: frontend
- Thư mục: mobile
- Thư mục: nfc-bridge
- Thư mục: nfc-bridge-app
- Tệp: package-lock.json
- Tệp: README copy.md
- Tệp: README.md
- Tệp: README_HUNG.md
- Tệp: README_Huy.md
- Tệp: README_NEW.md
- Tệp: README_ThongDV.md
- Tệp: slib.code-workspace
- Thư mục: slib_iot
- Thư mục: tools

## 3. Kiến trúc và dòng chảy dữ liệu
- Frontend React phục vụ admin và thủ thư. Luồng đăng nhập dùng token lưu trong localStorage hoặc sessionStorage rồi điều hướng theo role.
- Mobile Flutter phục vụ sinh viên. App gọi backend cho dữ liệu nghiệp vụ, nhận tin tức, thông báo, lịch sử đặt chỗ, vi phạm, hỗ trợ và các luồng ghế ngồi.
- Backend Spring Boot là trung tâm nghiệp vụ. Nó kiểm soát auth, phân quyền, API CRUD, scheduler, gửi thông báo, tương tác thiết bị và phát sự kiện realtime.
- AI service FastAPI xử lý chat RAG, ingestion tài liệu, analytics AI và quyết định khi nào cần escalation sang thủ thư.
- Kiosk là luồng đặc biệt cho thiết bị tại chỗ. Nó dùng endpoint kích hoạt thiết bị, session token và role KIOSK để chạy booking hoặc xác nhận.
- NFC và HCE liên quan đến checkin, xác nhận ghế hoặc định danh sinh viên. Luồng này có cả controller backend lẫn bridge app hoặc service phụ.
- WebSocket tại /ws dùng STOMP với broker prefix /topic và app prefix /app; frontend web dùng sockjs-client và @stomp/stompjs để subscribe sự kiện.
- Khi một tính năng lỗi đồng thời trên web, mobile và AI thì cần xác minh lại base URL, token, CORS, scheduler và secret trước khi kết luận logic sai.

## 4. Quy ước quan trọng của dự án
- Tất cả text hiển thị cho người dùng cần giữ tiếng Việt có dấu.
- Không tự chuyển API base path sang /api/v1 cho backend chính; SLIB dùng /slib/ là tiền tố chuẩn.
- Với backend, đọc SecurityConfig.java, WebSocketConfig.java và application.properties trước khi mở rộng endpoint mới.
- Với frontend, đọc App.jsx, route hiện tại, service tương ứng và stylesheet chung của khu vực đang sửa.
- Với mobile, đọc screen mục tiêu kèm service và model liên quan để tránh vỡ state hoặc persistence hiện hữu.
- Với AI service, đọc app/main.py, chat_service.py, escalation_service.py, settings.py trước khi thay đổi hành vi chatbot.
- Nhánh làm việc nên tách rõ theo tính năng; không trộn refactor lớn với bugfix nhỏ nếu chưa có lý do rõ ràng.
- Khi thấy lỗi full-stack, luôn truy từ backend contract trước rồi mới kết luận UI hoặc AI là nơi gây lỗi.

## 5. Checklist làm việc nhanh theo module
- Backend: kiểm tra request mapping, role access, DTO, service, transaction, repository và migration.
- Frontend: kiểm tra route, service gọi API, xử lý token, loading state, error state, websocket subscription và text tiếng Việt.
- Mobile: kiểm tra constant API, parsing model, guard context khi show snackbar hoặc dialog, điều hướng và restore trạng thái.
- AI service: kiểm tra env, khả năng gọi backend Java, kết nối Qdrant, Ollama, Mongo và threshold escalation.
- Kiosk và NFC: kiểm tra whitelist IP, secret key, token kiosk, controller auth thiết bị, bridge app và luồng xác nhận bằng QR hoặc NFC.
- CI/CD: kiểm tra workflow branch, workflow deploy VM, biến môi trường và version dependencies giữa các module.

## 6. Danh mục backend controller
Danh mục này giúp xác định nơi bắt đầu khi truy luồng API hoặc tìm logic phân quyền, mapping request và gọi service.

### Mục 1
- Đường dẫn: backend/src/main/java/slib/com/example/controller/activity/ActivityController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: thành phần ActivityController trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần ActivityController trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần ActivityController trong SLIB.

### Mục 2
- Đường dẫn: backend/src/main/java/slib/com/example/controller/admin/ReputationRuleController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: thành phần ReputationRuleController trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần ReputationRuleController trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần ReputationRuleController trong SLIB.

### Mục 3
- Đường dẫn: backend/src/main/java/slib/com/example/controller/ai/AIAnalyticsProxyController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần AIAnalyticsProxyController trong SLIB.

### Mục 4
- Đường dẫn: backend/src/main/java/slib/com/example/controller/ai/AIConfigController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần AIConfigController trong SLIB.

### Mục 5
- Đường dẫn: backend/src/main/java/slib/com/example/controller/ai/ChatController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần ChatController trong SLIB.

### Mục 6
- Đường dẫn: backend/src/main/java/slib/com/example/controller/ai/KnowledgeBaseController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KnowledgeBaseController trong SLIB.

### Mục 7
- Đường dẫn: backend/src/main/java/slib/com/example/controller/ai/KnowledgeStoreController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KnowledgeStoreController trong SLIB.

### Mục 8
- Đường dẫn: backend/src/main/java/slib/com/example/controller/ai/MaterialController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: thành phần MaterialController trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần MaterialController trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần MaterialController trong SLIB.

### Mục 9
- Đường dẫn: backend/src/main/java/slib/com/example/controller/ai/PromptTemplateController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: thành phần PromptTemplateController trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần PromptTemplateController trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần PromptTemplateController trong SLIB.

### Mục 10
- Đường dẫn: backend/src/main/java/slib/com/example/controller/auth/AuthController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: xác thực, phiên đăng nhập, token hoặc quyền truy cập
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết xác thực, phiên đăng nhập, token hoặc quyền truy cập.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần AuthController trong SLIB.

### Mục 11
- Đường dẫn: backend/src/main/java/slib/com/example/controller/auth/PasswordResetController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: xác thực, phiên đăng nhập, token hoặc quyền truy cập
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết xác thực, phiên đăng nhập, token hoặc quyền truy cập.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần PasswordResetController trong SLIB.

### Mục 12
- Đường dẫn: backend/src/main/java/slib/com/example/controller/booking/BookingController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần BookingController trong SLIB.

### Mục 13
- Đường dẫn: backend/src/main/java/slib/com/example/controller/chat/InternalChatController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần InternalChatController trong SLIB.

### Mục 14
- Đường dẫn: backend/src/main/java/slib/com/example/controller/chat/UserChatController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần UserChatController trong SLIB.

### Mục 15
- Đường dẫn: backend/src/main/java/slib/com/example/controller/complaint/ComplaintController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần ComplaintController trong SLIB.

### Mục 16
- Đường dẫn: backend/src/main/java/slib/com/example/controller/dashboard/DashboardController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần DashboardController trong SLIB.

### Mục 17
- Đường dẫn: backend/src/main/java/slib/com/example/controller/dashboard/StatisticController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần StatisticController trong SLIB.

### Mục 18
- Đường dẫn: backend/src/main/java/slib/com/example/controller/feedback/FeedbackController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần FeedbackController trong SLIB.

### Mục 19
- Đường dẫn: backend/src/main/java/slib/com/example/controller/feedback/SeatStatusReportController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần SeatStatusReportController trong SLIB.

### Mục 20
- Đường dẫn: backend/src/main/java/slib/com/example/controller/feedback/SeatViolationReportController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần SeatViolationReportController trong SLIB.

### Mục 21
- Đường dẫn: backend/src/main/java/slib/com/example/controller/hce/HCEController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: NFC, HCE, thẻ sinh viên hoặc giao tiếp thiết bị
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết NFC, HCE, thẻ sinh viên hoặc giao tiếp thiết bị.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần HCEController trong SLIB.

### Mục 22
- Đường dẫn: backend/src/main/java/slib/com/example/controller/hce/HceStationController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: NFC, HCE, thẻ sinh viên hoặc giao tiếp thiết bị
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết NFC, HCE, thẻ sinh viên hoặc giao tiếp thiết bị.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần HceStationController trong SLIB.

### Mục 23
- Đường dẫn: backend/src/main/java/slib/com/example/controller/kiosk/KioskAdminController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskAdminController trong SLIB.

### Mục 24
- Đường dẫn: backend/src/main/java/slib/com/example/controller/kiosk/KioskAuthController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: xác thực, phiên đăng nhập, token hoặc quyền truy cập
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết xác thực, phiên đăng nhập, token hoặc quyền truy cập.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskAuthController trong SLIB.

### Mục 25
- Đường dẫn: backend/src/main/java/slib/com/example/controller/kiosk/KioskLibraryMapController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskLibraryMapController trong SLIB.

### Mục 26
- Đường dẫn: backend/src/main/java/slib/com/example/controller/kiosk/KioskMonitorController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskMonitorController trong SLIB.

### Mục 27
- Đường dẫn: backend/src/main/java/slib/com/example/controller/kiosk/KioskSlideshowController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskSlideshowController trong SLIB.

### Mục 28
- Đường dẫn: backend/src/main/java/slib/com/example/controller/news/CategoryController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: nội dung truyền thông như tin tức hoặc sách mới
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết nội dung truyền thông như tin tức hoặc sách mới.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần CategoryController trong SLIB.

### Mục 29
- Đường dẫn: backend/src/main/java/slib/com/example/controller/news/NewBookController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: nội dung truyền thông như tin tức hoặc sách mới
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết nội dung truyền thông như tin tức hoặc sách mới.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần NewBookController trong SLIB.

### Mục 30
- Đường dẫn: backend/src/main/java/slib/com/example/controller/news/NewsController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: nội dung truyền thông như tin tức hoặc sách mới
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết nội dung truyền thông như tin tức hoặc sách mới.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần NewsController trong SLIB.

### Mục 31
- Đường dẫn: backend/src/main/java/slib/com/example/controller/notification/LibrarianNotificationController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: thông báo, phân phối sự kiện và nhắc việc
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thông báo, phân phối sự kiện và nhắc việc.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần LibrarianNotificationController trong SLIB.

### Mục 32
- Đường dẫn: backend/src/main/java/slib/com/example/controller/notification/NotificationController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: thông báo, phân phối sự kiện và nhắc việc
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thông báo, phân phối sự kiện và nhắc việc.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần NotificationController trong SLIB.

### Mục 33
- Đường dẫn: backend/src/main/java/slib/com/example/controller/support/SupportRequestController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần SupportRequestController trong SLIB.

### Mục 34
- Đường dẫn: backend/src/main/java/slib/com/example/controller/system/BackupController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần BackupController trong SLIB.

### Mục 35
- Đường dẫn: backend/src/main/java/slib/com/example/controller/system/FileUploadController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần FileUploadController trong SLIB.

### Mục 36
- Đường dẫn: backend/src/main/java/slib/com/example/controller/system/LibrarySettingController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần LibrarySettingController trong SLIB.

### Mục 37
- Đường dẫn: backend/src/main/java/slib/com/example/controller/system/SeedDataController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần SeedDataController trong SLIB.

### Mục 38
- Đường dẫn: backend/src/main/java/slib/com/example/controller/system/SystemInfoController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần SystemInfoController trong SLIB.

### Mục 39
- Đường dẫn: backend/src/main/java/slib/com/example/controller/system/SystemLogController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần SystemLogController trong SLIB.

### Mục 40
- Đường dẫn: backend/src/main/java/slib/com/example/controller/users/StudentProfileController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: người dùng, hồ sơ, danh tính và phân quyền
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết người dùng, hồ sơ, danh tính và phân quyền.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần StudentProfileController trong SLIB.

### Mục 41
- Đường dẫn: backend/src/main/java/slib/com/example/controller/users/UserController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: người dùng, hồ sơ, danh tính và phân quyền
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết người dùng, hồ sơ, danh tính và phân quyền.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần UserController trong SLIB.

### Mục 42
- Đường dẫn: backend/src/main/java/slib/com/example/controller/users/UserSettingController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần UserSettingController trong SLIB.

### Mục 43
- Đường dẫn: backend/src/main/java/slib/com/example/controller/zone_config/AmenityController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần AmenityController trong SLIB.

### Mục 44
- Đường dẫn: backend/src/main/java/slib/com/example/controller/zone_config/AreaController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần AreaController trong SLIB.

### Mục 45
- Đường dẫn: backend/src/main/java/slib/com/example/controller/zone_config/AreaFactoryController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần AreaFactoryController trong SLIB.

### Mục 46
- Đường dẫn: backend/src/main/java/slib/com/example/controller/zone_config/LayoutAdminController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần LayoutAdminController trong SLIB.

### Mục 47
- Đường dẫn: backend/src/main/java/slib/com/example/controller/zone_config/SeatController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần SeatController trong SLIB.

### Mục 48
- Đường dẫn: backend/src/main/java/slib/com/example/controller/zone_config/SeatHoldController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần SeatHoldController trong SLIB.

### Mục 49
- Đường dẫn: backend/src/main/java/slib/com/example/controller/zone_config/ZoneController.java
- Khu vực: Backend Spring Boot
- Phạm vi: API backend
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: SecurityConfig.java, WebSocketConfig.java, application.properties, service tương ứng, repository tương ứng, migration liên quan.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần ZoneController trong SLIB.

## 7. Danh mục frontend page
Danh mục này giúp xác định màn hình nghiệp vụ cho admin, thủ thư, auth, kiosk và error handling trên web.

### Mục 1
- Đường dẫn: frontend/src/pages/admin/AIConfig/AIConfig.jsx
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần AIConfig trong SLIB.

### Mục 2
- Đường dẫn: frontend/src/pages/admin/AreaManagement/AreaManagement.jsx
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần AreaManagement trong SLIB.

### Mục 3
- Đường dẫn: frontend/src/pages/admin/AreaManagement/dashboard/Dashboard.jsx
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần Dashboard trong SLIB.

### Mục 4
- Đường dẫn: frontend/src/pages/admin/AreaManagement/dashboard/Header.jsx
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần Header trong SLIB.

### Mục 5
- Đường dẫn: frontend/src/pages/admin/AreaManagement/dashboard/StatCard.jsx
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần StatCard trong SLIB.

### Mục 6
- Đường dẫn: frontend/src/pages/admin/Dashboard/Dashboard.jsx
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần Dashboard trong SLIB.

### Mục 7
- Đường dẫn: frontend/src/pages/admin/Dashboard/StatCard.jsx
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần StatCard trong SLIB.

### Mục 8
- Đường dẫn: frontend/src/pages/admin/DeviceManagement/DeviceManagement.jsx
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: thành phần DeviceManagement trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần DeviceManagement trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần DeviceManagement trong SLIB.

### Mục 9
- Đường dẫn: frontend/src/pages/admin/KioskManagement/KioskManagement.css
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskManagement trong SLIB.

### Mục 10
- Đường dẫn: frontend/src/pages/admin/KioskManagement/KioskManagement.jsx
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskManagement trong SLIB.

### Mục 11
- Đường dẫn: frontend/src/pages/admin/NfcManagement/NfcManagement.css
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: NFC, HCE, thẻ sinh viên hoặc giao tiếp thiết bị
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết NFC, HCE, thẻ sinh viên hoặc giao tiếp thiết bị.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần NfcManagement trong SLIB.

### Mục 12
- Đường dẫn: frontend/src/pages/admin/NfcManagement/NfcManagement.jsx
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: NFC, HCE, thẻ sinh viên hoặc giao tiếp thiết bị
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết NFC, HCE, thẻ sinh viên hoặc giao tiếp thiết bị.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần NfcManagement trong SLIB.

### Mục 13
- Đường dẫn: frontend/src/pages/admin/SystemConfig/SystemConfig.jsx
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần SystemConfig trong SLIB.

### Mục 14
- Đường dẫn: frontend/src/pages/admin/SystemHealth/SystemHealth.css
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần SystemHealth trong SLIB.

### Mục 15
- Đường dẫn: frontend/src/pages/admin/SystemHealth/SystemHealth.jsx
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần SystemHealth trong SLIB.

### Mục 16
- Đường dẫn: frontend/src/pages/admin/UserManagement/UserManagement.css
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: người dùng, hồ sơ, danh tính và phân quyền
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết người dùng, hồ sơ, danh tính và phân quyền.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần UserManagement trong SLIB.

### Mục 17
- Đường dẫn: frontend/src/pages/admin/UserManagement/UserManagement.jsx
- Khu vực: Frontend React
- Phạm vi: Admin web
- Vai trò chính: người dùng, hồ sơ, danh tính và phân quyền
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết người dùng, hồ sơ, danh tính và phân quyền.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần UserManagement trong SLIB.

### Mục 18
- Đường dẫn: frontend/src/pages/auth/LoginPage.jsx
- Khu vực: Frontend React
- Phạm vi: Module chung
- Vai trò chính: xác thực, phiên đăng nhập, token hoặc quyền truy cập
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết xác thực, phiên đăng nhập, token hoặc quyền truy cập.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần LoginPage trong SLIB.

### Mục 19
- Đường dẫn: frontend/src/pages/errors/ErrorPages.jsx
- Khu vực: Frontend React
- Phạm vi: Module chung
- Vai trò chính: thành phần ErrorPages trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần ErrorPages trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần ErrorPages trong SLIB.

### Mục 20
- Đường dẫn: frontend/src/pages/kiosk/AttendanceWaitingScreen.jsx
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần AttendanceWaitingScreen trong SLIB.

### Mục 21
- Đường dẫn: frontend/src/pages/kiosk/KioskAccessDenied.jsx
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskAccessDenied trong SLIB.

### Mục 22
- Đường dẫn: frontend/src/pages/kiosk/KioskDashboard.css
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskDashboard trong SLIB.

### Mục 23
- Đường dẫn: frontend/src/pages/kiosk/KioskDashboard.jsx
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskDashboard trong SLIB.

### Mục 24
- Đường dẫn: frontend/src/pages/kiosk/KioskHelp.jsx
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskHelp trong SLIB.

### Mục 25
- Đường dẫn: frontend/src/pages/kiosk/KioskHome.jsx
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskHome trong SLIB.

### Mục 26
- Đường dẫn: frontend/src/pages/kiosk/KioskLockScreen.css
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskLockScreen trong SLIB.

### Mục 27
- Đường dẫn: frontend/src/pages/kiosk/KioskLockScreen.jsx
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskLockScreen trong SLIB.

### Mục 28
- Đường dẫn: frontend/src/pages/kiosk/KioskModeSelect.jsx
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskModeSelect trong SLIB.

### Mục 29
- Đường dẫn: frontend/src/pages/kiosk/KioskQrAuth.css
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: xác thực, phiên đăng nhập, token hoặc quyền truy cập
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết xác thực, phiên đăng nhập, token hoặc quyền truy cập.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskQrAuth trong SLIB.

### Mục 30
- Đường dẫn: frontend/src/pages/kiosk/KioskQrAuth.jsx
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: xác thực, phiên đăng nhập, token hoặc quyền truy cập
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết xác thực, phiên đăng nhập, token hoặc quyền truy cập.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskQrAuth trong SLIB.

### Mục 31
- Đường dẫn: frontend/src/pages/kiosk/KioskSeatBooking.css
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskSeatBooking trong SLIB.

### Mục 32
- Đường dẫn: frontend/src/pages/kiosk/KioskSeatBooking.jsx
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskSeatBooking trong SLIB.

### Mục 33
- Đường dẫn: frontend/src/pages/kiosk/KioskStudentLogin.jsx
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskStudentLogin trong SLIB.

### Mục 34
- Đường dẫn: frontend/src/pages/kiosk/KioskStudentMode.css
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskStudentMode trong SLIB.

### Mục 35
- Đường dẫn: frontend/src/pages/kiosk/KioskStudentMode.jsx
- Khu vực: Frontend React
- Phạm vi: Kiosk web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskStudentMode trong SLIB.

### Mục 36
- Đường dẫn: frontend/src/pages/librarian/BookingManage/BookingManage.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần BookingManage trong SLIB.

### Mục 37
- Đường dẫn: frontend/src/pages/librarian/ChatManage/ChatManage.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần ChatManage trong SLIB.

### Mục 38
- Đường dẫn: frontend/src/pages/librarian/CheckInOut/CheckInOut.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: thành phần CheckInOut trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần CheckInOut trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần CheckInOut trong SLIB.

### Mục 39
- Đường dẫn: frontend/src/pages/librarian/ComplaintManage/ComplaintManage.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần ComplaintManage trong SLIB.

### Mục 40
- Đường dẫn: frontend/src/pages/librarian/Dashboard/Dashboard.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần Dashboard trong SLIB.

### Mục 41
- Đường dẫn: frontend/src/pages/librarian/Dashboard/StatCard.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần StatCard trong SLIB.

### Mục 42
- Đường dẫn: frontend/src/pages/librarian/FeedbackManage/FeedbackManage.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần FeedbackManage trong SLIB.

### Mục 43
- Đường dẫn: frontend/src/pages/librarian/HeatMap/HeatMap.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: thành phần HeatMap trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần HeatMap trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần HeatMap trong SLIB.

### Mục 44
- Đường dẫn: frontend/src/pages/librarian/KioskSeatManage/KioskSeatManage.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết luồng kiosk, kích hoạt thiết bị và trải nghiệm sử dụng tại chỗ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần KioskSeatManage trong SLIB.

### Mục 45
- Đường dẫn: frontend/src/pages/librarian/LibrarianAreas/LibrarianAreas.css
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần LibrarianAreas trong SLIB.

### Mục 46
- Đường dẫn: frontend/src/pages/librarian/LibrarianAreas/LibrarianAreas.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần LibrarianAreas trong SLIB.

### Mục 47
- Đường dẫn: frontend/src/pages/librarian/NewBooks/NewBookCreate.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: nội dung truyền thông như tin tức hoặc sách mới
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết nội dung truyền thông như tin tức hoặc sách mới.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần NewBookCreate trong SLIB.

### Mục 48
- Đường dẫn: frontend/src/pages/librarian/NewBooks/NewBookManage.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: nội dung truyền thông như tin tức hoặc sách mới
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết nội dung truyền thông như tin tức hoặc sách mới.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần NewBookManage trong SLIB.

### Mục 49
- Đường dẫn: frontend/src/pages/librarian/NewsManage/NewCreate.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: nội dung truyền thông như tin tức hoặc sách mới
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết nội dung truyền thông như tin tức hoặc sách mới.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần NewCreate trong SLIB.

### Mục 50
- Đường dẫn: frontend/src/pages/librarian/NewsManage/NewsDetailView.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: nội dung truyền thông như tin tức hoặc sách mới
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết nội dung truyền thông như tin tức hoặc sách mới.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần NewsDetailView trong SLIB.

### Mục 51
- Đường dẫn: frontend/src/pages/librarian/NewsManage/NotificationManage.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: thông báo, phân phối sự kiện và nhắc việc
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thông báo, phân phối sự kiện và nhắc việc.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần NotificationManage trong SLIB.

### Mục 52
- Đường dẫn: frontend/src/pages/librarian/NotificationManage/NewCreate.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: thông báo, phân phối sự kiện và nhắc việc
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thông báo, phân phối sự kiện và nhắc việc.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần NewCreate trong SLIB.

### Mục 53
- Đường dẫn: frontend/src/pages/librarian/SeatPlan/SeatPlan.backup.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần SeatPlan.backup trong SLIB.

### Mục 54
- Đường dẫn: frontend/src/pages/librarian/SeatPlan/SeatPlan.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần SeatPlan trong SLIB.

### Mục 55
- Đường dẫn: frontend/src/pages/librarian/SeatStatusReportManage/SeatStatusReportManage.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần SeatStatusReportManage trong SLIB.

### Mục 56
- Đường dẫn: frontend/src/pages/librarian/Statistic/AIAnalyticsPanel.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần AIAnalyticsPanel trong SLIB.

### Mục 57
- Đường dẫn: frontend/src/pages/librarian/Statistic/Statistic.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần Statistic trong SLIB.

### Mục 58
- Đường dẫn: frontend/src/pages/librarian/StudentsManage/StudentsManage.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: người dùng, hồ sơ, danh tính và phân quyền
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết người dùng, hồ sơ, danh tính và phân quyền.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần StudentsManage trong SLIB.

### Mục 59
- Đường dẫn: frontend/src/pages/librarian/SupportRequest/SupportRequestManage.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần SupportRequestManage trong SLIB.

### Mục 60
- Đường dẫn: frontend/src/pages/librarian/ViolationManage/ViolationManage.jsx
- Khu vực: Frontend React
- Phạm vi: Librarian web
- Vai trò chính: phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: App.jsx, route tương ứng, service liên quan, stylesheet cùng khu vực, context nếu có.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần ViolationManage trong SLIB.

## 8. Danh mục mobile view
Danh mục này giúp truy các screen dành cho sinh viên, đặc biệt là booking, profile, vi phạm, hỗ trợ và thông báo.

### Mục 1
- Đường dẫn: mobile/lib/views/authentication/change_password_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: xác thực, phiên đăng nhập, token hoặc quyền truy cập
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết xác thực, phiên đăng nhập, token hoặc quyền truy cập.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần change_password_screen trong SLIB.

### Mục 2
- Đường dẫn: mobile/lib/views/authentication/forgot_password_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: xác thực, phiên đăng nhập, token hoặc quyền truy cập
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết xác thực, phiên đăng nhập, token hoặc quyền truy cập.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần forgot_password_screen trong SLIB.

### Mục 3
- Đường dẫn: mobile/lib/views/authentication/login_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: xác thực, phiên đăng nhập, token hoặc quyền truy cập
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết xác thực, phiên đăng nhập, token hoặc quyền truy cập.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần login_screen trong SLIB.

### Mục 4
- Đường dẫn: mobile/lib/views/authentication/on_boarding_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: xác thực, phiên đăng nhập, token hoặc quyền truy cập
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết xác thực, phiên đăng nhập, token hoặc quyền truy cập.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần on_boarding_screen trong SLIB.

### Mục 5
- Đường dẫn: mobile/lib/views/booking/floor_plan_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần floor_plan_screen trong SLIB.

### Mục 6
- Đường dẫn: mobile/lib/views/booking/README.md
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần README trong SLIB.

### Mục 7
- Đường dẫn: mobile/lib/views/card/student_card_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: NFC, HCE, thẻ sinh viên hoặc giao tiếp thiết bị
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết NFC, HCE, thẻ sinh viên hoặc giao tiếp thiết bị.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần student_card_screen trong SLIB.

### Mục 8
- Đường dẫn: mobile/lib/views/chat/chat_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần chat_screen trong SLIB.

### Mục 9
- Đường dẫn: mobile/lib/views/chat/README.md
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần README trong SLIB.

### Mục 10
- Đường dẫn: mobile/lib/views/checkin/qr_scan_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần qr_scan_screen trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần qr_scan_screen trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần qr_scan_screen trong SLIB.

### Mục 11
- Đường dẫn: mobile/lib/views/checkin/README.md
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần README trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần README trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần README trong SLIB.

### Mục 12
- Đường dẫn: mobile/lib/views/history/README.md
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần README trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần README trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần README trong SLIB.

### Mục 13
- Đường dẫn: mobile/lib/views/home/home_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần home_screen trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần home_screen trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần home_screen trong SLIB.

### Mục 14
- Đường dẫn: mobile/lib/views/home/README.md
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần README trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần README trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần README trong SLIB.

### Mục 15
- Đường dẫn: mobile/lib/views/home/widgets/ai_suggestion_card.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: NFC, HCE, thẻ sinh viên hoặc giao tiếp thiết bị
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết NFC, HCE, thẻ sinh viên hoặc giao tiếp thiết bị.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần ai_suggestion_card trong SLIB.

### Mục 16
- Đường dẫn: mobile/lib/views/home/widgets/booking_action_dialog.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần booking_action_dialog trong SLIB.

### Mục 17
- Đường dẫn: mobile/lib/views/home/widgets/booking_confirm_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần booking_confirm_screen trong SLIB.

### Mục 18
- Đường dẫn: mobile/lib/views/home/widgets/compact_header.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần compact_header trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần compact_header trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần compact_header trong SLIB.

### Mục 19
- Đường dẫn: mobile/lib/views/home/widgets/home_appbar.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần home_appbar trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần home_appbar trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần home_appbar trong SLIB.

### Mục 20
- Đường dẫn: mobile/lib/views/home/widgets/live_status_dashboard.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần live_status_dashboard trong SLIB.

### Mục 21
- Đường dẫn: mobile/lib/views/home/widgets/new_books_slider.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần new_books_slider trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần new_books_slider trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần new_books_slider trong SLIB.

### Mục 22
- Đường dẫn: mobile/lib/views/home/widgets/news_slider.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: nội dung truyền thông như tin tức hoặc sách mới
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết nội dung truyền thông như tin tức hoặc sách mới.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần news_slider trong SLIB.

### Mục 23
- Đường dẫn: mobile/lib/views/home/widgets/nfc_seat_verification_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần nfc_seat_verification_screen trong SLIB.

### Mục 24
- Đường dẫn: mobile/lib/views/home/widgets/notification_bell_button.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thông báo, phân phối sự kiện và nhắc việc
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thông báo, phân phối sự kiện và nhắc việc.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần notification_bell_button trong SLIB.

### Mục 25
- Đường dẫn: mobile/lib/views/home/widgets/quick_action_grid.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần quick_action_grid trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần quick_action_grid trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần quick_action_grid trong SLIB.

### Mục 26
- Đường dẫn: mobile/lib/views/home/widgets/reputation_card.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: NFC, HCE, thẻ sinh viên hoặc giao tiếp thiết bị
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết NFC, HCE, thẻ sinh viên hoặc giao tiếp thiết bị.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần reputation_card trong SLIB.

### Mục 27
- Đường dẫn: mobile/lib/views/home/widgets/section_title.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần section_title trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần section_title trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần section_title trong SLIB.

### Mục 28
- Đường dẫn: mobile/lib/views/home/widgets/upcoming_booking_card.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần upcoming_booking_card trong SLIB.

### Mục 29
- Đường dẫn: mobile/lib/views/map/map_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần map_screen trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần map_screen trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần map_screen trong SLIB.

### Mục 30
- Đường dẫn: mobile/lib/views/map/README.md
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần README trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần README trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần README trong SLIB.

### Mục 31
- Đường dẫn: mobile/lib/views/menu/README.md
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần README trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần README trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần README trong SLIB.

### Mục 32
- Đường dẫn: mobile/lib/views/menu/setting_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần setting_screen trong SLIB.

### Mục 33
- Đường dẫn: mobile/lib/views/new_books/new_book_detail_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần new_book_detail_screen trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần new_book_detail_screen trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần new_book_detail_screen trong SLIB.

### Mục 34
- Đường dẫn: mobile/lib/views/new_books/new_books_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần new_books_screen trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần new_books_screen trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần new_books_screen trong SLIB.

### Mục 35
- Đường dẫn: mobile/lib/views/news/news_detail_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: nội dung truyền thông như tin tức hoặc sách mới
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết nội dung truyền thông như tin tức hoặc sách mới.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần news_detail_screen trong SLIB.

### Mục 36
- Đường dẫn: mobile/lib/views/news/news_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: nội dung truyền thông như tin tức hoặc sách mới
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết nội dung truyền thông như tin tức hoặc sách mới.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần news_screen trong SLIB.

### Mục 37
- Đường dẫn: mobile/lib/views/news/README.md
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: nội dung truyền thông như tin tức hoặc sách mới
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết nội dung truyền thông như tin tức hoặc sách mới.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần README trong SLIB.

### Mục 38
- Đường dẫn: mobile/lib/views/news/widgets/news_item.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: nội dung truyền thông như tin tức hoặc sách mới
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết nội dung truyền thông như tin tức hoặc sách mới.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần news_item trong SLIB.

### Mục 39
- Đường dẫn: mobile/lib/views/notification/notification_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thông báo, phân phối sự kiện và nhắc việc
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thông báo, phân phối sự kiện và nhắc việc.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần notification_screen trong SLIB.

### Mục 40
- Đường dẫn: mobile/lib/views/profile/activity_history_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: người dùng, hồ sơ, danh tính và phân quyền
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết người dùng, hồ sơ, danh tính và phân quyền.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần activity_history_screen trong SLIB.

### Mục 41
- Đường dẫn: mobile/lib/views/profile/booking_history_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần booking_history_screen trong SLIB.

### Mục 42
- Đường dẫn: mobile/lib/views/profile/complaint_history_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần complaint_history_screen trong SLIB.

### Mục 43
- Đường dẫn: mobile/lib/views/profile/profile_info_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: người dùng, hồ sơ, danh tính và phân quyền
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết người dùng, hồ sơ, danh tính và phân quyền.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần profile_info_screen trong SLIB.

### Mục 44
- Đường dẫn: mobile/lib/views/profile/report_history_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: người dùng, hồ sơ, danh tính và phân quyền
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết người dùng, hồ sơ, danh tính và phân quyền.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần report_history_screen trong SLIB.

### Mục 45
- Đường dẫn: mobile/lib/views/profile/violation_history_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần violation_history_screen trong SLIB.

### Mục 46
- Đường dẫn: mobile/lib/views/README.md
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần README trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần README trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần README trong SLIB.

### Mục 47
- Đường dẫn: mobile/lib/views/seat_status_report/seat_status_report_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết đặt chỗ, sơ đồ thư viện, ghế, khu vực và trạng thái chỗ ngồi.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần seat_status_report_screen trong SLIB.

### Mục 48
- Đường dẫn: mobile/lib/views/support/support_request_history_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần support_request_history_screen trong SLIB.

### Mục 49
- Đường dẫn: mobile/lib/views/support/support_request_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần support_request_screen trong SLIB.

### Mục 50
- Đường dẫn: mobile/lib/views/violation_report/violation_report_screen.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần violation_report_screen trong SLIB.

### Mục 51
- Đường dẫn: mobile/lib/views/widgets/bottom_nav_widget.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần bottom_nav_widget trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần bottom_nav_widget trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần bottom_nav_widget trong SLIB.

### Mục 52
- Đường dẫn: mobile/lib/views/widgets/error_display_widget.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: thành phần error_display_widget trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần error_display_widget trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần error_display_widget trong SLIB.

### Mục 53
- Đường dẫn: mobile/lib/views/widgets/feedback_dialog.dart
- Khu vực: Mobile Flutter
- Phạm vi: Mobile student
- Vai trò chính: phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết phản hồi, khiếu nại, hỗ trợ và xử lý vi phạm.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: service liên quan, model liên quan, constants API, widget hoặc screen cùng luồng.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần feedback_dialog trong SLIB.

## 9. Danh mục AI service
Danh mục này tập trung vào routers, services và cấu hình của microservice AI.

### Mục 1
- Đường dẫn: ai-service/app/__init__.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần __init__ trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần __init__ trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần __init__ trong SLIB.

### Mục 2
- Đường dẫn: ai-service/app/__pycache__/main.cpython-314.pyc
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần main.cpython-314 trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần main.cpython-314 trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần main.cpython-314 trong SLIB.

### Mục 3
- Đường dẫn: ai-service/app/config/__init__.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần __init__ trong SLIB.

### Mục 4
- Đường dẫn: ai-service/app/config/__pycache__/settings.cpython-314.pyc
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần settings.cpython-314 trong SLIB.

### Mục 5
- Đường dẫn: ai-service/app/config/settings.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: cấu hình hệ thống, thông tin hệ thống hoặc vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết cấu hình hệ thống, thông tin hệ thống hoặc vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần settings trong SLIB.

### Mục 6
- Đường dẫn: ai-service/app/core/__init__.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần __init__ trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần __init__ trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần __init__ trong SLIB.

### Mục 7
- Đường dẫn: ai-service/app/core/admin_auth.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: xác thực, phiên đăng nhập, token hoặc quyền truy cập
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết xác thực, phiên đăng nhập, token hoặc quyền truy cập.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần admin_auth trong SLIB.

### Mục 8
- Đường dẫn: ai-service/app/core/database.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần database trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần database trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần database trong SLIB.

### Mục 9
- Đường dẫn: ai-service/app/core/env_loader.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần env_loader trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần env_loader trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần env_loader trong SLIB.

### Mục 10
- Đường dẫn: ai-service/app/main.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần main trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần main trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần main trong SLIB.

### Mục 11
- Đường dẫn: ai-service/app/models/__init__.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần __init__ trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần __init__ trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần __init__ trong SLIB.

### Mục 12
- Đường dẫn: ai-service/app/models/__pycache__/schemas.cpython-314.pyc
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần schemas.cpython-314 trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần schemas.cpython-314 trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần schemas.cpython-314 trong SLIB.

### Mục 13
- Đường dẫn: ai-service/app/models/schemas.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần schemas trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần schemas trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần schemas trong SLIB.

### Mục 14
- Đường dẫn: ai-service/app/routers/__init__.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần __init__ trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần __init__ trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần __init__ trong SLIB.

### Mục 15
- Đường dẫn: ai-service/app/routers/analytics.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần analytics trong SLIB.

### Mục 16
- Đường dẫn: ai-service/app/routers/chat.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần chat trong SLIB.

### Mục 17
- Đường dẫn: ai-service/app/routers/ingestion.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần ingestion trong SLIB.

### Mục 18
- Đường dẫn: ai-service/app/services/__init__.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần __init__ trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần __init__ trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần __init__ trong SLIB.

### Mục 19
- Đường dẫn: ai-service/app/services/__pycache__/__init__.cpython-314.pyc
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần __init__.cpython-314 trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần __init__.cpython-314 trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần __init__.cpython-314 trong SLIB.

### Mục 20
- Đường dẫn: ai-service/app/services/__pycache__/analytics_service.cpython-314.pyc
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần analytics_service.cpython-314 trong SLIB.

### Mục 21
- Đường dẫn: ai-service/app/services/__pycache__/escalation_service.cpython-314.pyc
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần escalation_service.cpython-314 trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần escalation_service.cpython-314 trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần escalation_service.cpython-314 trong SLIB.

### Mục 22
- Đường dẫn: ai-service/app/services/__pycache__/gemini_service.cpython-314.pyc
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần gemini_service.cpython-314 trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần gemini_service.cpython-314 trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần gemini_service.cpython-314 trong SLIB.

### Mục 23
- Đường dẫn: ai-service/app/services/__pycache__/knowledge_base.cpython-314.pyc
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần knowledge_base.cpython-314 trong SLIB.

### Mục 24
- Đường dẫn: ai-service/app/services/__pycache__/ollama_service.cpython-314.pyc
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần ollama_service.cpython-314 trong SLIB.

### Mục 25
- Đường dẫn: ai-service/app/services/analytics_service.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: dashboard, thống kê và chỉ số vận hành
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết dashboard, thống kê và chỉ số vận hành.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần analytics_service trong SLIB.

### Mục 26
- Đường dẫn: ai-service/app/services/chat_service.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết chat AI, hội thoại người dùng hoặc chuyển tiếp cho thủ thư.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần chat_service trong SLIB.

### Mục 27
- Đường dẫn: ai-service/app/services/embedding_service.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần embedding_service trong SLIB.

### Mục 28
- Đường dẫn: ai-service/app/services/escalation_service.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần escalation_service trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần escalation_service trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần escalation_service trong SLIB.

### Mục 29
- Đường dẫn: ai-service/app/services/gemini_service.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần gemini_service trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần gemini_service trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần gemini_service trong SLIB.

### Mục 30
- Đường dẫn: ai-service/app/services/ingestion_service.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần ingestion_service trong SLIB.

### Mục 31
- Đường dẫn: ai-service/app/services/java_backend_client.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần java_backend_client trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần java_backend_client trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần java_backend_client trong SLIB.

### Mục 32
- Đường dẫn: ai-service/app/services/knowledge_base.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần knowledge_base trong SLIB.

### Mục 33
- Đường dẫn: ai-service/app/services/mongo_service.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: thành phần mongo_service trong kiến trúc SLIB
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết thành phần mongo_service trong kiến trúc SLIB.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần mongo_service trong SLIB.

### Mục 34
- Đường dẫn: ai-service/app/services/ollama_service.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần ollama_service trong SLIB.

### Mục 35
- Đường dẫn: ai-service/app/services/qdrant_service.py
- Khu vực: AI Service FastAPI
- Phạm vi: AI microservice
- Vai trò chính: RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ
- Dấu hiệu nên mở file này: khi cần sửa hoặc truy vết RAG, tri thức, embeddings, vector search hoặc mô hình ngôn ngữ.
- Điều nên xác nhận trước khi sửa: contract vào ra, role nào được phép chạm tới luồng này, và dữ liệu nào là nguồn sự thật cuối cùng.
- File nên đọc cùng: main.py, settings.py, router tương ứng, service nền tảng và tích hợp backend Java.
- Rủi ro thường gặp: sửa màn hình hoặc controller nhưng quên service, DTO, validation, websocket event hoặc migration liên quan.
- Góc nhìn review: tên biến có phản ánh nghiệp vụ thật không, trạng thái lỗi có hiển thị đúng tiếng Việt không, và luồng fallback có còn hợp lệ không.
- Câu hỏi khi debug: lỗi xảy ra từ dữ liệu đầu vào, phân quyền, side effect scheduler hoặc realtime, hay do module khác thay đổi contract?
- Giá trị tra cứu: file này là một điểm neo tốt để dựng sơ đồ luồng của phần qdrant_service trong SLIB.

## 10. Luồng nghiệp vụ quan trọng cần nhớ
- Đăng nhập web: người dùng vào trang login chung, backend xác thực, frontend lưu librarian_token và librarian_user, sau đó điều hướng theo role ADMIN hoặc LIBRARIAN.
- Đăng nhập Google: domain phải khớp @fpt.edu.vn; nếu login thành công nhưng web không vào được màn hình chính thì cần kiểm tra parse user, role và hết hạn token.
- Đặt ghế cho sinh viên: mobile hoặc kiosk lấy danh sách khu vực, zone và seat; người dùng chọn chỗ; backend giữ hoặc tạo booking; xác nhận có thể đi qua NFC hoặc kiosk flow.
- Realtime trạng thái ghế: backend publish qua STOMP topic; web hoặc mobile subscribe để đổi màu hoặc trạng thái chỗ ngồi gần thời gian thực.
- Chat AI: frontend hoặc mobile gửi message đến backend hoặc AI service tùy contract; AI service truy tri thức ở Qdrant; nếu confidence thấp hoặc user yêu cầu thì hệ thống escalate sang thủ thư.
- Khiếu nại và phản hồi: sinh viên gửi complaint hoặc feedback; backend lưu nghiệp vụ; thủ thư xem danh sách, phản hồi hoặc cập nhật trạng thái trên web quản trị.
- Thông báo: backend tổng hợp và gửi notification; mobile hoặc web lấy danh sách, filter hoặc đọc chi tiết; migration mới gần đây mở rộng loại notification nên cần để ý enum và constraint DB.
- Kiosk management: admin hoặc thủ thư cấu hình thiết bị kiosk, kích hoạt phiên, kiểm soát session token và giám sát tình trạng thiết bị từ dashboard hoặc màn hình quản trị.

## 11. Các lệnh hay dùng
- cd backend && ./mvnw spring-boot:run
- cd backend && ./mvnw clean package -DskipTests
- cd backend && ./mvnw test
- cd frontend && npm install
- cd frontend && npm run dev
- cd frontend && npm run build
- cd frontend && npm run lint
- cd mobile && flutter pub get
- cd mobile && flutter run
- cd ai-service && uvicorn app.main:app --reload --port 8001
- docker-compose up -d
- docker-compose logs -f

## 12. Checklist review trước khi merge
- Endpoint mới có nằm đúng dưới /slib/ hoặc /api/ai/ theo module không?
- CORS, SecurityConfig và role access đã tương thích với luồng mới chưa?
- Frontend hoặc mobile có hiển thị đúng loading, empty, error và text tiếng Việt không?
- Nếu có realtime thì đã xác nhận topic, subscription và cleanup chưa?
- Nếu có dữ liệu mới thì migration, DTO, response model và parsing UI hoặc mobile đã cập nhật cùng nhau chưa?
- Nếu liên quan AI thì threshold, escalation, persistence Mongo và nguồn tri thức đã được kiểm chứng chưa?
- Nếu liên quan kiosk hoặc NFC thì IP whitelist, secret, token và bridge app có bị ảnh hưởng không?
- README hoặc tài liệu nội bộ có cần cập nhật để người sau đọc không?

## 13. Nhật ký đọc hiểu dự án
### Ghi chú đọc hiểu 001
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 002
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 003
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 004
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 005
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 006
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 007
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 008
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 009
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 010
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 011
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 012
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 013
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 014
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 015
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 016
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 017
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 018
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 019
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 020
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 021
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 022
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 023
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 024
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 025
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 026
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 027
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 028
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 029
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 030
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 031
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 032
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 033
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 034
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 035
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 036
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 037
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 038
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 039
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 040
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 041
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 042
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 043
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 044
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 045
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 046
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 047
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 048
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 049
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 050
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 051
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 052
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 053
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 054
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 055
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 056
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 057
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 058
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 059
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 060
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 061
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 062
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 063
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 064
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 065
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 066
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 067
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 068
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 069
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 070
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 071
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 072
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 073
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 074
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 075
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 076
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 077
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 078
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 079
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 080
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 081
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 082
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 083
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 084
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 085
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 086
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 087
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 088
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 089
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 090
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 091
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 092
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 093
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 094
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 095
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 096
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 097
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 098
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 099
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 100
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 101
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 102
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 103
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 104
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 105
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 106
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 107
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 108
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 109
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 110
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 111
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 112
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 113
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 114
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 115
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 116
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 117
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 118
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 119
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 120
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 121
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 122
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 123
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 124
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 125
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 126
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 127
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 128
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 129
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 130
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 131
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 132
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 133
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 134
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 135
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 136
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 137
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 138
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 139
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 140
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 141
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 142
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 143
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 144
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 145
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 146
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 147
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 148
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 149
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 150
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 151
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 152
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 153
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 154
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 155
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 156
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 157
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 158
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 159
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 160
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 161
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 162
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 163
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 164
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 165
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 166
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 167
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 168
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 169
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 170
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 171
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 172
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 173
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 174
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 175
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 176
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 177
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 178
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 179
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 180
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 181
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 182
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 183
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 184
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 185
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 186
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 187
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 188
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 189
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 190
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 191
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 192
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 193
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 194
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 195
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 196
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 197
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 198
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 199
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 200
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 201
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 202
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 203
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 204
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 205
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 206
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 207
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 208
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 209
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 210
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 211
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 212
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 213
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 214
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 215
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 216
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 217
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 218
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 219
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

### Ghi chú đọc hiểu 220
- Mục tiêu phiên đọc: rà soát một phần của SLIB để hiểu rõ hơn ranh giới giữa backend, frontend, mobile và AI service.
- Kết luận ngắn: phần thay đổi nên bắt đầu từ contract API và role access trước khi chạm tới giao diện hoặc automation bên dưới.
- Điểm cần soi kỹ: token, response shape, xử lý trạng thái lỗi, dữ liệu realtime và các side effect từ scheduler hoặc notification.
- Dấu hiệu có khả năng phát sinh bug: một module đổi enum hoặc trạng thái nhưng module còn lại vẫn giữ mapping cũ.
- Hướng tiếp cận an toàn: đọc file route hoặc controller, theo xuống service, rồi mới nhìn model, repository hoặc component gọi API.
- Hướng tiếp cận khi debug UI: xác minh API trả gì, role nào được vào, component có chặn render không, và CSS có che mất trạng thái lỗi không.
- Hướng tiếp cận khi debug mobile: xác minh constants API, parse model, mounted hoặc context safety và luồng điều hướng sau khi gọi API.
- Hướng tiếp cận khi debug AI: xác minh ingestion, Qdrant, Mongo, backend Java proxy và ngưỡng escalation có đang tạo kết quả khó đoán không.
- Hướng tiếp cận khi debug kiosk: xác minh token kiosk, activation flow, endpoint public hoặc role KIOSK, và trạng thái thiết bị thực tế.
- Giá trị của ghi chú này: dùng như checklist suy nghĩ nhanh trước khi sửa một tính năng trong SLIB.

## 14. Từ điển thuật ngữ ngắn
- Booking: bản ghi đặt chỗ hoặc giữ ghế theo thời gian.
- Seat hold: trạng thái giữ ghế tạm thời trước khi xác nhận.
- Kiosk: thiết bị hoặc chế độ giao diện phục vụ thao tác tại chỗ trong thư viện.
- Escalation: chuyển cuộc hội thoại hoặc yêu cầu sang thủ thư để xử lý thủ công.
- Knowledge base: tập tài liệu hoặc nội dung tri thức mà AI service ingest để trả lời.
- Qdrant: vector database cho truy vấn ngữ nghĩa.
- HCE hoặc NFC: cơ chế xác thực hoặc tương tác thẻ và thiết bị gần trường.
- Topic STOMP: kênh server phát sự kiện realtime tới client.
- Migration: script cập nhật schema hoặc dữ liệu nền trong PostgreSQL.
- Staff auth: nhóm logic xác thực cho admin hoặc thủ thư trên web.

