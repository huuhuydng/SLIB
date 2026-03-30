-- Seed default reputation rules
INSERT INTO
    reputation_rules (
        rule_code,
        rule_name,
        description,
        points,
        rule_type,
        is_active
    )
VALUES
    -- Quy tắc trừ điểm (Vi phạm)
    (
        'NO_SHOW',
        'Không quét NFC',
        'Đặt chỗ nhưng không quét NFC xác nhận trong thời gian quy định',
        -10,
        'PENALTY',
        true
    ),
    (
        'LATE_CHECKOUT',
        'Trả chỗ muộn',
        'Trả ghế muộn hơn thời gian quy định',
        -5,
        'PENALTY',
        true
    ),
    (
        'NOISE_VIOLATION',
        'Gây ồn ào',
        'Vi phạm quy định về tiếng ồn trong thư viện',
        -10,
        'PENALTY',
        true
    ),
    (
        'FOOD_DRINK',
        'Ăn uống trong thư viện',
        'Mang đồ ăn/nước uống vào khu vực cấm',
        -8,
        'PENALTY',
        true
    ),
    (
        'PHONE_NOISE',
        'Sử dụng điện thoại gây ồn',
        'Nghe gọi điện thoại trong khu yên tĩnh',
        -5,
        'PENALTY',
        true
    ),
    (
        'SLEEPING',
        'Ngủ trong thư viện',
        'Ngủ tại bàn học quá 30 phút',
        -5,
        'PENALTY',
        true
    ),
    (
        'FEET_ON_SEAT',
        'Gác chân lên ghế/bàn',
        'Gác chân lên ghế hoặc bàn học',
        -5,
        'PENALTY',
        true
    ),
    (
        'UNAUTHORIZED_SEAT',
        'Sử dụng ghế không đúng',
        'Ngồi ghế không đúng theo đặt chỗ',
        -8,
        'PENALTY',
        true
    ),
    (
        'LEFT_BELONGINGS',
        'Để đồ giữ chỗ',
        'Để đồ đạc giữ chỗ khi không có mặt',
        -5,
        'PENALTY',
        true
    ),
    (
        'OTHER_VIOLATION',
        'Vi phạm khác',
        'Các vi phạm khác do thư viện viên ghi nhận',
        -5,
        'PENALTY',
        true
    ),
    -- Quy tắc cộng điểm (Thưởng)
    (
        'CHECK_IN_BONUS',
        'Quét NFC đúng giờ',
        'Quét NFC xác nhận ghế đúng giờ',
        2,
        'REWARD',
        true
    ),
    (
        'WEEKLY_PERFECT',
        'Tuần hoàn hảo',
        'Không có vi phạm nào trong tuần',
        5,
        'REWARD',
        true
    )
ON CONFLICT (rule_code) DO NOTHING;