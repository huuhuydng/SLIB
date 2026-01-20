-- Tao user LIBRARIAN: Nguyen Hoang Phuc
-- Email: phucnhde170706@fpt.edu.vn
-- Password se duoc ma hoa bang Supabase Auth hoac can set sau

INSERT INTO "public"."users" (
    "id",
    "supabase_uid",
    "student_code",
    "full_name",
    "email",
    "role",
    "reputation_score",
    "is_active",
    "created_at",
    "updated_at"
) VALUES (
    gen_random_uuid(),
    gen_random_uuid(), -- Tam thoi dung random UUID, neu dung Supabase Auth thi can sync
    'LIBRARIAN001', -- Ma so cho librarian
    'Nguyen Hoang Phuc',
    'phucnhde170706@fpt.edu.vn',
    'LIBRARIAN', -- Role la LIBRARIAN
    100, -- Diem uy tin mac dinh
    true, -- Active
    NOW(),
    NOW()
)
ON CONFLICT (student_code) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    role = EXCLUDED.role;

-- Lay ID cua user vua tao va ID cua ghe A6
DO $$
DECLARE
    v_user_id uuid;
    v_seat_id integer;
BEGIN
    -- Lay user_id cua Phuc
    SELECT id INTO v_user_id
    FROM "public"."users"
    WHERE email = 'phucnhde170706@fpt.edu.vn'
    LIMIT 1;

    -- Lay seat_id cua ghe A6
    SELECT seat_id INTO v_seat_id
    FROM "public"."seats"
    WHERE seat_code = 'A6'
    LIMIT 1;

    -- Kiem tra neu tim thay ca user va seat
    IF v_user_id IS NOT NULL AND v_seat_id IS NOT NULL THEN
        -- Tao reservation cho khung gio 7h-9h hom nay
        INSERT INTO "public"."reservations" (
            "reservation_id",
            "user_id",
            "seat_id",
            "start_time",
            "end_time",
            "status",
            "created_at",
            "created_time"
        ) VALUES (
            gen_random_uuid(),
            v_user_id,
            v_seat_id,
            CURRENT_DATE + INTERVAL '7 hours', -- 7:00 sang hom nay
            CURRENT_DATE + INTERVAL '9 hours', -- 9:00 sang hom nay
            'PENDING', -- Status: PENDING, CONFIRMED, CANCELLED, COMPLETED
            NOW(),
            NOW()
        );

        RAISE NOTICE 'Da tao reservation cho user % tai ghe A6 (ID: %) tu 7h-9h', v_user_id, v_seat_id;
    ELSE
        RAISE NOTICE 'Khong tim thay user hoac ghe A6. User ID: %, Seat ID: %', v_user_id, v_seat_id;
    END IF;
END $$;

-- Kiem tra ket qua
SELECT 
    u.full_name,
    u.email,
    u.role,
    u.student_code,
    u.is_active
FROM "public"."users" u
WHERE u.email = 'phucnhde170706@fpt.edu.vn';

SELECT 
    r.reservation_id,
    u.full_name,
    s.seat_code,
    r.start_time,
    r.end_time,
    r.status
FROM "public"."reservations" r
JOIN "public"."users" u ON r.user_id = u.id
JOIN "public"."seats" s ON r.seat_id = s.seat_id
WHERE u.email = 'phucnhde170706@fpt.edu.vn'
ORDER BY r.created_at DESC;
