-- 1. Chuyển giá trị mặc định về null tạm thời để tránh lỗi ép kiểu
ALTER TABLE "public"."users" ALTER COLUMN "role" DROP DEFAULT;

-- 2. Cập nhật dữ liệu cũ từ thường sang HOA (ép kiểu qua text trước)
UPDATE "public"."users" 
SET "role" = 'STUDENT' 
WHERE "role"::text = 'student';

-- 3. Ép kiểu lại cột role sang enum user_role một cách tường minh
ALTER TABLE "public"."users" 
ALTER COLUMN "role" TYPE public.user_role 
USING "role"::text::public.user_role;

-- 4. Đặt lại giá trị mặc định chuẩn HOA
ALTER TABLE "public"."users" 
ALTER COLUMN "role" SET DEFAULT 'STUDENT'::public.user_role;