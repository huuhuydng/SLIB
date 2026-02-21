-- V16_1: Seed initial users (LIBRARIAN and ADMIN)

-- Insert Nguyễn Hoàng Phúc - LIBRARIAN

INSERT INTO
    users (
        id,
        user_code,
        email,
        full_name,
        role,
        is_active,
        created_at,
        updated_at
    )
VALUES (
        gen_random_uuid (),
        'DE170706',
        'phucnhde170706@fpt.edu.vn',
        'Nguyễn Hoàng Phúc',
        'LIBRARIAN'::user_role,
        true,
        NOW(),
        NOW()
    )
ON CONFLICT (user_code) DO
UPDATE
SET
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role;

-- Insert Lê Phương Uyên - ADMIN

INSERT INTO
    users (
        id,
        user_code,
        email,
        full_name,
        role,
        is_active,
        created_at,
        updated_at
    )
VALUES (
        gen_random_uuid (),
        'DE180893',
        'uyenlpde180893@fpt.edu.vn',
        'Lê Phương Uyên',
        'ADMIN'::user_role,
        true,
        NOW(),
        NOW()
    )
ON CONFLICT (user_code) DO
UPDATE
SET
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role;