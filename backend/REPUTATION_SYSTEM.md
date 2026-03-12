# Hệ Thống Reputation (Điểm Uy Tín)

## Tổng Quan

Hệ thống reputation quản lý điểm uy tín của người dùng dựa trên các quy tắc (rules) được định nghĩa trong database. Admin có thể tạo, sửa, xóa các quy tắc mà không cần thay đổi code.

## Kiến Trúc

### 1. Database Schema

#### Table: `reputation_rules`
```sql
- id: SERIAL PRIMARY KEY
- rule_code: VARCHAR(50) UNIQUE (e.g., 'NO_SHOW', 'LATE_CHECKOUT')
- rule_name: VARCHAR(200) (Tên hiển thị)
- description: TEXT (Mô tả chi tiết)
- points: INTEGER (Số điểm: âm = phạt, dương = thưởng)
- rule_type: ENUM('PENALTY', 'REWARD')
- is_active: BOOLEAN (Bật/tắt rule)
- created_at, updated_at: TIMESTAMP
```

#### Table: `users`
```sql
- id: UUID PRIMARY KEY
- reputation_score: INTEGER DEFAULT 100 (Điểm uy tín của user)
- ... (các field khác)
```

#### Table: `point_transactions`
```sql
- id: UUID PRIMARY KEY
- user_id: UUID (FK -> users)
- points: INTEGER (Số điểm thay đổi)
- transaction_type: VARCHAR(50)
- title, description: VARCHAR/TEXT
- balance_after: INTEGER (Số điểm còn lại sau transaction)
- rule_id: INTEGER (FK -> reputation_rules)
- activity_log_id: UUID
- created_at: TIMESTAMP
```

### 2. Services

#### `ReputationService`
**Chức năng chính:** Quản lý toàn bộ logic áp dụng penalty/reward

**Methods:**
- `applyReputationRule()` - Method tổng quát để áp dụng bất kỳ rule nào
- `applyNoShowPenalty()` - Áp dụng phạt không check-in
- `applyLateCheckoutPenalty()` - Áp dụng phạt trả chỗ muộn
- `applyCheckInBonus()` - Áp dụng thưởng check-in đúng giờ
- `getUserReputationScore()` - Lấy điểm reputation hiện tại của user

**Cách hoạt động:**
1. Nhận vào: `userId`, `ruleCode`, thông tin context (seat, zone, reservation)
2. Query rule từ database qua `ruleCode`
3. Kiểm tra rule có active không
4. Cập nhật `users.reputation_score`
5. Tạo `ActivityLog` và `PointTransaction`
6. Return true/false

**Ưu điểm:**
- ✅ **Dynamic**: Admin thêm rule mới → Service tự động áp dụng
- ✅ **DRY**: Tái sử dụng code, không duplicate logic
- ✅ **Maintainable**: Dễ bảo trì, mở rộng
- ✅ **Traceable**: Đầy đủ logs và transaction history

### 3. Controllers

#### `ReputationRuleController` (Admin Only)
**Endpoint Base:** `/slib/admin/reputation-rules`

**APIs:**
- `GET /` - Lấy tất cả rules
- `GET /{id}` - Lấy rule theo ID
- `POST /` - Tạo rule mới
- `PUT /{id}` - Cập nhật rule
- `PATCH /{id}/toggle` - Bật/tắt rule
- `DELETE /{id}` - Xóa rule

**Security:** Chỉ ADMIN mới có quyền truy cập

### 4. DTOs

- `ReputationRuleRequest` - DTO cho create/update
- `ReputationRuleResponse` - DTO cho response

## Quy Trình Hoạt Động

### Ví dụ: Phạt Không Check-in

1. **Scheduler** chạy mỗi phút:
   ```java
   ReservationScheduler.checkLateCheckInsAndApplyPenalty()
   ```

2. **Phát hiện** user quá deadline 15 phút:
   ```java
   reputationService.applyNoShowPenalty(userId, seatCode, zoneName, reservationId)
   ```

3. **Service xử lý:**
   - Query rule `NO_SHOW` từ database
   - Kiểm tra rule active
   - Trừ 10 điểm từ `users.reputation_score`
   - Tạo activity log
   - Tạo point transaction với `rule_id` và `balance_after`

4. **Kết quả:**
   - User reputation: 100 → 90
   - Có log trong `activity_logs`
   - Có transaction trong `point_transactions`

## Cách Admin Thêm Rule Mới

### Bước 1: Tạo Rule qua API

```http
POST /slib/admin/reputation-rules
Content-Type: application/json
Authorization: Bearer <admin_token>

{
  "ruleCode": "NOISE_VIOLATION",
  "ruleName": "Gây ồn ào",
  "description": "Gây ồn ào trong khu vực thư viện",
  "points": -15,
  "ruleType": "PENALTY",
  "isActive": true
}
```

### Bước 2: Code áp dụng rule mới

**Option 1: Tạo helper method trong ReputationService**
```java
public boolean applyNoiseViolationPenalty(UUID userId, String location) {
    return applyReputationRule(
        userId,
        "NOISE_VIOLATION",
        "Phạt: Gây ồn ào",
        "Bạn đã gây ồn ào tại " + location,
        ActivityLogEntity.TYPE_VIOLATION,
        PointTransactionEntity.TYPE_PENALTY,
        null,
        location,
        null
    );
}
```

**Option 2: Gọi trực tiếp method chung**
```java
reputationService.applyReputationRule(
    userId,
    "NOISE_VIOLATION",
    "Phạt: Gây ồn ào",
    "Bạn đã gây ồn ào tại khu đọc sách",
    ActivityLogEntity.TYPE_VIOLATION,
    PointTransactionEntity.TYPE_PENALTY,
    null,
    "Khu đọc sách",
    null
);
```

### Bước 3: Không cần restart service!

Rule đã sẵn trong database → Service tự động query và áp dụng

## Rules Hiện Có

| Rule Code | Tên | Điểm | Loại | Mô Tả |
|-----------|-----|------|------|-------|
| NO_SHOW | Không đến sau khi đặt chỗ | -10 | PENALTY | Đặt chỗ nhưng không check-in trong 15 phút |
| LATE_CHECKOUT | Trả chỗ muộn | -5 | PENALTY | Trả chỗ quá thời gian quy định |
| NOISE_VIOLATION | Gây ồn ào | -15 | PENALTY | Gây ồn ào trong khu vực thư viện |
| UNAUTHORIZED_SEAT | Sử dụng ghế không đúng | -10 | PENALTY | Ngồi ghế đã được người khác đặt |
| CHECK_IN_BONUS | Bonus check-in đúng giờ | +2 | REWARD | Check-in đúng giờ như đã đặt |
| WEEKLY_PERFECT | Tuần hoàn hảo | +5 | REWARD | Không có vi phạm trong tuần |

## Best Practices

### 1. Naming Convention
- Rule code: UPPER_SNAKE_CASE (e.g., `NO_SHOW`, `LATE_CHECKOUT`)
- Activity type: TYPE_UPPER_SNAKE_CASE (e.g., `TYPE_LATE_CHECKIN_PENALTY`)

### 2. Points Policy
- Penalty: Số âm (-5, -10, -15)
- Reward: Số dương (+2, +5, +10)
- Reputation score: Min = 0, Max = không giới hạn (default = 100)

### 3. Transaction Types
- `TYPE_PENALTY` - Phạt chung
- `TYPE_REWARD` - Thưởng chung
- `TYPE_NO_SHOW_PENALTY` - Phạt không đến
- `TYPE_LATE_CHECKIN_PENALTY` - Phạt check-in muộn
- `TYPE_CHECK_OUT_LATE_PENALTY` - Phạt checkout muộn
- `TYPE_WEEKLY_BONUS` - Thưởng tuần

### 4. Activity Types
- `TYPE_LATE_CHECKIN_PENALTY` - Phạt check-in muộn
- `TYPE_NO_SHOW` - Không đến
- `TYPE_NFC_CONFIRM` - Check-in thành công
- `TYPE_VIOLATION` - Vi phạm chung

## Testing

### Test thêm rule mới

1. Tạo rule qua API
2. Kiểm tra trong database
3. Trigger event để test áp dụng rule
4. Verify:
   - User reputation có thay đổi?
   - Activity log có tạo?
   - Point transaction có đầy đủ thông tin (rule_id, balance_after)?

### Test tắt rule

1. PATCH `/slib/admin/reputation-rules/{id}/toggle`
2. Trigger event
3. Verify: Rule không được áp dụng

## Migration Guide

Nếu muốn thêm rule mới vào database khi deploy:

```sql
-- V16__add_custom_rules.sql
INSERT INTO reputation_rules (
    rule_code, rule_name, description, points, rule_type
) VALUES (
    'YOUR_RULE_CODE',
    'Tên rule',
    'Mô tả chi tiết',
    -10,  -- hoặc +10 nếu là reward
    'PENALTY'  -- hoặc 'REWARD'
)
ON CONFLICT (rule_code) DO NOTHING;
```

## Kết Luận

Hệ thống reputation đã được thiết kế **dynamic và scalable**:

✅ Admin có thể thêm/sửa/xóa rules mà không cần sửa code  
✅ Service tự động query và áp dụng rules từ database  
✅ Đầy đủ logs và transaction history  
✅ Dễ dàng mở rộng cho các rules mới  
✅ RESTful API để quản lý rules  

**Khi cần thêm rule mới:**
1. Admin tạo rule qua API hoặc database
2. Developer thêm helper method trong ReputationService (optional)
3. Gọi `reputationService.applyReputationRule()` ở nơi cần áp dụng
4. Done! ✨
