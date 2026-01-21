-- Setup seats and restrictions for SLIB system
-- This script will:
-- 1. Create a default zone if not exists
-- 2. Insert seats A1-A30, B1-B30, C1-C30
-- 3. Set A1, A2 as UNAVAILABLE
-- 4. Create reservations for A6 (13:00-15:00) and A7 (15:00-17:00) with BOOKED status
-- 5. Set all other seats as AVAILABLE

BEGIN;

-- Step 1: Create default zone if not exists
INSERT INTO zones (zone_name, description, floor_number, map_data, created_at, updated_at)
VALUES (
    'Main Library',
    'Khu vực chính của thư viện',
    1,
    '{}',
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;

-- Get zone_id for reference
DO $$
DECLARE
    v_zone_id INTEGER;
    v_user_id UUID;
    seat_code VARCHAR(10);
    i INTEGER;
BEGIN
    -- Get the zone_id (assuming we just created or it's the first zone)
    SELECT zone_id INTO v_zone_id FROM zones ORDER BY zone_id LIMIT 1;
    
    -- Get a user_id for the reservations (using the first user, or create a system user)
    SELECT id INTO v_user_id FROM users ORDER BY created_at LIMIT 1;
    
    -- If no user exists, we'll skip reservations for now
    IF v_user_id IS NULL THEN
        RAISE NOTICE 'No user found. Reservations will not be created.';
    END IF;

    -- Step 2: Insert seats A1-A30
    FOR i IN 1..30 LOOP
        seat_code := 'A' || i;
        INSERT INTO seats (zone_id, seat_code, seat_status, position_x, position_y, row_number, column_number, width, height)
        VALUES (
            v_zone_id,
            seat_code,
            CASE 
                WHEN i = 1 OR i = 2 THEN 'UNAVAILABLE'::seat_status
                ELSE 'AVAILABLE'::seat_status
            END,
            (i - 1) * 100,  -- position_x
            0,              -- position_y (row A)
            1,              -- row_number
            i,              -- column_number
            80,             -- width
            80              -- height
        );
    END LOOP;

    -- Step 3: Insert seats B1-B30
    FOR i IN 1..30 LOOP
        seat_code := 'B' || i;
        INSERT INTO seats (zone_id, seat_code, seat_status, position_x, position_y, row_number, column_number, width, height)
        VALUES (
            v_zone_id,
            seat_code,
            'AVAILABLE'::seat_status,
            (i - 1) * 100,  -- position_x
            100,            -- position_y (row B)
            2,              -- row_number
            i,              -- column_number
            80,             -- width
            80              -- height
        );
    END LOOP;

    -- Step 4: Insert seats C1-C30
    FOR i IN 1..30 LOOP
        seat_code := 'C' || i;
        INSERT INTO seats (zone_id, seat_code, seat_status, position_x, position_y, row_number, column_number, width, height)
        VALUES (
            v_zone_id,
            seat_code,
            'AVAILABLE'::seat_status,
            (i - 1) * 100,  -- position_x
            200,            -- position_y (row C)
            3,              -- row_number
            i,              -- column_number
            80,             -- width
            80              -- height
        );
    END LOOP;

    RAISE NOTICE 'Successfully inserted 90 seats (A1-A30, B1-B30, C1-C30)';
    RAISE NOTICE 'A1 and A2 are set to UNAVAILABLE';

    -- Step 5: Create reservations for A6 and A7 (only if we have a user)
    IF v_user_id IS NOT NULL THEN
        -- Reservation for A6: 13:00-15:00 today
        INSERT INTO reservations (user_id, seat_id, start_time, end_time, status, created_at)
        SELECT 
            v_user_id,
            s.seat_id,
            CURRENT_DATE + TIME '13:00:00',
            CURRENT_DATE + TIME '15:00:00',
            'CONFIRMED',
            NOW()
        FROM seats s
        WHERE s.seat_code = 'A6';

        -- Update seat A6 status to BOOKED
        UPDATE seats SET seat_status = 'BOOKED'::seat_status WHERE seat_code = 'A6';

        -- Reservation for A7: 15:00-17:00 today
        INSERT INTO reservations (user_id, seat_id, start_time, end_time, status, created_at)
        SELECT 
            v_user_id,
            s.seat_id,
            CURRENT_DATE + TIME '15:00:00',
            CURRENT_DATE + TIME '17:00:00',
            'CONFIRMED',
            NOW()
        FROM seats s
        WHERE s.seat_code = 'A7';

        -- Update seat A7 status to BOOKED
        UPDATE seats SET seat_status = 'BOOKED'::seat_status WHERE seat_code = 'A7';

        RAISE NOTICE 'Created reservations for A6 (13:00-15:00) and A7 (15:00-17:00)';
    END IF;
END $$;

COMMIT;

-- Verify the results
SELECT 
    seat_code,
    seat_status,
    CASE 
        WHEN seat_status = 'UNAVAILABLE' THEN 'Bị hạn chế'
        WHEN seat_status = 'BOOKED' THEN 'Đã được book'
        WHEN seat_status = 'AVAILABLE' THEN 'Còn trống'
    END as status_description
FROM seats
WHERE seat_code IN ('A1', 'A2', 'A6', 'A7', 'B1', 'C1')
ORDER BY seat_code;
