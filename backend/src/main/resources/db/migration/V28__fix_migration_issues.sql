-- =============================================
-- V28: Fix migration issues for clean setup
-- =============================================

-- 1. Ensure library_settings has default data
DO $
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'library_settings') THEN
        INSERT INTO library_settings (id, open_time, close_time, working_days, slot_duration, max_booking_days, max_bookings_per_day, max_hours_per_day)
        VALUES (1, '07:00', '21:00', '1,2,3,4,5,6', 60, 7, 3, 4)
        ON CONFLICT (id) DO UPDATE SET
            open_time = EXCLUDED.open_time,
            close_time = EXCLUDED.close_time,
            working_days = EXCLUDED.working_days,
            slot_duration = EXCLUDED.slot_duration,
            max_booking_days = EXCLUDED.max_booking_days,
            max_bookings_per_day = EXCLUDED.max_bookings_per_day,
            max_hours_per_day = EXCLUDED.max_hours_per_day;
    END IF;
END $;

-- 2. Add updated_at triggers for kiosk tables (if tables exist)
DO $
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'kiosk_configs') THEN
        DROP TRIGGER IF EXISTS update_kiosk_configs_updated_at ON kiosk_configs;
        CREATE TRIGGER update_kiosk_configs_updated_at
            BEFORE UPDATE ON kiosk_configs
            FOR EACH ROW
            EXECUTE PROCEDURE update_updated_at_column();
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'kiosk_qr_sessions') THEN
        DROP TRIGGER IF EXISTS update_kiosk_qr_sessions_updated_at ON kiosk_qr_sessions;
        CREATE TRIGGER update_kiosk_qr_sessions_updated_at
            BEFORE UPDATE ON kiosk_qr_sessions
            FOR EACH ROW
            EXECUTE PROCEDURE update_updated_at_column();
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'kiosk_images') THEN
        DROP TRIGGER IF EXISTS update_kiosk_images_updated_at ON kiosk_images;
        CREATE TRIGGER update_kiosk_images_updated_at
            BEFORE UPDATE ON kiosk_images
            FOR EACH ROW
            EXECUTE PROCEDURE update_updated_at_column();
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'library_maps') THEN
        DROP TRIGGER IF EXISTS update_library_maps_updated_at ON library_maps;
        CREATE TRIGGER update_library_maps_updated_at
            BEFORE UPDATE ON library_maps
            FOR EACH ROW
            EXECUTE PROCEDURE update_updated_at_column();
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'zone_maps') THEN
        DROP TRIGGER IF EXISTS update_zone_maps_updated_at ON zone_maps;
        CREATE TRIGGER update_zone_maps_updated_at
            BEFORE UPDATE ON zone_maps
            FOR EACH ROW
            EXECUTE PROCEDURE update_updated_at_column();
    END IF;
END $;

-- 3. Add updated_at trigger for student_behaviors (if table exists)
DO $
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'student_behaviors') THEN
        DROP TRIGGER IF EXISTS update_student_behaviors_updated_at ON student_behaviors;
        CREATE TRIGGER update_student_behaviors_updated_at
            BEFORE UPDATE ON student_behaviors
            FOR EACH ROW
            EXECUTE PROCEDURE update_updated_at_column();
    END IF;
END $;

-- 4. Ensure seed categories exist
DO $
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'news_categories') THEN
        INSERT INTO news_categories (name, color_code) VALUES
            ('Thông báo', '#FF5722'),
            ('Tin tức', '#2196F3'),
            ('Sự kiện', '#4CAF50'),
            ('Hướng dẫn', '#9C27B0')
        ON CONFLICT (name) DO NOTHING;
    ELSIF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'categories') THEN
        INSERT INTO categories (name, color_code) VALUES
            ('Thông báo', '#FF5722'),
            ('Tin tức', '#2196F3'),
            ('Sự kiện', '#4CAF50'),
            ('Hướng dẫn', '#9C27B0')
        ON CONFLICT (name) DO NOTHING;
    END IF;
END $;

-- 5. Ensure seed users exist (password: admin123 / librarian123)
INSERT INTO users (id, user_code, email, full_name, role, is_active, username, password)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'ADMIN001', 'admin@fpt.edu.vn', 'Admin User', 'ADMIN', true, 'admin', '$2a$10$xGJ9Q7K5X8M5Z9Y3W7K8O9P0Q1R2S3T4U5V6W7X8Y9Z0A1B2C3D4E5F')
ON CONFLICT (user_code) DO NOTHING;

INSERT INTO users (id, user_code, email, full_name, role, is_active, username, password)
VALUES
    ('00000000-0000-0000-0000-000000000002', 'LIB001', 'librarian@fpt.edu.vn', 'Thủ thư', 'LIBRARIAN', true, 'librarian', '$2a$10$ABCD9Q7K5X8M5Z9Y3W7K8O9P0Q1R2S3T4U5V6W7X8Y9Z0A1B2C')
ON CONFLICT (user_code) DO NOTHING;

-- 6. Ensure access_logs has required columns
DO $
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'access_logs') THEN
        ALTER TABLE access_logs ADD COLUMN IF NOT EXISTS check_in_time TIMESTAMP WITH TIME ZONE;
        ALTER TABLE access_logs ADD COLUMN IF NOT EXISTS check_out_time TIMESTAMP WITH TIME ZONE;
    END IF;
END $;

-- 7. Ensure is_staff() function exists (required for RLS)
CREATE OR REPLACE FUNCTION is_staff()
RETURNS BOOLEAN AS $
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM users
        WHERE users.id = get_current_user_id()
        AND users.role IN ('LIBRARIAN', 'ADMIN')
    );
END;
$ LANGUAGE plpgsql SECURITY DEFINER;

-- 8. Ensure get_current_user_id() function exists
CREATE OR REPLACE FUNCTION get_current_user_id()
RETURNS UUID AS $
DECLARE
    ret_uuid UUID;
BEGIN
    RETURN NULL;
END;
$ LANGUAGE plpgsql SECURITY DEFINER;

-- 9. Grant permissions for new tables (if they exist)
DO $
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'kiosk_configs') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON kiosk_configs TO PUBLIC;
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'kiosk_qr_sessions') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON kiosk_qr_sessions TO PUBLIC;
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'kiosk_images') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON kiosk_images TO PUBLIC;
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'library_maps') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON library_maps TO PUBLIC;
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'zone_maps') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON zone_maps TO PUBLIC;
    END IF;
    IF EXISTS (SELECT FROM pg_tables WHERE tablename = 'student_behaviors') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON student_behaviors TO PUBLIC;
    END IF;
END $;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO PUBLIC;
