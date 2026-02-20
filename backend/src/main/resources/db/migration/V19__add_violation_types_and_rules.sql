-- V19: Mở rộng violation_type cho seat_violation_reports
-- Thêm: FEET_ON_SEAT, FOOD_DRINK, SLEEPING

-- Drop old constraint and add new one with expanded types
ALTER TABLE seat_violation_reports
DROP CONSTRAINT IF EXISTS seat_violation_reports_violation_type_check;

ALTER TABLE seat_violation_reports
ADD CONSTRAINT seat_violation_reports_violation_type_check CHECK (
    violation_type IN (
        'UNAUTHORIZED_USE',
        'LEFT_BELONGINGS',
        'NOISE',
        'FEET_ON_SEAT',
        'FOOD_DRINK',
        'SLEEPING',
        'OTHER'
    )
);

-- Thêm reputation rules mới cho các loại vi phạm
INSERT INTO
    reputation_rules (
        rule_code,
        rule_name,
        description,
        points,
        rule_type
    )
VALUES (
        'FEET_ON_SEAT',
        'Gác chân lên ghế/bàn',
        'Gác chân lên ghế hoặc bàn trong thư viện',
        -10,
        'PENALTY'
    ),
    (
        'FOOD_DRINK',
        'Ăn uống trong thư viện',
        'Ăn uống trong khu vực thư viện',
        -10,
        'PENALTY'
    ),
    (
        'SLEEPING',
        'Ngủ tại chỗ ngồi',
        'Ngủ tại chỗ ngồi trong thư viện',
        -5,
        'PENALTY'
    ),
    (
        'LEFT_BELONGINGS',
        'Để đồ giữ chỗ',
        'Để đồ trên ghế để giữ chỗ nhưng không có mặt',
        -10,
        'PENALTY'
    )
ON CONFLICT (rule_code) DO NOTHING;

-- Thêm notification type cho VIOLATION_REPORT
ALTER TABLE notifications
DROP CONSTRAINT IF EXISTS notifications_notification_type_check;

ALTER TABLE notifications
ADD CONSTRAINT notifications_notification_type_check CHECK (
    notification_type IN (
        'BOOKING',
        'REMINDER',
        'VIOLATION',
        'SYSTEM',
        'NEWS',
        'SUPPORT_REQUEST',
        'VIOLATION_REPORT'
    )
);