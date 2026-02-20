-- V21: Đồng bộ reputation_rules với tất cả violation types
-- DB hiện tại chỉ có: NO_SHOW, LATE_CHECKOUT, NOISE_VIOLATION, UNAUTHORIZED_SEAT, CHECK_IN_BONUS, WEEKLY_PERFECT
-- Cần thêm: FEET_ON_SEAT, FOOD_DRINK, SLEEPING, LEFT_BELONGINGS, OTHER_VIOLATION

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
    ),
    (
        'OTHER_VIOLATION',
        'Vi phạm khác',
        'Các vi phạm khác không thuộc danh mục cụ thể',
        -5,
        'PENALTY'
    )
ON CONFLICT (rule_code) DO NOTHING;