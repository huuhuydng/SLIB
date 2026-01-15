-- Insert test reservations for A6 (7-9h) and A7 (9-11h)
-- Current date: 2026-01-14

-- First, update seat_status to BOOKED for seats A6 and A7
UPDATE public.seats 
SET seat_status = 'BOOKED'
WHERE seat_code IN ('A6', 'A7');

-- Insert reservation for A6 (7:00-9:00)
-- Assuming user_id exists, using a sample UUID
INSERT INTO public.reservations (
    reservation_id,
    user_id,
    seat_id,
    start_time,
    end_time,
    status,
    created_at
)
VALUES (
    gen_random_uuid(),
    '5181c5a3-d41a-423d-8a3c-fff1b6bb5af0', -- Replace with actual user_id from your database
    (SELECT seat_id FROM public.seats WHERE seat_code = 'A6'),
    '2026-01-14 07:00:00'::timestamp,
    '2026-01-14 09:00:00'::timestamp,
    'confirmed',
    NOW()
);

-- Insert reservation for A7 (9:00-11:00)
INSERT INTO public.reservations (
    reservation_id,
    user_id,
    seat_id,
    start_time,
    end_time,
    status,
    created_at
)
VALUES (
    gen_random_uuid(),
    '5181c5a3-d41a-423d-8a3c-fff1b6bb5af0', -- Replace with actual user_id from your database
    (SELECT seat_id FROM public.seats WHERE seat_code = 'A7'),
    '2026-01-14 09:00:00'::timestamp,
    '2026-01-14 11:00:00'::timestamp,
    'confirmed',
    NOW()
);
