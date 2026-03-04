-- =================================================================
-- Script tạo dữ liệu demo cho Violation Management
-- Sử dụng dữ liệu user có sẵn trong database
-- 
-- YÊU CẦU:
-- - Phải có ít nhất 5 users với role='STUDENT' trong database
-- - Phải có ít nhất 1 user với role='LIBRARIAN' trong database
--
-- Script sẽ:
-- 1. Lấy 5 sinh viên đầu tiên từ database
-- 2. Tạo student_profiles nếu chưa có
-- 3. Tạo 12 violations cho 5 sinh viên này
-- 4. Tạo 2 appeals ở trạng thái PENDING
-- 5. Tự động tính lại reputation_score và violation_count
-- 
-- Chạy script này trong pgAdmin hoặc DBeaver
-- =================================================================

-- [OPTIONAL] Kiểm tra trước khi chạy:
-- SELECT role, COUNT(*) FROM users GROUP BY role;
-- Cần có: STUDENT >= 5, LIBRARIAN >= 1

DO $$
DECLARE
    librarian_id UUID;
    student1_id UUID;
    student2_id UUID;
    student3_id UUID;
    student4_id UUID;
    student5_id UUID;
    student_ids UUID[];
BEGIN
    -- Lấy 5 sinh viên từ database có sẵn
    SELECT ARRAY_AGG(id) INTO student_ids
    FROM (
        SELECT id FROM users 
        WHERE role = 'STUDENT' AND is_active = true
        LIMIT 5
    ) students;
    
    -- Kiểm tra nếu không đủ 5 sinh viên
    IF array_length(student_ids, 1) < 5 THEN
        RAISE EXCEPTION 'Không tìm thấy đủ 5 sinh viên trong database. Hiện có: %', array_length(student_ids, 1);
    END IF;
    
    -- Gán ID cho từng biến
    student1_id := student_ids[1];
    student2_id := student_ids[2];
    student3_id := student_ids[3];
    student4_id := student_ids[4];
    student5_id := student_ids[5];
    
    -- Tạo student profiles nếu chưa có
    INSERT INTO student_profiles (user_id, reputation_score, total_study_hours, violation_count, created_at, updated_at)
    VALUES 
        (student1_id, 100, 0, 0, NOW(), NOW()),
        (student2_id, 100, 0, 0, NOW(), NOW()),
        (student3_id, 100, 0, 0, NOW(), NOW()),
        (student4_id, 100, 0, 0, NOW(), NOW()),
        (student5_id, 100, 0, 0, NOW(), NOW())
    ON CONFLICT (user_id) DO NOTHING;
    
    -- Lấy ID của librarian đầu tiên
    SELECT id INTO librarian_id FROM users WHERE role = 'LIBRARIAN' LIMIT 1;
    
    IF librarian_id IS NULL THEN
        RAISE EXCEPTION 'Không tìm thấy librarian trong database. Vui lòng tạo ít nhất 1 user có role LIBRARIAN';
    END IF;
    
    -- In ra thông tin sinh viên được chọn
    RAISE NOTICE 'Đang tạo violations cho 5 sinh viên:';
    RAISE NOTICE 'Student 1: %', (SELECT user_code || ' - ' || full_name FROM users WHERE id = student1_id);
    RAISE NOTICE 'Student 2: %', (SELECT user_code || ' - ' || full_name FROM users WHERE id = student2_id);
    RAISE NOTICE 'Student 3: %', (SELECT user_code || ' - ' || full_name FROM users WHERE id = student3_id);
    RAISE NOTICE 'Student 4: %', (SELECT user_code || ' - ' || full_name FROM users WHERE id = student4_id);
    RAISE NOTICE 'Student 5: %', (SELECT user_code || ' - ' || full_name FROM users WHERE id = student5_id);

    -- Tạo violations cho Student 1 (3 vi phạm)
    INSERT INTO violation_records (id, student_id, created_by, violation_reason, penalty_points, status, created_at, updated_at)
    VALUES 
        (gen_random_uuid(), student1_id, librarian_id, 'Làm ồn trong thư viện', 10, 'APPEALED', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
        (gen_random_uuid(), student1_id, librarian_id, 'Trả sách muộn 3 ngày', 5, 'ACTIVE', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
        (gen_random_uuid(), student1_id, librarian_id, 'Không trả thẻ khi ra về', 3, 'ACTIVE', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

    -- Tạo violations cho Student 2 (2 vi phạm)
    INSERT INTO violation_records (id, student_id, created_by, violation_reason, penalty_points, status, created_at, updated_at)
    VALUES 
        (gen_random_uuid(), student2_id, librarian_id, 'Sử dụng điện thoại trong khu vực yên lặng', 5, 'DISMISSED', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
        (gen_random_uuid(), student2_id, librarian_id, 'Mang đồ ăn vào thư viện', 10, 'ACTIVE', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days');

    -- Tạo violations cho Student 3 (4 vi phạm)
    INSERT INTO violation_records (id, student_id, created_by, violation_reason, penalty_points, status, created_at, updated_at)
    VALUES 
        (gen_random_uuid(), student3_id, librarian_id, 'Viết, vẽ lên sách', 15, 'APPEALED', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
        (gen_random_uuid(), student3_id, librarian_id, 'Không đặt chỗ ngồi đúng quy định', 3, 'ACTIVE', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
        (gen_random_uuid(), student3_id, librarian_id, 'Để rác trong thư viện', 5, 'ACTIVE', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
        (gen_random_uuid(), student3_id, librarian_id, 'Trả sách muộn 1 tuần', 10, 'ACTIVE', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days');

    -- Tạo violations cho Student 4 (2 vi phạm)
    INSERT INTO violation_records (id, student_id, created_by, violation_reason, penalty_points, status, created_at, updated_at)
    VALUES 
        (gen_random_uuid(), student4_id, librarian_id, 'Làm mất sách mượn', 20, 'CANCELLED', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days'),
        (gen_random_uuid(), student4_id, librarian_id, 'Ngủ trong thư viện', 5, 'ACTIVE', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

    -- Tạo violations cho Student 5 (1 vi phạm)
    INSERT INTO violation_records (id, student_id, created_by, violation_reason, penalty_points, status, created_at, updated_at)
    VALUES 
        (gen_random_uuid(), student5_id, librarian_id, 'Trả sách muộn 1 ngày', 3, 'ACTIVE', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

    -- Tạo appeals cho một số vi phạm có status APPEALED
    INSERT INTO violation_appeals (id, violation_id, student_id, appeal_reason, status, created_at, updated_at)
    SELECT 
        gen_random_uuid(),
        vr.id,
        vr.student_id,
        'Xin được xem xét lại vì có lý do chính đáng',
        'PENDING',
        NOW() - INTERVAL '1 day',
        NOW() - INTERVAL '1 day'
    FROM violation_records vr
    WHERE vr.status = 'APPEALED'
    LIMIT 2;

    -- Cập nhật lại violation_count cho student_profiles
    UPDATE student_profiles sp
    SET violation_count = (
        SELECT COUNT(*) 
        FROM violation_records vr 
        WHERE vr.student_id = sp.user_id 
        AND vr.status IN ('ACTIVE', 'APPEALED', 'DISMISSED')
    );

    -- Cập nhật lại reputation_score (trừ điểm vi phạm)
    UPDATE student_profiles sp
    SET reputation_score = GREATEST(0, 100 - COALESCE((
        SELECT SUM(penalty_points) 
        FROM violation_records vr 
        WHERE vr.student_id = sp.user_id 
        AND vr.status IN ('ACTIVE', 'APPEALED', 'DISMISSED')
    ), 0));

END $$;

-- Kiểm tra kết quả - Hiển thị 5 sinh viên vừa được tạo violations
SELECT 
    u.user_code,
    u.full_name,
    sp.reputation_score,
    sp.violation_count,
    COUNT(vr.id) as total_violations
FROM users u
JOIN student_profiles sp ON u.id = sp.user_id
LEFT JOIN violation_records vr ON u.id = vr.student_id
WHERE u.role = 'STUDENT'
GROUP BY u.id, u.user_code, u.full_name, sp.reputation_score, sp.violation_count
HAVING COUNT(vr.id) > 0
ORDER BY COUNT(vr.id) DESC, u.user_code;
