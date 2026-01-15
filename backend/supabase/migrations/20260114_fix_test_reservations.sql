-- Fix test reservations - Xóa dữ liệu cũ SAI và tạo lại ĐÚNG
-- Current date: 2026-01-14

-- 1. XÓA reservation cũ (nếu có)
DELETE FROM public.reservations 
WHERE seat_id IN (
    SELECT seat_id FROM public.seats WHERE seat_code IN ('A6', 'A7')
);

-- 2. ✅ RESET seat_status về AVAILABLE (QUAN TRỌNG!)
-- seat_status KHÔNG BAO GIỜ được set thành BOOKED!
UPDATE public.seats
SET seat_status = 'AVAILABLE'
WHERE seat_code IN ('A6', 'A7');

-- 3. Tạo reservation MỚI - CHỈ A6 từ 9:00-11:00
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
    '5181c5a3-d41a-423d-8a3c-fff1b6bb5af0',
    (SELECT seat_id FROM public.seats WHERE seat_code = 'A6'),
    '2026-01-14 09:00:00'::timestamp,
    '2026-01-14 11:00:00'::timestamp,
    'BOOKED', -- ✅ Dùng status mới
    NOW()
);

-- 4. Verify
SELECT 
    s.seat_code,
    s.seat_status AS "Seat Status in DB (should be AVAILABLE)",
    r.start_time,
    r.end_time,
    r.status AS "Reservation Status"
FROM public.seats s
LEFT JOIN public.reservations r ON s.seat_id = r.seat_id
WHERE s.seat_code IN ('A6', 'A7')
ORDER BY s.seat_code;
