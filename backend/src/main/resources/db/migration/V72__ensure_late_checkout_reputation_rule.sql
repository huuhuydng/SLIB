-- Ensure the late checkout reputation rule always exists and remains active.

INSERT INTO reputation_rules (rule_code, rule_name, description, points, rule_type, is_active)
VALUES (
    'LATE_CHECKOUT',
    'Không check-out trong ngày',
    'Không thực hiện check-out thủ công trước khi hệ thống tự động check-out cuối ngày.',
    -5,
    'PENALTY',
    true
)
ON CONFLICT (rule_code) DO UPDATE
SET
    rule_name = EXCLUDED.rule_name,
    description = EXCLUDED.description,
    points = EXCLUDED.points,
    rule_type = EXCLUDED.rule_type,
    is_active = EXCLUDED.is_active;
