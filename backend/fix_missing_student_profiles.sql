-- =====================================================
-- FIX MISSING STUDENT PROFILES
-- Tạo student_profiles cho tất cả STUDENT users chưa có profile
-- =====================================================

INSERT INTO student_profiles (user_id, reputation_score, total_study_hours, violation_count, created_at, updated_at)
SELECT 
    u.id,
    100,  -- Default reputation score
    0.0,  -- Default study hours
    0,    -- Default violation count
    NOW(),
    NOW()
FROM users u
WHERE u.role = 'STUDENT'
  AND NOT EXISTS (
    SELECT 1 FROM student_profiles sp WHERE sp.user_id = u.id
  );

-- Kiểm tra kết quả
SELECT 
    COUNT(*) as total_students,
    (SELECT COUNT(*) FROM student_profiles) as total_profiles,
    COUNT(*) - (SELECT COUNT(*) FROM student_profiles) as missing_profiles
FROM users 
WHERE role = 'STUDENT';
