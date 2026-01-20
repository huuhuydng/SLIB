-- =========================================
-- Fix reservations cho ghế A6 và A7
-- A6: 13:00-15:00 hôm nay
-- A7: 15:00-17:00 hôm nay
-- =========================================

DO $$
DECLARE
    v_user_id uuid;
    v_seat_a6_id integer;
    v_seat_a7_id integer;
    v_today date := CURRENT_DATE;
BEGIN
    -- Lấy user đầu tiên trong hệ thống để làm owner của reservation
    SELECT id INTO v_user_id FROM users LIMIT 1;
    
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Không tìm thấy user nào trong hệ thống';
    END IF;
    
    -- Lấy seat_id của A6 và A7
    SELECT seat_id INTO v_seat_a6_id FROM seats WHERE seat_code = 'A6';
    SELECT seat_id INTO v_seat_a7_id FROM seats WHERE seat_code = 'A7';
    
    -- Xóa tất cả reservations cũ của A6 và A7
    DELETE FROM reservations WHERE seat_id IN (v_seat_a6_id, v_seat_a7_id);
    RAISE NOTICE 'Đã xóa reservations cũ';
    
    -- Tạo reservation mới cho A6: 13:00-15:00 hôm nay
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
        v_user_id,
        v_seat_a6_id,
        v_today + INTERVAL '13 hours',
        v_today + INTERVAL '15 hours',
        'BOOKED',
        NOW(),
        NOW()
    );
    RAISE NOTICE 'Đã tạo reservation cho A6: 13:00-15:00 ngày %', v_today;
    
    -- Tạo reservation mới cho A7: 15:00-17:00 hôm nay
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
        v_user_id,
        v_seat_a7_id,
        v_today + INTERVAL '15 hours',
        v_today + INTERVAL '17 hours',
        'BOOKED',
        NOW(),
        NOW()
    );
    RAISE NOTICE 'Đã tạo reservation cho A7: 15:00-17:00 ngày %', v_today;
    
    -- Update seat status về AVAILABLE (để backend tính toán lại status dựa trên reservation)
    UPDATE seats SET seat_status = 'AVAILABLE'::seat_status 
    WHERE seat_code IN ('A6', 'A7');
    RAISE NOTICE 'Đã update seat status về AVAILABLE';
    
END $$;

-- Kiểm tra kết quả
SELECT 
    s.seat_code,
    s.seat_status,
    r.start_time,
    r.end_time,
    r.status as reservation_status,
    CASE 
        WHEN r.start_time::date = CURRENT_DATE THEN '✅ Hôm nay'
        ELSE '❌ Ngày khác'
    END as date_check
FROM seats s
LEFT JOIN reservations r ON s.seat_id = r.seat_id
WHERE s.seat_code IN ('A6', 'A7')
ORDER BY s.seat_code;

\echo ''
\echo '========================================='
\echo 'Hoàn thành!'
\echo 'A6: 13:00-15:00 hôm nay'
\echo 'A7: 15:00-17:00 hôm nay'
\echo '========================================='
