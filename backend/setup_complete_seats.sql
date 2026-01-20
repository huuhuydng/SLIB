-- SQL script to create areas, zones, seats and restrictions
-- Run this after starting the PostgreSQL database
-- Usage: psql -U postgres -d slib -h localhost -p 5432 -f setup_complete_seats.sql

-- =========================================
-- Step 1: Create area (if not exists)
-- =========================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM areas WHERE area_id = 1) THEN
        INSERT INTO areas (area_name, description, is_active, created_at, updated_at)
        VALUES ('Main Study Area', 'Main library study area with reading seats', true, NOW(), NOW());
        RAISE NOTICE 'Area created';
    ELSE
        RAISE NOTICE 'Area already exists';
    END IF;
END $$;

-- =========================================
-- Step 2: Insert 3 zones (A, B, C)
-- =========================================
DO $$
BEGIN
    -- Zone A
    IF NOT EXISTS (SELECT 1 FROM zones WHERE zone_name = 'Zone A') THEN
        INSERT INTO zones (zone_name, zone_des, has_power_outlet, height, is_locked, position_x, position_y, width, area_id)
        VALUES ('Zone A', 'Zone A - Left section', true, 100, false, 0, 0, 300, 1);
        RAISE NOTICE 'Zone A created';
    END IF;
    
    -- Zone B
    IF NOT EXISTS (SELECT 1 FROM zones WHERE zone_name = 'Zone B') THEN
        INSERT INTO zones (zone_name, zone_des, has_power_outlet, height, is_locked, position_x, position_y, width, area_id)
        VALUES ('Zone B', 'Zone B - Middle section', true, 100, false, 310, 0, 300, 1);
        RAISE NOTICE 'Zone B created';
    END IF;
    
    -- Zone C
    IF NOT EXISTS (SELECT 1 FROM zones WHERE zone_name = 'Zone C') THEN
        INSERT INTO zones (zone_name, zone_des, has_power_outlet, height, is_locked, position_x, position_y, width, area_id)
        VALUES ('Zone C', 'Zone C - Right section', true, 100, false, 620, 0, 300, 1);
        RAISE NOTICE 'Zone C created';
    END IF;
END $$;

-- =========================================
-- Step 3: Insert 90 seats (30 per zone)
-- =========================================

-- Zone A seats (A1-A30)
-- A1, A2 will be UNAVAILABLE, others AVAILABLE
INSERT INTO seats (seat_code, zone_id, status, position_x, position_y, created_at, updated_at)
SELECT 
    'A' || s.seat_num,
    (SELECT zone_id FROM zones WHERE zone_name = 'Zone A'),
    CASE 
        WHEN s.seat_num IN (1, 2) THEN 'UNAVAILABLE'
        ELSE 'AVAILABLE'
    END,
    ((s.seat_num - 1) % 6) * 50,
    ((s.seat_num - 1) / 6) * 50,
    NOW(),
    NOW()
FROM generate_series(1, 30) AS s(seat_num)
WHERE NOT EXISTS (
    SELECT 1 FROM seats 
    WHERE seat_code = 'A' || s.seat_num
);

-- Zone B seats (B1-B30) - All AVAILABLE
INSERT INTO seats (seat_code, zone_id, status, position_x, position_y, created_at, updated_at)
SELECT 
    'B' || s.seat_num,
    (SELECT zone_id FROM zones WHERE zone_name = 'Zone B'),
    'AVAILABLE',
    ((s.seat_num - 1) % 6) * 50,
    ((s.seat_num - 1) / 6) * 50,
    NOW(),
    NOW()
FROM generate_series(1, 30) AS s(seat_num)
WHERE NOT EXISTS (
    SELECT 1 FROM seats 
    WHERE seat_code = 'B' || s.seat_num
);

-- Zone C seats (C1-C30) - All AVAILABLE
INSERT INTO seats (seat_code, zone_id, status, position_x, position_y, created_at, updated_at)
SELECT 
    'C' || s.seat_num,
    (SELECT zone_id FROM zones WHERE zone_name = 'Zone C'),
    'AVAILABLE',
    ((s.seat_num - 1) % 6) * 50,
    ((s.seat_num - 1) / 6) * 50,
    NOW(),
    NOW()
FROM generate_series(1, 30) AS s(seat_num)
WHERE NOT EXISTS (
    SELECT 1 FROM seats 
    WHERE seat_code = 'C' || s.seat_num
);

-- =========================================
-- Step 4: Create time-based reservations
-- =========================================

DO $$
DECLARE
    v_user_id UUID;
    v_seat_a6_id INTEGER;
    v_seat_a7_id INTEGER;
BEGIN
    -- Get or create a test user for reservations
    SELECT id INTO v_user_id 
    FROM users 
    WHERE role = 'STUDENT' 
    LIMIT 1;
    
    -- If no student exists, use any user
    IF v_user_id IS NULL THEN
        SELECT id INTO v_user_id FROM users LIMIT 1;
    END IF;
    
    -- Get seat IDs
    SELECT seat_id INTO v_seat_a6_id FROM seats WHERE seat_code = 'A6';
    SELECT seat_id INTO v_seat_a7_id FROM seats WHERE seat_code = 'A7';
    
    -- Only create reservations if user and seats exist
    IF v_user_id IS NOT NULL AND v_seat_a6_id IS NOT NULL THEN
        -- Reservation for A6 (13:00-15:00 today)
        INSERT INTO reservations (user_id, seat_id, start_time, end_time, status, created_at, updated_at)
        VALUES (
            v_user_id,
            v_seat_a6_id,
            CURRENT_DATE + INTERVAL '13 hours',
            CURRENT_DATE + INTERVAL '15 hours',
            'BOOKED',
            NOW(),
            NOW()
        )
        ON CONFLICT DO NOTHING;
        RAISE NOTICE 'Reservation created for A6 (13:00-15:00)';
    END IF;
    
    IF v_user_id IS NOT NULL AND v_seat_a7_id IS NOT NULL THEN
        -- Reservation for A7 (15:00-17:00 today)
        INSERT INTO reservations (user_id, seat_id, start_time, end_time, status, created_at, updated_at)
        VALUES (
            v_user_id,
            v_seat_a7_id,
            CURRENT_DATE + INTERVAL '15 hours',
            CURRENT_DATE + INTERVAL '17 hours',
            'BOOKED',
            NOW(),
            NOW()
        )
        ON CONFLICT DO NOTHING;
        RAISE NOTICE 'Reservation created for A7 (15:00-17:00)';
    END IF;
END $$;

-- =========================================
-- Verification Queries
-- =========================================

\echo '========================================='
\echo 'VERIFICATION RESULTS'
\echo '========================================='

\echo ''
\echo '1. Areas:'
SELECT area_id, area_name, is_active FROM areas;

\echo ''
\echo '2. Zones:'
SELECT zone_id, zone_name, zone_des, area_id FROM zones ORDER BY zone_name;

\echo ''
\echo '3. Seats summary by zone and status:'
SELECT 
    z.zone_name,
    s.status,
    COUNT(*) as count
FROM seats s
JOIN zones z ON s.zone_id = z.zone_id
GROUP BY z.zone_name, s.status
ORDER BY z.zone_name, s.status;

\echo ''
\echo '4. Total seats count:'
SELECT COUNT(*) as total_seats FROM seats;

\echo ''
\echo '5. Unavailable seats:'
SELECT s.seat_code, s.status, z.zone_name
FROM seats s
JOIN zones z ON s.zone_id = z.zone_id
WHERE s.status = 'UNAVAILABLE'
ORDER BY s.seat_code;

\echo ''
\echo '6. Active reservations for today:'
SELECT 
    s.seat_code,
    TO_CHAR(r.start_time, 'HH24:MI') as start_time,
    TO_CHAR(r.end_time, 'HH24:MI') as end_time,
    r.status,
    u.full_name
FROM reservations r
JOIN seats s ON r.seat_id = s.seat_id
JOIN users u ON r.user_id = u.id
WHERE DATE(r.start_time) = CURRENT_DATE
ORDER BY s.seat_code;

\echo ''
\echo '========================================='
\echo 'Setup completed successfully!'
\echo '========================================='
