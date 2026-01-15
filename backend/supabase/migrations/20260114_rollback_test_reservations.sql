-- Rollback test reservations for A6 and A7
-- This will restore the database to the state before test data was inserted

-- Delete reservations for A6 and A7 on 2026-01-14
DELETE FROM public.reservations 
WHERE seat_id IN (
    SELECT seat_id FROM public.seats WHERE seat_code IN ('A6', 'A7')
)
AND start_time::date = '2026-01-14'::date;

-- Reset seat_status back to AVAILABLE for A6 and A7
UPDATE public.seats 
SET seat_status = 'AVAILABLE'
WHERE seat_code IN ('A6', 'A7');
