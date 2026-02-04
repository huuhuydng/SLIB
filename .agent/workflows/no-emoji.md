---
description: Quy tắc không sử dụng emoji/icon trong code và documentation
---

# No Emoji/Icon Rule

## Quy tắc

Khi viết code hoặc documentation cho dự án SLIB, **KHÔNG** sử dụng:

1. **Emoji** (ví dụ: 🚀, 📦, 🔧, 💬, ✅, etc.)
2. **Unicode icons** hoặc special characters tương tự

## Áp dụng cho

- README.md files
- Code comments
- Documentation
- Markdown files
- Log messages trong code
- UI text (ngoại trừ khi có yêu cầu design cụ thể)

## Thay thế

Thay vì sử dụng emoji, hãy:

- Sử dụng text mô tả rõ ràng
- Sử dụng dấu gạch đầu dòng (bullet points) `-`
- Sử dụng numbered lists `1. 2. 3.`
- Sử dụng **bold** hoặc *italic* để nhấn mạnh

## Ví dụ

### Sai (có emoji):
```markdown
## 🚀 Tổng quan
- 📦 **Package**: Cài đặt dependencies
- 🔧 **Config**: Cấu hình hệ thống
```

### Đúng (không emoji):
```markdown
## Tổng quan
- **Package**: Cài đặt dependencies
- **Config**: Cấu hình hệ thống
```

## Lưu ý

- Badge images từ shields.io với logo vẫn được phép vì đó là image, không phải emoji
- Icon fonts (như Lucide React) trong UI code vẫn được phép sử dụng cho giao diện người dùng
