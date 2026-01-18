-- -- ============================================================
-- -- SLIB - Smart Library Ecosystem
-- -- Migration: Enhance Library Schema for Better Seat Management
-- -- Date: 2026-01-16
-- -- Description: 
-- --   - Add zone types and configurations
-- --   - Add seat amenities
-- --   - Add library configuration table
-- --   - Add violation types table
-- --   - Improve area settings
-- -- ============================================================

-- -- ============================================================
-- -- 1. ENUM TYPES
-- -- ============================================================

-- -- Zone Type Enum
-- DO $$ 
-- BEGIN
--     IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'zone_type') THEN
--         CREATE TYPE "public"."zone_type" AS ENUM (
--             'SILENT',       -- Khu Yên Tĩnh
--             'DISCUSSION',   -- Khu Thảo Luận
--             'COMPUTER',     -- Khu Máy Tính
--             'SELF_STUDY',   -- Khu Tự Học
--             'GENERAL'       -- Khu Chung
--         );
--     END IF;
-- END $$;

-- -- Violation Type Enum
-- DO $$ 
-- BEGIN
--     IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'violation_type_enum') THEN
--         CREATE TYPE "public"."violation_type_enum" AS ENUM (
--             'NO_SHOW',
--             'LATE_CHECKIN',
--             'NOISE_VIOLATION',
--             'SLEEPING',
--             'FOOD_VIOLATION',
--             'DAMAGE_PROPERTY',
--             'OTHER'
--         );
--     END IF;
-- END $$;

-- -- ============================================================
-- -- 2. ALTER TABLE: areas
-- -- ============================================================

-- -- Add new columns to areas table
-- ALTER TABLE "public"."areas" 
-- ADD COLUMN IF NOT EXISTS "description" TEXT,
-- ADD COLUMN IF NOT EXISTS "open_time" TIME DEFAULT '07:00',
-- ADD COLUMN IF NOT EXISTS "close_time" TIME DEFAULT '22:00',
-- ADD COLUMN IF NOT EXISTS "grace_period" INTEGER DEFAULT 15,
-- ADD COLUMN IF NOT EXISTS "max_booking_duration" INTEGER DEFAULT 180,
-- ADD COLUMN IF NOT EXISTS "created_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
-- ADD COLUMN IF NOT EXISTS "updated_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- -- Add comment
-- COMMENT ON COLUMN "public"."areas"."description" IS 'Mô tả phòng thư viện';
-- COMMENT ON COLUMN "public"."areas"."open_time" IS 'Giờ mở cửa';
-- COMMENT ON COLUMN "public"."areas"."close_time" IS 'Giờ đóng cửa';
-- COMMENT ON COLUMN "public"."areas"."grace_period" IS 'Thời gian ân hạn check-in (phút)';
-- COMMENT ON COLUMN "public"."areas"."max_booking_duration" IS 'Thời gian booking tối đa (phút)';

-- -- ============================================================
-- -- 3. ALTER TABLE: zones
-- -- ============================================================

-- -- Add new columns to zones table
-- ALTER TABLE "public"."zones"
-- ADD COLUMN IF NOT EXISTS "zone_type" VARCHAR(50) DEFAULT 'GENERAL',
-- ADD COLUMN IF NOT EXISTS "max_capacity" INTEGER DEFAULT 50,
-- ADD COLUMN IF NOT EXISTS "open_time" TIME,
-- ADD COLUMN IF NOT EXISTS "close_time" TIME,
-- ADD COLUMN IF NOT EXISTS "max_booking_duration" INTEGER,
-- ADD COLUMN IF NOT EXISTS "is_active" BOOLEAN DEFAULT TRUE,
-- ADD COLUMN IF NOT EXISTS "rules" TEXT,
-- ADD COLUMN IF NOT EXISTS "created_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
-- ADD COLUMN IF NOT EXISTS "updated_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- -- Add comments
-- COMMENT ON COLUMN "public"."zones"."zone_type" IS 'Loại khu vực: SILENT, DISCUSSION, COMPUTER, SELF_STUDY, GENERAL';
-- COMMENT ON COLUMN "public"."zones"."max_capacity" IS 'Sức chứa tối đa';
-- COMMENT ON COLUMN "public"."zones"."open_time" IS 'Giờ mở cửa riêng (NULL = theo area)';
-- COMMENT ON COLUMN "public"."zones"."close_time" IS 'Giờ đóng cửa riêng (NULL = theo area)';
-- COMMENT ON COLUMN "public"."zones"."max_booking_duration" IS 'Thời gian booking tối đa riêng (NULL = theo area)';
-- COMMENT ON COLUMN "public"."zones"."is_active" IS 'Khu vực có đang hoạt động không';
-- COMMENT ON COLUMN "public"."zones"."rules" IS 'Quy định riêng của khu vực';

-- -- ============================================================
-- -- 4. ALTER TABLE: seats
-- -- ============================================================

-- -- Add new columns to seats table
-- ALTER TABLE "public"."seats"
-- ADD COLUMN IF NOT EXISTS "has_power_outlet" BOOLEAN DEFAULT FALSE,
-- ADD COLUMN IF NOT EXISTS "has_pc" BOOLEAN DEFAULT FALSE,
-- ADD COLUMN IF NOT EXISTS "has_lamp" BOOLEAN DEFAULT FALSE,
-- ADD COLUMN IF NOT EXISTS "near_window" BOOLEAN DEFAULT FALSE,
-- ADD COLUMN IF NOT EXISTS "is_active" BOOLEAN DEFAULT TRUE,
-- ADD COLUMN IF NOT EXISTS "notes" VARCHAR(255),
-- ADD COLUMN IF NOT EXISTS "created_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
-- ADD COLUMN IF NOT EXISTS "updated_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- -- Add comments
-- COMMENT ON COLUMN "public"."seats"."has_power_outlet" IS 'Có ổ cắm điện';
-- COMMENT ON COLUMN "public"."seats"."has_pc" IS 'Có máy tính';
-- COMMENT ON COLUMN "public"."seats"."has_lamp" IS 'Có đèn bàn';
-- COMMENT ON COLUMN "public"."seats"."near_window" IS 'Gần cửa sổ';
-- COMMENT ON COLUMN "public"."seats"."is_active" IS 'Ghế có đang hoạt động (FALSE = bảo trì)';
-- COMMENT ON COLUMN "public"."seats"."notes" IS 'Ghi chú về ghế';

-- -- ============================================================
-- -- 5. CREATE TABLE: library_config
-- -- ============================================================

-- CREATE TABLE IF NOT EXISTS "public"."library_config" (
--     "config_id" SERIAL PRIMARY KEY,
--     "config_key" VARCHAR(100) UNIQUE NOT NULL,
--     "config_value" TEXT NOT NULL,
--     "config_type" VARCHAR(20) DEFAULT 'string', -- string, number, boolean, time, json
--     "description" VARCHAR(255),
--     "is_editable" BOOLEAN DEFAULT TRUE,
--     "created_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
--     "updated_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
--     "updated_by" UUID REFERENCES "public"."users"("id")
-- );

-- -- Add comments
-- COMMENT ON TABLE "public"."library_config" IS 'Bảng cấu hình hệ thống thư viện';
-- COMMENT ON COLUMN "public"."library_config"."config_key" IS 'Khóa cấu hình (unique)';
-- COMMENT ON COLUMN "public"."library_config"."config_value" IS 'Giá trị cấu hình';
-- COMMENT ON COLUMN "public"."library_config"."config_type" IS 'Loại dữ liệu: string, number, boolean, time, json';

-- -- Insert default configurations
-- INSERT INTO "public"."library_config" ("config_key", "config_value", "config_type", "description") VALUES
-- -- Thời gian
-- ('library_open_time', '07:00', 'time', 'Giờ mở cửa thư viện'),
-- ('library_close_time', '22:00', 'time', 'Giờ đóng cửa thư viện'),
-- ('default_booking_duration', '120', 'number', 'Thời gian booking mặc định (phút)'),
-- ('max_booking_duration', '240', 'number', 'Thời gian booking tối đa (phút)'),
-- ('min_booking_duration', '30', 'number', 'Thời gian booking tối thiểu (phút)'),
-- ('check_in_grace_period', '15', 'number', 'Thời gian ân hạn check-in (phút)'),
-- ('advance_booking_days', '7', 'number', 'Số ngày có thể đặt trước'),

-- -- Điểm uy tín
-- ('no_show_penalty', '-10', 'number', 'Điểm trừ khi no-show'),
-- ('late_checkin_penalty', '-5', 'number', 'Điểm trừ khi check-in muộn'),
-- ('noise_violation_penalty', '-5', 'number', 'Điểm trừ khi vi phạm yên tĩnh'),
-- ('sleeping_penalty', '-3', 'number', 'Điểm trừ khi ngủ'),
-- ('food_violation_penalty', '-3', 'number', 'Điểm trừ khi ăn uống sai quy định'),
-- ('damage_property_penalty', '-15', 'number', 'Điểm trừ khi làm hỏng tài sản'),
-- ('early_checkout_reward', '2', 'number', 'Điểm thưởng khi checkout sớm'),
-- ('on_time_checkin_reward', '1', 'number', 'Điểm thưởng khi check-in đúng giờ'),
-- ('consecutive_days_reward', '5', 'number', 'Điểm thưởng khi sử dụng liên tục 7 ngày'),

-- -- Giới hạn
-- ('max_active_reservations', '2', 'number', 'Số lượng booking đang hoạt động tối đa'),
-- ('min_reputation_to_book', '30', 'number', 'Điểm uy tín tối thiểu để đặt chỗ'),
-- ('low_reputation_threshold', '50', 'number', 'Ngưỡng điểm uy tín thấp (cảnh báo)'),

-- -- Thông báo
-- ('reminder_before_booking', '30', 'number', 'Nhắc trước khi booking bắt đầu (phút)'),
-- ('reminder_before_end', '15', 'number', 'Nhắc trước khi hết giờ (phút)')

-- ON CONFLICT ("config_key") DO NOTHING;

-- -- ============================================================
-- -- 6. CREATE TABLE: violation_types
-- -- ============================================================

-- CREATE TABLE IF NOT EXISTS "public"."violation_types" (
--     "violation_id" SERIAL PRIMARY KEY,
--     "violation_code" VARCHAR(50) UNIQUE NOT NULL,
--     "violation_name" VARCHAR(100) NOT NULL,
--     "description" TEXT,
--     "penalty_points" INTEGER NOT NULL,
--     "is_active" BOOLEAN DEFAULT TRUE,
--     "created_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW()
-- );

-- -- Add comments
-- COMMENT ON TABLE "public"."violation_types" IS 'Danh sách các loại vi phạm và điểm phạt';

-- -- Insert default violation types
-- INSERT INTO "public"."violation_types" ("violation_code", "violation_name", "description", "penalty_points") VALUES
-- ('NO_SHOW', 'Không đến', 'Đặt chỗ nhưng không đến sau thời gian ân hạn', -10),
-- ('LATE_CHECKIN', 'Check-in muộn', 'Check-in sau thời gian ân hạn nhưng vẫn đến', -5),
-- ('NOISE_VIOLATION', 'Vi phạm yên tĩnh', 'Gây ồn ào tại khu vực yên tĩnh', -5),
-- ('SLEEPING', 'Ngủ tại thư viện', 'Ngủ tại ghế học', -3),
-- ('FOOD_VIOLATION', 'Ăn uống sai quy định', 'Ăn uống tại khu vực không được phép', -3),
-- ('DAMAGE_PROPERTY', 'Làm hỏng tài sản', 'Làm hỏng ghế, bàn, thiết bị thư viện', -15),
-- ('EARLY_LEAVE', 'Rời đi sớm không checkout', 'Rời đi trước thời gian mà không checkout', -2),
-- ('MULTIPLE_BOOKING', 'Đặt nhiều chỗ cùng lúc', 'Cố tình đặt nhiều chỗ cùng thời điểm', -8)
-- ON CONFLICT ("violation_code") DO NOTHING;

-- -- ============================================================
-- -- 7. CREATE TABLE: user_violations
-- -- ============================================================

-- CREATE TABLE IF NOT EXISTS "public"."user_violations" (
--     "id" UUID DEFAULT gen_random_uuid() PRIMARY KEY,
--     "user_id" UUID NOT NULL REFERENCES "public"."users"("id"),
--     "violation_id" INTEGER NOT NULL REFERENCES "public"."violation_types"("violation_id"),
--     "reservation_id" UUID REFERENCES "public"."reservations"("reservation_id"),
--     "reported_by" UUID REFERENCES "public"."users"("id"),
--     "notes" TEXT,
--     "penalty_applied" INTEGER NOT NULL,
--     "created_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW()
-- );

-- -- Add index
-- CREATE INDEX IF NOT EXISTS "idx_user_violations_user_id" ON "public"."user_violations"("user_id");
-- CREATE INDEX IF NOT EXISTS "idx_user_violations_created_at" ON "public"."user_violations"("created_at" DESC);

-- -- Add comments
-- COMMENT ON TABLE "public"."user_violations" IS 'Lịch sử vi phạm của người dùng';

-- -- ============================================================
-- -- 8. CREATE TABLE: nfc_devices (Thiết bị NFC)
-- -- ============================================================

-- CREATE TABLE IF NOT EXISTS "public"."nfc_devices" (
--     "device_id" UUID DEFAULT gen_random_uuid() PRIMARY KEY,
--     "device_code" VARCHAR(50) UNIQUE NOT NULL,
--     "device_name" VARCHAR(100) NOT NULL,
--     "device_type" VARCHAR(50) DEFAULT 'READER', -- READER, GATE, KIOSK
--     "location_type" VARCHAR(50), -- ENTRANCE, EXIT, SEAT, ZONE
--     "area_id" BIGINT REFERENCES "public"."areas"("area_id"),
--     "zone_id" INTEGER REFERENCES "public"."zones"("zone_id"),
--     "seat_id" INTEGER REFERENCES "public"."seats"("seat_id"),
--     "is_active" BOOLEAN DEFAULT TRUE,
--     "last_ping" TIMESTAMP WITH TIME ZONE,
--     "ip_address" VARCHAR(50),
--     "firmware_version" VARCHAR(20),
--     "notes" TEXT,
--     "created_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
--     "updated_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW()
-- );

-- -- Add comments
-- COMMENT ON TABLE "public"."nfc_devices" IS 'Danh sách thiết bị NFC/HCE';
-- COMMENT ON COLUMN "public"."nfc_devices"."device_type" IS 'Loại thiết bị: READER (đầu đọc), GATE (cổng), KIOSK (máy tự phục vụ)';
-- COMMENT ON COLUMN "public"."nfc_devices"."location_type" IS 'Vị trí: ENTRANCE (cổng vào), EXIT (cổng ra), SEAT (tại ghế), ZONE (tại khu vực)';

-- -- ============================================================
-- -- 9. CREATE TABLE: ai_config (Cấu hình AI Gemini)
-- -- ============================================================

-- CREATE TABLE IF NOT EXISTS "public"."ai_config" (
--     "config_id" SERIAL PRIMARY KEY,
--     "api_key" TEXT, -- Encrypted
--     "model_name" VARCHAR(100) DEFAULT 'gemini-pro',
--     "temperature" DECIMAL(3,2) DEFAULT 0.7,
--     "max_tokens" INTEGER DEFAULT 1024,
--     "is_enabled" BOOLEAN DEFAULT TRUE,
--     "system_prompt" TEXT,
--     "created_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
--     "updated_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW()
-- );

-- -- Add comments
-- COMMENT ON TABLE "public"."ai_config" IS 'Cấu hình AI Chatbot (Gemini)';

-- -- Insert default AI config
-- INSERT INTO "public"."ai_config" ("model_name", "temperature", "max_tokens", "system_prompt") VALUES
-- ('gemini-pro', 0.7, 1024, 'Bạn là trợ lý AI của hệ thống thư viện thông minh SLIB. Hãy giúp sinh viên tìm chỗ ngồi phù hợp, trả lời câu hỏi về quy định thư viện, và hỗ trợ các vấn đề liên quan đến việc sử dụng thư viện. Trả lời bằng tiếng Việt, ngắn gọn và thân thiện.')
-- ON CONFLICT DO NOTHING;

-- -- ============================================================
-- -- 10. CREATE TABLE: ai_knowledge_base
-- -- ============================================================

-- CREATE TABLE IF NOT EXISTS "public"."ai_knowledge_base" (
--     "knowledge_id" SERIAL PRIMARY KEY,
--     "category" VARCHAR(100) NOT NULL,
--     "question" TEXT NOT NULL,
--     "answer" TEXT NOT NULL,
--     "keywords" TEXT[], -- Array of keywords for matching
--     "is_active" BOOLEAN DEFAULT TRUE,
--     "usage_count" INTEGER DEFAULT 0,
--     "created_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
--     "updated_at" TIMESTAMP WITH TIME ZONE DEFAULT NOW()
-- );

-- -- Add index for full-text search
-- CREATE INDEX IF NOT EXISTS "idx_ai_knowledge_question" ON "public"."ai_knowledge_base" USING gin(to_tsvector('vietnamese', "question"));
-- CREATE INDEX IF NOT EXISTS "idx_ai_knowledge_keywords" ON "public"."ai_knowledge_base" USING gin("keywords");

-- -- Add comments
-- COMMENT ON TABLE "public"."ai_knowledge_base" IS 'Cơ sở kiến thức cho AI chatbot';

-- -- Insert default knowledge
-- INSERT INTO "public"."ai_knowledge_base" ("category", "question", "answer", "keywords") VALUES
-- ('booking', 'Làm sao để đặt chỗ ngồi?', 'Để đặt chỗ ngồi, bạn vào mục "Đặt chỗ" trên app, chọn khu vực và ghế trống, sau đó chọn thời gian và xác nhận đặt chỗ.', ARRAY['đặt chỗ', 'booking', 'đặt ghế', 'chỗ ngồi']),
-- ('booking', 'Tôi có thể đặt chỗ trước bao nhiêu ngày?', 'Bạn có thể đặt chỗ trước tối đa 7 ngày.', ARRAY['đặt trước', 'trước bao nhiêu ngày', 'advance booking']),
-- ('rules', 'Quy định khu yên tĩnh là gì?', 'Khu yên tĩnh yêu cầu: Không nói chuyện, điện thoại để chế độ im lặng, không ăn uống, và giữ yên lặng tuyệt đối.', ARRAY['yên tĩnh', 'quy định', 'silent zone', 'rules']),
-- ('checkin', 'Làm sao để check-in?', 'Bạn có thể check-in bằng cách quét NFC tại đầu đọc hoặc quét mã QR trên app khi đến ghế đã đặt.', ARRAY['check-in', 'checkin', 'quét', 'nfc', 'qr']),
-- ('reputation', 'Điểm uy tín là gì?', 'Điểm uy tín phản ánh mức độ tuân thủ quy định của bạn. Điểm cao giúp bạn ưu tiên đặt chỗ, điểm thấp sẽ bị hạn chế một số quyền.', ARRAY['điểm uy tín', 'reputation', 'điểm', 'score']),
-- ('hours', 'Thư viện mở cửa lúc mấy giờ?', 'Thư viện mở cửa từ 7h00 sáng đến 22h00 tối hàng ngày, kể cả cuối tuần.', ARRAY['giờ mở cửa', 'mấy giờ', 'opening hours', 'thời gian'])
-- ON CONFLICT DO NOTHING;

-- -- ============================================================
-- -- 11. TRIGGERS for updated_at
-- -- ============================================================

-- -- Create trigger for library_config
-- CREATE OR REPLACE TRIGGER "update_library_config_updated_at"
--     BEFORE UPDATE ON "public"."library_config"
--     FOR EACH ROW
--     EXECUTE FUNCTION "public"."update_updated_at_column"();

-- -- Create trigger for zones
-- DROP TRIGGER IF EXISTS "update_zones_updated_at" ON "public"."zones";
-- CREATE TRIGGER "update_zones_updated_at"
--     BEFORE UPDATE ON "public"."zones"
--     FOR EACH ROW
--     EXECUTE FUNCTION "public"."update_updated_at_column"();

-- -- Create trigger for seats
-- DROP TRIGGER IF EXISTS "update_seats_updated_at" ON "public"."seats";
-- CREATE TRIGGER "update_seats_updated_at"
--     BEFORE UPDATE ON "public"."seats"
--     FOR EACH ROW
--     EXECUTE FUNCTION "public"."update_updated_at_column"();

-- -- Create trigger for areas
-- DROP TRIGGER IF EXISTS "update_areas_updated_at" ON "public"."areas";
-- CREATE TRIGGER "update_areas_updated_at"
--     BEFORE UPDATE ON "public"."areas"
--     FOR EACH ROW
--     EXECUTE FUNCTION "public"."update_updated_at_column"();

-- -- Create trigger for nfc_devices
-- CREATE OR REPLACE TRIGGER "update_nfc_devices_updated_at"
--     BEFORE UPDATE ON "public"."nfc_devices"
--     FOR EACH ROW
--     EXECUTE FUNCTION "public"."update_updated_at_column"();

-- -- Create trigger for ai_config
-- CREATE OR REPLACE TRIGGER "update_ai_config_updated_at"
--     BEFORE UPDATE ON "public"."ai_config"
--     FOR EACH ROW
--     EXECUTE FUNCTION "public"."update_updated_at_column"();

-- -- Create trigger for ai_knowledge_base
-- CREATE OR REPLACE TRIGGER "update_ai_knowledge_base_updated_at"
--     BEFORE UPDATE ON "public"."ai_knowledge_base"
--     FOR EACH ROW
--     EXECUTE FUNCTION "public"."update_updated_at_column"();

-- -- ============================================================
-- -- 12. ROW LEVEL SECURITY
-- -- ============================================================

-- -- Enable RLS on new tables
-- ALTER TABLE "public"."library_config" ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE "public"."violation_types" ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE "public"."user_violations" ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE "public"."nfc_devices" ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE "public"."ai_config" ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE "public"."ai_knowledge_base" ENABLE ROW LEVEL SECURITY;

-- -- Policies for library_config
-- CREATE POLICY "Public read library_config" ON "public"."library_config" 
--     FOR SELECT USING (true);

-- CREATE POLICY "Staff modify library_config" ON "public"."library_config" 
--     USING ("public"."is_staff"());

-- -- Policies for violation_types
-- CREATE POLICY "Public read violation_types" ON "public"."violation_types" 
--     FOR SELECT USING (true);

-- CREATE POLICY "Staff modify violation_types" ON "public"."violation_types" 
--     USING ("public"."is_staff"());

-- -- Policies for user_violations
-- CREATE POLICY "Staff view all violations" ON "public"."user_violations" 
--     FOR SELECT USING ("public"."is_staff"());

-- CREATE POLICY "User view own violations" ON "public"."user_violations" 
--     FOR SELECT USING ("user_id" = "public"."get_current_user_id"());

-- CREATE POLICY "Staff create violations" ON "public"."user_violations" 
--     FOR INSERT WITH CHECK ("public"."is_staff"());

-- -- Policies for nfc_devices
-- CREATE POLICY "Staff manage nfc_devices" ON "public"."nfc_devices" 
--     USING ("public"."is_staff"());

-- -- Policies for ai_config (Admin only)
-- CREATE POLICY "Admin manage ai_config" ON "public"."ai_config" 
--     USING (EXISTS (
--         SELECT 1 FROM "public"."users" 
--         WHERE "supabase_uid" = auth.uid() AND "role" = 'ADMIN'
--     ));

-- -- Policies for ai_knowledge_base
-- CREATE POLICY "Public read ai_knowledge" ON "public"."ai_knowledge_base" 
--     FOR SELECT USING (true);

-- CREATE POLICY "Staff modify ai_knowledge" ON "public"."ai_knowledge_base" 
--     USING ("public"."is_staff"());

-- -- ============================================================
-- -- 13. VIEWS for Statistics
-- -- ============================================================

-- -- View: Thống kê ghế theo khu vực
-- CREATE OR REPLACE VIEW "public"."v_zone_seat_stats" AS
-- SELECT 
--     z.zone_id,
--     z.zone_name,
--     z.zone_type,
--     z.area_id,
--     a.area_name,
--     COUNT(s.seat_id) as total_seats,
--     COUNT(CASE WHEN s.is_active = true THEN 1 END) as active_seats,
--     COUNT(CASE WHEN s.is_active = false THEN 1 END) as maintenance_seats,
--     COUNT(CASE WHEN s.has_power_outlet = true THEN 1 END) as seats_with_power,
--     COUNT(CASE WHEN s.has_pc = true THEN 1 END) as seats_with_pc,
--     COUNT(CASE WHEN s.has_lamp = true THEN 1 END) as seats_with_lamp
-- FROM "public"."zones" z
-- LEFT JOIN "public"."areas" a ON z.area_id = a.area_id
-- LEFT JOIN "public"."seats" s ON z.zone_id = s.zone_id
-- GROUP BY z.zone_id, z.zone_name, z.zone_type, z.area_id, a.area_name;

-- -- View: Thống kê phòng thư viện
-- CREATE OR REPLACE VIEW "public"."v_area_stats" AS
-- SELECT 
--     a.area_id,
--     a.area_name,
--     a.is_active,
--     COUNT(DISTINCT z.zone_id) as total_zones,
--     COUNT(DISTINCT s.seat_id) as total_seats,
--     COUNT(DISTINCT CASE WHEN s.is_active = true THEN s.seat_id END) as active_seats
-- FROM "public"."areas" a
-- LEFT JOIN "public"."zones" z ON a.area_id = z.area_id
-- LEFT JOIN "public"."seats" s ON z.zone_id = s.zone_id
-- GROUP BY a.area_id, a.area_name, a.is_active;

-- -- ============================================================
-- -- 14. GRANTS
-- -- ============================================================

-- GRANT SELECT ON "public"."library_config" TO "anon", "authenticated";
-- GRANT ALL ON "public"."library_config" TO "service_role";

-- GRANT SELECT ON "public"."violation_types" TO "anon", "authenticated";
-- GRANT ALL ON "public"."violation_types" TO "service_role";

-- GRANT SELECT ON "public"."user_violations" TO "authenticated";
-- GRANT ALL ON "public"."user_violations" TO "service_role";

-- GRANT ALL ON "public"."nfc_devices" TO "service_role";

-- GRANT ALL ON "public"."ai_config" TO "service_role";

-- GRANT SELECT ON "public"."ai_knowledge_base" TO "anon", "authenticated";
-- GRANT ALL ON "public"."ai_knowledge_base" TO "service_role";

-- GRANT SELECT ON "public"."v_zone_seat_stats" TO "anon", "authenticated";
-- GRANT SELECT ON "public"."v_area_stats" TO "anon", "authenticated";

-- -- Grant sequences
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA "public" TO "anon", "authenticated", "service_role";

-- -- ============================================================
-- -- MIGRATION COMPLETE
-- -- ============================================================

-- -- Log migration
-- DO $$
-- BEGIN
--     RAISE NOTICE 'Migration 20260116150000_enhance_library_schema completed successfully!';
--     RAISE NOTICE 'New tables: library_config, violation_types, user_violations, nfc_devices, ai_config, ai_knowledge_base';
--     RAISE NOTICE 'New views: v_zone_seat_stats, v_area_stats';
--     RAISE NOTICE 'Modified tables: areas, zones, seats';
-- END $$;
--CHƯA DÙNG