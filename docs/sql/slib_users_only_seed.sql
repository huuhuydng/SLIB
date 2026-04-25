-- User-only seed data for SLIB
-- Source: user-related rows found in Flyway migrations
-- Scope: users + user_settings only
-- Note: this file does not include production data.

-- ========================================
-- 1. Seed staff accounts from V16_1
-- ========================================
INSERT INTO users (
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
    gen_random_uuid(),
    'DE170706',
    'phucnhde170706@fpt.edu.vn',
    'Nguyễn Hoàng Phúc',
    'LIBRARIAN'::user_role,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (user_code) DO UPDATE
SET
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    is_active = EXCLUDED.is_active,
    updated_at = NOW();

INSERT INTO users (
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
    gen_random_uuid(),
    'DE180893',
    'uyenlpde180893@fpt.edu.vn',
    'Lê Phương Uyên',
    'ADMIN'::user_role,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (user_code) DO UPDATE
SET
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    is_active = EXCLUDED.is_active,
    updated_at = NOW();

-- ========================================
-- 2. Seed sample login accounts from V28
-- Comment in migration says:
-- - admin / admin123
-- - librarian / librarian123
-- ========================================
INSERT INTO users (
    id,
    user_code,
    email,
    full_name,
    role,
    is_active,
    username,
    password,
    created_at,
    updated_at
)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'ADMIN001',
    'admin@fpt.edu.vn',
    'Admin User',
    'ADMIN'::user_role,
    TRUE,
    'admin',
    '$2a$10$xGJ9Q7K5X8M5Z9Y3W7K8O9P0Q1R2S3T4U5V6W7X8Y9Z0A1B2C3D4E5F',
    NOW(),
    NOW()
)
ON CONFLICT (user_code) DO UPDATE
SET
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    is_active = EXCLUDED.is_active,
    username = EXCLUDED.username,
    password = EXCLUDED.password,
    updated_at = NOW();

INSERT INTO users (
    id,
    user_code,
    email,
    full_name,
    role,
    is_active,
    username,
    password,
    created_at,
    updated_at
)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'LIB001',
    'librarian@fpt.edu.vn',
    'Thủ thư',
    'LIBRARIAN'::user_role,
    TRUE,
    'librarian',
    '$2a$10$ABCD9Q7K5X8M5Z9Y3W7K8O9P0Q1R2S3T4U5V6W7X8Y9Z0A1B2C',
    NOW(),
    NOW()
)
ON CONFLICT (user_code) DO UPDATE
SET
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    is_active = EXCLUDED.is_active,
    username = EXCLUDED.username,
    password = EXCLUDED.password,
    updated_at = NOW();

-- ========================================
-- 3. Backfill user_settings for seeded users
-- ========================================
INSERT INTO user_settings (
    user_id,
    language_code,
    theme_mode,
    is_booking_remind_enabled,
    is_ai_recommend_enabled,
    is_hce_enabled,
    created_at,
    updated_at
)
SELECT
    u.id,
    'vi',
    'light',
    TRUE,
    TRUE,
    TRUE,
    NOW(),
    NOW()
FROM users u
WHERE u.user_code IN ('DE170706', 'DE180893', 'ADMIN001', 'LIB001')
ON CONFLICT (user_id) DO NOTHING;
