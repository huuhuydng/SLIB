-- Normalize reputation rule values and deactivate unused legacy rules.

INSERT INTO reputation_rules (rule_code, rule_name, description, points, rule_type, is_active)
VALUES
    ('NO_SHOW', 'Không quét NFC', 'Đặt chỗ nhưng không quét NFC xác nhận trong thời gian quy định.', -10, 'PENALTY', true),
    ('LATE_CHECKOUT', 'Không check-out trong ngày', 'Không thực hiện check-out thủ công trước khi hệ thống tự động check-out cuối ngày.', -5, 'PENALTY', true),
    ('NOISE_VIOLATION', 'Gây ồn ào', 'Vi phạm quy định về tiếng ồn trong thư viện.', -10, 'PENALTY', true),
    ('FOOD_DRINK', 'Ăn uống trong thư viện', 'Mang đồ ăn hoặc nước uống vào khu vực cấm.', -8, 'PENALTY', true),
    ('SLEEPING', 'Ngủ tại chỗ ngồi', 'Ngủ tại chỗ ngồi trong thư viện.', -5, 'PENALTY', true),
    ('FEET_ON_SEAT', 'Gác chân lên ghế/bàn', 'Gác chân lên ghế hoặc bàn trong thư viện.', -5, 'PENALTY', true),
    ('UNAUTHORIZED_SEAT', 'Sử dụng ghế không đúng', 'Ngồi ghế không đúng theo đặt chỗ.', -8, 'PENALTY', true),
    ('LEFT_BELONGINGS', 'Để đồ giữ chỗ', 'Để đồ đạc giữ chỗ khi không có mặt.', -8, 'PENALTY', true),
    ('OTHER_VIOLATION', 'Vi phạm khác', 'Các vi phạm khác do thủ thư xác nhận.', -5, 'PENALTY', true),
    ('CHECK_IN_BONUS', 'Quét NFC đúng giờ', 'Quét NFC xác nhận ghế đúng giờ.', 2, 'REWARD', true),
    ('WEEKLY_PERFECT', 'Tuần hoàn hảo', 'Không có vi phạm hoặc penalty nào trong tuần.', 5, 'REWARD', true)
ON CONFLICT (rule_code) DO UPDATE
SET
    rule_name = EXCLUDED.rule_name,
    description = EXCLUDED.description,
    points = EXCLUDED.points,
    rule_type = EXCLUDED.rule_type,
    is_active = EXCLUDED.is_active;

UPDATE reputation_rules
SET
    rule_name = 'Không sử dụng',
    description = 'Quy tắc cũ đã được gộp vào Gây ồn ào.',
    is_active = false
WHERE rule_code = 'PHONE_NOISE';
