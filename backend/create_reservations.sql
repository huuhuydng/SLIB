-- Create reservations for A6 and A7 using existing user
DO $$
DECLARE
    v_seat_a6_id integer;
    v_seat_a7_id integer;
    v_user_id uuid;
BEGIN
    -- Get the first available user
    SELECT id INTO v_user_id FROM users LIMIT 1;
    
    -- Get seat IDs for A6 and A7
    SELECT seat_id INTO v_seat_a6_id FROM seats WHERE seat_code = 'A6';
    SELECT seat_id INTO v_seat_a7_id FROM seats WHERE seat_code = 'A7';
    
    -- Delete existing reservations for these seats (if any)
    DELETE FROM reservations WHERE seat_id IN (v_seat_a6_id, v_seat_a7_id);
    
    -- Create reservation for A6 (13:00-15:00 today)
    INSERT INTO reservations (reservation_id, user_id, seat_id, start_time, end_time, status, created_at, created_time)
    VALUES (
        gen_random_uuid(),
        v_user_id,
        v_seat_a6_id,
        CURRENT_DATE + INTERVAL '13 hours',
        CURRENT_DATE + INTERVAL '15 hours',
        'BOOKED',
        NOW(),
        NOW()
    );
    
    -- Create reservation for A7 (15:00-17:00 today)
    INSERT INTO reservations (reservation_id, user_id, seat_id, start_time, end_time, status, created_at, created_time)
    VALUES (
        gen_random_uuid(),
        v_user_id,
        v_seat_a7_id,
        CURRENT_DATE + INTERVAL '15 hours',
        CURRENT_DATE + INTERVAL '17 hours',
        'BOOKED',
        NOW(),
        NOW()
    );
    
    RAISE NOTICE 'Created reservations for user: %', v_user_id;
END $$;

-- Verify reservations
SELECT 
    s.seat_code,
    r.start_time,
    r.end_time,
    r.status,
    u.email,
    u.full_name
FROM reservations r
JOIN seats s ON r.seat_id = s.seat_id
JOIN users u ON r.user_id = u.id
WHERE s.seat_code IN ('A6', 'A7')
ORDER BY s.seat_code;
