-- Script to set up seat restrictions with time slots
-- A1, A2: UNAVAILABLE (permanently restricted)
-- A6: BOOKED with reservation 13:00-15:00
-- A7: BOOKED with reservation 15:00-17:00
-- All others: AVAILABLE

-- First, set all seats to AVAILABLE by default
UPDATE seats 
SET seat_status = 'AVAILABLE'
WHERE seat_status IS NULL OR seat_status != 'AVAILABLE';

-- Set A1, A2 as UNAVAILABLE (permanently restricted)
UPDATE seats 
SET seat_status = 'UNAVAILABLE'
WHERE seat_code IN ('A1', 'A2');

-- Set A6 as BOOKED
UPDATE seats 
SET seat_status = 'BOOKED'
WHERE seat_code = 'A6';

-- Set A7 as BOOKED
UPDATE seats 
SET seat_status = 'BOOKED'
WHERE seat_code = 'A7';

-- Create reservation for A6 (13:00-15:00 today)
-- First get the user_id for phucnhde170706@fpt.edu.vn and seat_id for A6
DO $$
DECLARE
    v_user_id UUID;
    v_seat_id_a6 INTEGER;
    v_seat_id_a7 INTEGER;
    v_today DATE := CURRENT_DATE;
BEGIN
    -- Get user_id (using the librarian account)
    SELECT user_id INTO v_user_id 
    FROM users 
    WHERE email = 'phucnhde170706@fpt.edu.vn' 
    LIMIT 1;

    -- If user doesn't exist, create a dummy reservation user
    IF v_user_id IS NULL THEN
        INSERT INTO users (user_id, email, full_name, role, password, student_code)
        VALUES (gen_random_uuid(), 'system@reservation.local', 'System Reservation', 'LIBRARIAN', 'N/A', 'SYSTEM001')
        RETURNING user_id INTO v_user_id;
    END IF;

    -- Get seat_id for A6
    SELECT seat_id INTO v_seat_id_a6 
    FROM seats 
    WHERE seat_code = 'A6';

    -- Get seat_id for A7
    SELECT seat_id INTO v_seat_id_a7 
    FROM seats 
    WHERE seat_code = 'A7';

    -- Delete existing reservations for A6 and A7 to avoid conflicts
    DELETE FROM reservations WHERE seat_id IN (v_seat_id_a6, v_seat_id_a7);

    -- Create reservation for A6 (13:00-15:00)
    IF v_seat_id_a6 IS NOT NULL THEN
        INSERT INTO reservations (reservation_id, user_id, seat_id, start_time, end_time, status, created_at)
        VALUES (
            gen_random_uuid(),
            v_user_id,
            v_seat_id_a6,
            v_today + TIME '13:00:00',
            v_today + TIME '15:00:00',
            'CONFIRMED',
            NOW()
        );
    END IF;

    -- Create reservation for A7 (15:00-17:00)
    IF v_seat_id_a7 IS NOT NULL THEN
        INSERT INTO reservations (reservation_id, user_id, seat_id, start_time, end_time, status, created_at)
        VALUES (
            gen_random_uuid(),
            v_user_id,
            v_seat_id_a7,
            v_today + TIME '15:00:00',
            v_today + TIME '17:00:00',
            'CONFIRMED',
            NOW()
        );
    END IF;
END $$;

-- Verify the changes
SELECT 
    s.seat_code,
    s.seat_status,
    r.start_time,
    r.end_time,
    r.status as reservation_status
FROM seats s
LEFT JOIN reservations r ON s.seat_id = r.seat_id
WHERE s.seat_code IN ('A1', 'A2', 'A6', 'A7')
ORDER BY s.seat_code;
