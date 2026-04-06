-- V57: Backfill missing user_settings rows and align DB defaults with backend behavior

ALTER TABLE user_settings
    ALTER COLUMN language_code SET DEFAULT 'vi',
    ALTER COLUMN theme_mode SET DEFAULT 'light',
    ALTER COLUMN is_booking_remind_enabled SET DEFAULT TRUE,
    ALTER COLUMN is_ai_recommend_enabled SET DEFAULT TRUE,
    ALTER COLUMN is_hce_enabled SET DEFAULT TRUE,
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN updated_at SET DEFAULT NOW();

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
LEFT JOIN user_settings us ON us.user_id = u.id
WHERE us.user_id IS NULL;

UPDATE user_settings
SET
    language_code = COALESCE(language_code, 'vi'),
    theme_mode = COALESCE(theme_mode, 'light'),
    is_booking_remind_enabled = COALESCE(is_booking_remind_enabled, TRUE),
    is_ai_recommend_enabled = COALESCE(is_ai_recommend_enabled, TRUE),
    is_hce_enabled = COALESCE(is_hce_enabled, TRUE),
    created_at = COALESCE(created_at, NOW()),
    updated_at = NOW()
WHERE language_code IS NULL
   OR theme_mode IS NULL
   OR is_booking_remind_enabled IS NULL
   OR is_ai_recommend_enabled IS NULL
   OR is_hce_enabled IS NULL
   OR created_at IS NULL
   OR updated_at IS NULL;

CREATE OR REPLACE FUNCTION create_user_settings_automatically()
RETURNS TRIGGER AS $$
BEGIN
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
    VALUES (
        NEW.id,
        'vi',
        'light',
        TRUE,
        TRUE,
        TRUE,
        NOW(),
        NOW()
    )
    ON CONFLICT (user_id) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
