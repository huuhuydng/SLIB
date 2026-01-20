-- =========================================
-- Complete Seat Setup Script
-- Based on actual database schema
-- =========================================

-- Step 1: Create Area (Main Study Area)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM areas WHERE area_name = 'Main Study Area') THEN
        INSERT INTO areas (area_name, height, is_active, locked, position_x, position_y, width)
        VALUES ('Main Study Area', 300, true, false, 0, 0, 1000);
    END IF;
END $$;

-- Step 2: Get the area_id for use in zones
DO $$
DECLARE
    v_area_id bigint;
BEGIN
    SELECT area_id INTO v_area_id FROM areas WHERE area_name = 'Main Study Area';
    
    -- Create Zone A
    IF NOT EXISTS (SELECT 1 FROM zones WHERE zone_name = 'Zone A') THEN
        INSERT INTO zones (zone_name, zone_des, has_power_outlet, height, is_locked, position_x, position_y, width, area_id)
        VALUES ('Zone A', 'Zone A - Left section', true, 100, false, 0, 0, 300, v_area_id);
    END IF;
    
    -- Create Zone B
    IF NOT EXISTS (SELECT 1 FROM zones WHERE zone_name = 'Zone B') THEN
        INSERT INTO zones (zone_name, zone_des, has_power_outlet, height, is_locked, position_x, position_y, width, area_id)
        VALUES ('Zone B', 'Zone B - Middle section', true, 100, false, 350, 0, 300, v_area_id);
    END IF;
    
    -- Create Zone C
    IF NOT EXISTS (SELECT 1 FROM zones WHERE zone_name = 'Zone C') THEN
        INSERT INTO zones (zone_name, zone_des, has_power_outlet, height, is_locked, position_x, position_y, width, area_id)
        VALUES ('Zone C', 'Zone C - Right section', true, 100, false, 700, 0, 300, v_area_id);
    END IF;
END $$;

-- Step 3: Clear existing seats (if any)
DELETE FROM reservations WHERE seat_id IN (SELECT seat_id FROM seats);
DELETE FROM seats;

-- Step 4: Insert seats for Zone A (A1-A30)
-- A1, A2 are UNAVAILABLE, others are AVAILABLE
INSERT INTO seats (seat_code, zone_id, seat_status, position_x, position_y, row_number, column_number, height, width)
SELECT 
    'A' || s.seat_num,
    (SELECT zone_id FROM zones WHERE zone_name = 'Zone A'),
    CASE 
        WHEN s.seat_num IN (1, 2) THEN 'UNAVAILABLE'::seat_status
        ELSE 'AVAILABLE'::seat_status
    END,
    ((s.seat_num - 1) % 6) * 50,
    ((s.seat_num - 1) / 6) * 50,
    ((s.seat_num - 1) / 6) + 1,
    ((s.seat_num - 1) % 6) + 1,
    50,
    50
FROM generate_series(1, 30) AS s(seat_num);

-- Step 5: Insert seats for Zone B (B1-B30)
-- All AVAILABLE
INSERT INTO seats (seat_code, zone_id, seat_status, position_x, position_y, row_number, column_number, height, width)
SELECT 
    'B' || s.seat_num,
    (SELECT zone_id FROM zones WHERE zone_name = 'Zone B'),
    'AVAILABLE'::seat_status,
    ((s.seat_num - 1) % 6) * 50,
    ((s.seat_num - 1) / 6) * 50,
    ((s.seat_num - 1) / 6) + 1,
    ((s.seat_num - 1) % 6) + 1,
    50,
    50
FROM generate_series(1, 30) AS s(seat_num);

-- Step 6: Insert seats for Zone C (C1-C30)
-- All AVAILABLE
INSERT INTO seats (seat_code, zone_id, seat_status, position_x, position_y, row_number, column_number, height, width)
SELECT 
    'C' || s.seat_num,
    (SELECT zone_id FROM zones WHERE zone_name = 'Zone C'),
    'AVAILABLE'::seat_status,
    ((s.seat_num - 1) % 6) * 50,
    ((s.seat_num - 1) / 6) * 50,
    ((s.seat_num - 1) / 6) + 1,
    ((s.seat_num - 1) % 6) + 1,
    50,
    50
FROM generate_series(1, 30) AS s(seat_num);

-- Step 7: Update seat status for A6 and A7 to BOOKED
UPDATE seats SET seat_status = 'BOOKED'::seat_status WHERE seat_code IN ('A6', 'A7');

-- Step 8: Create reservations for A6 (13:00-15:00) and A7 (15:00-17:00)
-- Note: Using a test user_id. You may need to update this with actual user UUID
DO $$
DECLARE
    v_seat_a6_id integer;
    v_seat_a7_id integer;
    v_test_user_id uuid := '00000000-0000-0000-0000-000000000001'; -- Placeholder, update with actual user
BEGIN
    -- Get seat IDs
    SELECT seat_id INTO v_seat_a6_id FROM seats WHERE seat_code = 'A6';
    SELECT seat_id INTO v_seat_a7_id FROM seats WHERE seat_code = 'A7';
    
    -- Create reservation for A6 (13:00-15:00 today)
    INSERT INTO reservations (reservation_id, user_id, seat_id, start_time, end_time, status, created_at, created_time)
    VALUES (
        gen_random_uuid(),
        v_test_user_id,
        v_seat_a6_id,
        CURRENT_DATE + INTERVAL '13 hours',
        CURRENT_DATE + INTERVAL '15 hours',
        'ACTIVE',
        NOW(),
        NOW()
    )
    ON CONFLICT DO NOTHING;
    
    -- Create reservation for A7 (15:00-17:00 today)
    INSERT INTO reservations (reservation_id, user_id, seat_id, start_time, end_time, status, created_at, created_time)
    VALUES (
        gen_random_uuid(),
        v_test_user_id,
        v_seat_a7_id,
        CURRENT_DATE + INTERVAL '15 hours',
        CURRENT_DATE + INTERVAL '17 hours',
        'ACTIVE',
        NOW(),
        NOW()
    )
    ON CONFLICT DO NOTHING;
END $$;

-- =========================================
-- VERIFICATION QUERIES
-- =========================================

\echo ''
\echo '========================================='
\echo 'VERIFICATION RESULTS'
\echo '========================================='

\echo ''
\echo '1. Areas:'
SELECT area_id, area_name, is_active FROM areas;

\echo ''
\echo '2. Zones:'
SELECT zone_id, zone_name, zone_des, area_id FROM zones;

\echo ''
\echo '3. Seats summary by zone and status:'
SELECT 
    z.zone_name,
    s.seat_status,
    COUNT(*) as seat_count
FROM seats s
JOIN zones z ON s.zone_id = z.zone_id
GROUP BY z.zone_name, s.seat_status
ORDER BY z.zone_name, s.seat_status;

\echo ''
\echo '4. Total seats count:'
SELECT COUNT(*) as total_seats FROM seats;

\echo ''
\echo '5. Unavailable and Booked seats:'
SELECT s.seat_code, s.seat_status, z.zone_name
FROM seats s
JOIN zones z ON s.zone_id = z.zone_id
WHERE s.seat_status IN ('UNAVAILABLE', 'BOOKED')
ORDER BY s.seat_code;

\echo ''
\echo '6. Active reservations for today:'
SELECT 
    s.seat_code,
    r.start_time,
    r.end_time,
    r.status
FROM reservations r
JOIN seats s ON r.seat_id = s.seat_id
WHERE DATE(r.start_time) = CURRENT_DATE
ORDER BY r.start_time;

\echo ''
\echo '========================================='
\echo 'Setup completed successfully!'
\echo 'Total: 90 seats created'
\echo '- A1, A2: UNAVAILABLE (permanent)'
\echo '- A6: BOOKED (13:00-15:00)'
\echo '- A7: BOOKED (15:00-17:00)'
\echo '- All others: AVAILABLE'
\echo '========================================='
