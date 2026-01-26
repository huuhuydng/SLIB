-- =============================================
-- V5: Create Student Profiles Table
-- Tách reputation_score ra bảng riêng cho sinh viên
-- =============================================

-- Bước 1: Xóa cột reputation_score ở bảng users cũ
ALTER TABLE users DROP COLUMN IF EXISTS reputation_score;

-- Bước 2: Tạo bảng mới student_profiles
CREATE TABLE student_profiles (
    user_id uuid not null primary key references users (id) on delete cascade,
    reputation_score integer default 100 not null,
    total_study_hours float default 0 not null,
    violation_count integer default 0 not null,
    created_at timestamp(6) default now(),
    updated_at timestamp(6) default now()
);

-- Bước 3: Gán quyền owner
ALTER TABLE student_profiles OWNER TO postgres;

-- Bước 4: Tạo Trigger để tự động cập nhật updated_at
CREATE TRIGGER update_student_profiles_updated_at
    BEFORE UPDATE
    ON student_profiles
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

-- =============================================
-- Row Level Security (RLS)
-- =============================================

-- Bật RLS
ALTER TABLE student_profiles ENABLE ROW LEVEL SECURITY;

-- 1. Staff (Thủ thư/Admin) được quyền xem tất cả hồ sơ sinh viên
CREATE POLICY "Staff view all profiles" ON student_profiles AS PERMISSIVE FOR
SELECT USING (is_staff ());

-- 2. Staff được quyền chỉnh sửa điểm số/vi phạm
CREATE POLICY "Staff modify profiles" ON student_profiles AS PERMISSIVE FOR ALL USING (is_staff ());

-- 3. Sinh viên xem được hồ sơ của chính mình
CREATE POLICY "User view own profile" ON student_profiles AS PERMISSIVE FOR
SELECT USING (
        user_id = get_current_user_id ()
    );

-- =============================================
-- Trigger tự động tạo profile khi thêm STUDENT mới
-- =============================================

-- 1. Tạo hàm xử lý (Function)
CREATE OR REPLACE FUNCTION public.handle_new_student_profile()
RETURNS TRIGGER AS $$
BEGIN
  -- Chỉ tạo profile nếu user có role là STUDENT
  IF NEW.role = 'STUDENT' THEN
    INSERT INTO public.student_profiles (user_id, reputation_score, total_study_hours, violation_count)
    VALUES (NEW.id, 100, 0, 0);
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. Gán Trigger vào bảng users
DROP TRIGGER IF EXISTS on_user_created_create_profile ON public.users;

CREATE TRIGGER on_user_created_create_profile
  AFTER INSERT ON public.users
  FOR EACH ROW
  EXECUTE PROCEDURE public.handle_new_student_profile();