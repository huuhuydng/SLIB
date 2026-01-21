-- =========================================
-- Script thêm ghế A6 và A7 (nếu chưa có)
-- =========================================

DO $$
DECLARE
    v_zone_a_id bigint;
    v_user_id uuid;
BEGIN
    -- Lấy zone_id của Zone A
    SELECT zone_id INTO v_zone_a_id 
    FROM zones 
    WHERE zone_name LIKE '%Zone A%' OR zone_name LIKE '%Khu%học%'
    ORDER BY zone_id 
    LIMIT 1;
    
    IF v_zone_a_id IS NULL THEN
        RAISE EXCEPTION 'Không tìm thấy Zone A trong database';
    END IF;
    
    -- Thêm ghế A6 nếu chưa tồn tại
    IF NOT EXISTS (SELECT 1 FROM seats WHERE seat_code = 'A6') THEN
        INSERT INTO seats (
            seat_code,
            seat_status,
            zone_id,
            row_number,
            column_number,
            position_x,
            position_y,
            width,
            height
        ) VALUES (
            'A6',
            'AVAILABLE'::seat_status,
            v_zone_a_id,
            1,  -- row 1
            6,  -- column 6
            250,  -- position_x (5 * 50)
            0,    -- position_y
            50,   -- width
            50    -- height
        );
        RAISE NOTICE 'Đã thêm ghế A6';
    ELSE
        RAISE NOTICE 'Ghế A6 đã tồn tại';
    END IF;
    
    -- Thêm ghế A7 nếu chưa tồn tại
    IF NOT EXISTS (SELECT 1 FROM seats WHERE seat_code = 'A7') THEN
        INSERT INTO seats (
            seat_code,
            seat_status,
            zone_id,
            row_number,
            column_number,
            position_x,
            position_y,
            width,
            height
        ) VALUES (
            'A7',
            'AVAILABLE'::seat_status,
            v_zone_a_id,
            1,  -- row 1
            7,  -- column 7
            300,  -- position_x (6 * 50)
            0,    -- position_y
            50,   -- width
            50    -- height
        );
        RAISE NOTICE 'Đã thêm ghế A7';
    ELSE
        RAISE NOTICE 'Ghế A7 đã tồn tại';
    END IF;
    
END $$;

-- Kiểm tra kết quả
SELECT 
    seat_id,
    seat_code,
    seat_status,
    zone_id,
    row_number,
    column_number
FROM seats 
WHERE seat_code IN ('A6', 'A7')
ORDER BY seat_code;

\echo ''
\echo '========================================='
\echo 'Hoàn thành! Kiểm tra ghế A6 và A7 ở trên'
\echo '========================================='
