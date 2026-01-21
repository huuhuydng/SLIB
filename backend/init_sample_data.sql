-- ================================================
-- SLIB Database Initialization Script
-- Tạo dữ liệu mẫu cho hệ thống SLIB
-- ================================================

-- ===========================
-- 1. TẠO USER LIBRARIAN
-- ===========================
-- User: Nguyễn Hoàng Phúc, email: phucnhde170706@fpt.edu.vn, role: LIBRARIAN

INSERT INTO users (
    id,
    supabase_uid,
    student_code,
    full_name,
    email,
    role,
    reputation_score,
    is_active,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    gen_random_uuid(), -- Supabase UID giả định
    'DE170706',
    'Nguyễn Hoàng Phúc',
    'phucnhde170706@fpt.edu.vn',
    'LIBRARIAN',
    100,
    true,
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- ===========================
-- 2. TẠO AREAS (Khu vực thư viện)
-- ===========================
-- Tạo 3 khu vực chính

DO $$
DECLARE
    area_a_id UUID;
    area_b_id UUID;
    area_c_id UUID;
BEGIN
    -- Tạo Area A
    INSERT INTO areas (area_id, area_name, area_des, width, height, canvas_offset_x, canvas_offset_y, canvas_zoom)
    VALUES (gen_random_uuid(), 'Khu vực A', 'Khu tự học', 800, 600, 0, 0, 1.0)
    RETURNING area_id INTO area_a_id;

    -- Tạo Area B
    INSERT INTO areas (area_id, area_name, area_des, width, height, canvas_offset_x, canvas_offset_y, canvas_zoom)
    VALUES (gen_random_uuid(), 'Khu vực B', 'Khu yên tĩnh', 800, 600, 0, 0, 1.0)
    RETURNING area_id INTO area_b_id;

    -- Tạo Area C
    INSERT INTO areas (area_id, area_name, area_des, width, height, canvas_offset_x, canvas_offset_y, canvas_zoom)
    VALUES (gen_random_uuid(), 'Khu vực C', 'Khu thảo luận', 800, 600, 0, 0, 1.0)
    RETURNING area_id INTO area_c_id;

    -- Lưu area IDs vào biến tạm để sử dụng cho zones
    -- (Trong thực tế, bạn có thể lấy area_id từ query sau)
END $$;

-- ===========================
-- 3. TẠO ZONES (Khu vực con)
-- ===========================
-- Tạo zones cho mỗi area

DO $$
DECLARE
    area_a_id UUID;
    area_b_id UUID;
    area_c_id UUID;
    zone_a_id UUID;
    zone_b_id UUID;
    zone_c_id UUID;
BEGIN
    -- Lấy Area IDs
    SELECT area_id INTO area_a_id FROM areas WHERE area_name = 'Khu vực A' LIMIT 1;
    SELECT area_id INTO area_b_id FROM areas WHERE area_name = 'Khu vực B' LIMIT 1;
    SELECT area_id INTO area_c_id FROM areas WHERE area_name = 'Khu vực C' LIMIT 1;

    -- Zone A (trong Area A)
    INSERT INTO zones (zone_id, zone_name, zone_des, x, y, width, height, color, area_id)
    VALUES (gen_random_uuid(), 'Zone A', 'Khu tự học', 50, 50, 700, 500, '#FFE5B4', area_a_id)
    RETURNING zone_id INTO zone_a_id;

    -- Zone B (trong Area B)
    INSERT INTO zones (zone_id, zone_name, zone_des, x, y, width, height, color, area_id)
    VALUES (gen_random_uuid(), 'Zone B', 'Khu yên tĩnh', 50, 50, 700, 500, '#E6F3FF', area_b_id)
    RETURNING zone_id INTO zone_b_id;

    -- Zone C (trong Area C)
    INSERT INTO zones (zone_id, zone_name, zone_des, x, y, width, height, color, area_id)
    VALUES (gen_random_uuid(), 'Zone C', 'Khu thảo luận', 50, 50, 700, 500, '#F0E6FF', area_c_id)
    RETURNING zone_id INTO zone_c_id;

    -- ===========================
    -- 4. TẠO SEATS (Chỗ ngồi)
    -- ===========================
    -- Tạo ghế A1-A25, B1-B25, C1-C25

    -- Seats A1-A25 (trong Zone A)
    FOR i IN 1..25 LOOP
        INSERT INTO seats (
            seat_id,
            seat_number,
            x,
            y,
            width,
            height,
            rotation,
            is_active,
            seat_status,
            zone_id
        ) VALUES (
            gen_random_uuid(),
            'A' || i,
            100 + ((i - 1) % 5) * 120, -- 5 ghế mỗi hàng
            100 + ((i - 1) / 5) * 100,
            44,
            44,
            0,
            true,
            CASE 
                WHEN i = 6 THEN 'BOOKED'::seat_status
                ELSE 'AVAILABLE'::seat_status
            END,
            zone_a_id
        );
    END LOOP;

    -- Seats B1-B25 (trong Zone B)
    FOR i IN 1..25 LOOP
        INSERT INTO seats (
            seat_id,
            seat_number,
            x,
            y,
            width,
            height,
            rotation,
            is_active,
            seat_status,
            zone_id
        ) VALUES (
            gen_random_uuid(),
            'B' || i,
            100 + ((i - 1) % 5) * 120,
            100 + ((i - 1) / 5) * 100,
            44,
            44,
            0,
            true,
            'AVAILABLE'::seat_status,
            zone_b_id
        );
    END LOOP;

    -- Seats C1-C25 (trong Zone C)
    FOR i IN 1..25 LOOP
        INSERT INTO seats (
            seat_id,
            seat_number,
            x,
            y,
            width,
            height,
            rotation,
            is_active,
            seat_status,
            zone_id
        ) VALUES (
            gen_random_uuid(),
            'C' || i,
            100 + ((i - 1) % 5) * 120,
            100 + ((i - 1) / 5) * 100,
            44,
            44,
            0,
            true,
            'AVAILABLE'::seat_status,
            zone_c_id
        );
    END LOOP;

    -- ===========================
    -- 5. TẠO RESERVATION cho ghế A6
    -- ===========================
    -- Đặt ghế A6 từ 15:00-17:00 hôm nay

    DECLARE
        seat_a6_id UUID;
        user_id UUID;
    BEGIN
        -- Lấy seat A6 ID
        SELECT seat_id INTO seat_a6_id 
        FROM seats 
        WHERE seat_number = 'A6' AND zone_id = zone_a_id 
        LIMIT 1;

        -- Lấy user ID của Nguyễn Hoàng Phúc
        SELECT id INTO user_id 
        FROM users 
        WHERE email = 'phucnhde170706@fpt.edu.vn' 
        LIMIT 1;

        -- Tạo reservation
        IF seat_a6_id IS NOT NULL AND user_id IS NOT NULL THEN
            INSERT INTO reservations (
                reservation_id,
                user_id,
                seat_id,
                start_time,
                end_time,
                status,
                created_at
            ) VALUES (
                gen_random_uuid(),
                user_id,
                seat_a6_id,
                DATE_TRUNC('day', NOW()) + INTERVAL '15 hours', -- 15:00 hôm nay
                DATE_TRUNC('day', NOW()) + INTERVAL '17 hours', -- 17:00 hôm nay
                'CONFIRMED',
                NOW()
            );
        END IF;
    END;

END $$;

-- ===========================
-- 6. VERIFY DATA
-- ===========================
-- Kiểm tra dữ liệu đã tạo

SELECT 'Users created:' AS info, COUNT(*) AS count FROM users WHERE email = 'phucnhde170706@fpt.edu.vn';
SELECT 'Areas created:' AS info, COUNT(*) AS count FROM areas;
SELECT 'Zones created:' AS info, COUNT(*) AS count FROM zones;
SELECT 'Seats created:' AS info, COUNT(*) AS count FROM seats;
SELECT 'Seats A1-A25:' AS info, COUNT(*) AS count FROM seats WHERE seat_number LIKE 'A%';
SELECT 'Seats B1-B25:' AS info, COUNT(*) AS count FROM seats WHERE seat_number LIKE 'B%';
SELECT 'Seats C1-C25:' AS info, COUNT(*) AS count FROM seats WHERE seat_number LIKE 'C%';
SELECT 'Seat A6 status:' AS info, seat_status FROM seats WHERE seat_number = 'A6';
SELECT 'Reservations:' AS info, COUNT(*) AS count FROM reservations;

COMMIT;
