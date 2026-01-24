-- Insert reservation cho ghế A8 
-- Ngày: 23/01/2026, Khung giờ: 07:00 - 09:00

-- Xóa reservation cũ của A8 (nếu có)
DELETE FROM reservations WHERE seat_id = 8;

-- INSERT reservation mới cho A8
INSERT INTO reservations (
    reservation_id, 
    user_id, 
    seat_id, 
    start_time, 
    end_time, 
    status, 
    created_at,
    created_time
) VALUES (
    gen_random_uuid(),
    (SELECT user_id FROM users LIMIT 1),
    8, -- seat_id của A8
    '2026-01-23 07:00:00',
    '2026-01-23 09:00:00',
    'BOOKED',
    NOW(),
    NOW()
);
