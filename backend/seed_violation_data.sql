-- =====================================================
-- SEED VIOLATION DATA - Run this directly in database
-- =====================================================

-- Step 1: Insert 5 students (if not exist)
INSERT INTO users (id, student_code, email, full_name, role, is_active, created_at, updated_at)
VALUES 
    ('11111111-1111-1111-1111-111111111111'::uuid, 'DE170707', 'antv@fpt.edu.vn', 'Trần Văn An', 'STUDENT', true, NOW(), NOW()),
    ('22222222-2222-2222-2222-222222222222'::uuid, 'DE170708', 'binhlt@fpt.edu.vn', 'Lê Thị Bình', 'STUDENT', true, NOW(), NOW()),
    ('33333333-3333-3333-3333-333333333333'::uuid, 'DE170709', 'cuongpm@fpt.edu.vn', 'Phạm Minh Cường', 'STUDENT', true, NOW(), NOW()),
    ('44444444-4444-4444-4444-444444444444'::uuid, 'DE170710', 'dangdh@fpt.edu.vn', 'Đỗ Hải Đăng', 'STUDENT', true, NOW(), NOW()),
    ('55555555-5555-5555-5555-555555555555'::uuid, 'DE170711', 'linhht@fpt.edu.vn', 'Hoàng Thùy Linh', 'STUDENT', true, NOW(), NOW())
ON CONFLICT (student_code) DO NOTHING;

-- Step 2: Create student profiles với reputation score
INSERT INTO student_profiles (user_id, reputation_score, total_study_hours, violation_count, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111111'::uuid, 95, 0, 3, NOW(), NOW()),
    ('22222222-2222-2222-2222-222222222222'::uuid, 75, 0, 2, NOW(), NOW()),
    ('33333333-3333-3333-3333-333333333333'::uuid, 70, 0, 4, NOW(), NOW()),
    ('44444444-4444-4444-4444-444444444444'::uuid, 80, 0, 2, NOW(), NOW()),
    ('55555555-5555-5555-5555-555555555555'::uuid, 90, 0, 3, NOW(), NOW())
ON CONFLICT (user_id) 
DO UPDATE SET 
    reputation_score = EXCLUDED.reputation_score,
    violation_count = EXCLUDED.violation_count;

-- Step 3: Get librarian ID
DO $$
DECLARE
    librarian_uuid UUID;
    student1_uuid UUID := '11111111-1111-1111-1111-111111111111'::uuid;
    student2_uuid UUID := '22222222-2222-2222-2222-222222222222'::uuid;
    student3_uuid UUID := '33333333-3333-3333-3333-333333333333'::uuid;
    student4_uuid UUID := '44444444-4444-4444-4444-444444444444'::uuid;
    student5_uuid UUID := '55555555-5555-5555-5555-555555555555'::uuid;
BEGIN
    -- Get librarian (DE170706)
    SELECT id INTO librarian_uuid FROM users WHERE student_code = 'DE170706' LIMIT 1;
    
    IF librarian_uuid IS NULL THEN
        RAISE NOTICE 'Librarian not found, using NULL';
    END IF;

    -- Delete old violations for these students
    DELETE FROM violation_appeals WHERE student_id IN (student1_uuid, student2_uuid, student3_uuid, student4_uuid, student5_uuid);
    DELETE FROM violation_records WHERE student_id IN (student1_uuid, student2_uuid, student3_uuid, student4_uuid, student5_uuid);

    -- Insert violations for Student 1: Trần Văn An
    INSERT INTO violation_records (id, student_id, created_by, rule_id, violation_reason, penalty_points, status, created_at)
    VALUES 
        (gen_random_uuid(), student1_uuid, librarian_uuid, 3, 'Gây mất trật tự trong thư viện', 15, 'APPEALED', NOW() - INTERVAL '5 days'),
        (gen_random_uuid(), student1_uuid, librarian_uuid, 4, 'Sử dụng thức ăn trong thư viện', 10, 'APPEALED', NOW() - INTERVAL '3 days'),
        (gen_random_uuid(), student1_uuid, librarian_uuid, 2, 'Trả chỗ muộn 30 phút', 5, 'ACTIVE', NOW() - INTERVAL '1 day');

    -- Insert violations for Student 2: Lê Thị Bình
    INSERT INTO violation_records (id, student_id, created_by, rule_id, violation_reason, penalty_points, status, created_at)
    VALUES 
        (gen_random_uuid(), student2_uuid, librarian_uuid, 1, 'Không đến sau khi đặt chỗ', 10, 'APPEALED', NOW() - INTERVAL '7 days'),
        (gen_random_uuid(), student2_uuid, librarian_uuid, 3, 'Nói chuyện lớn tiếng', 15, 'ACTIVE', NOW() - INTERVAL '2 days');

    -- Insert violations for Student 3: Phạm Minh Cường
    INSERT INTO violation_records (id, student_id, created_by, rule_id, violation_reason, penalty_points, status, created_at)
    VALUES 
        (gen_random_uuid(), student3_uuid, librarian_uuid, 3, 'Gây ồn ào', 15, 'DISMISSED', NOW() - INTERVAL '10 days'),
        (gen_random_uuid(), student3_uuid, librarian_uuid, 2, 'Trả chỗ muộn', 5, 'ACTIVE', NOW() - INTERVAL '6 days'),
        (gen_random_uuid(), student3_uuid, librarian_uuid, 4, 'Ngồi sai chỗ', 10, 'ACTIVE', NOW() - INTERVAL '4 days'),
        (gen_random_uuid(), student3_uuid, librarian_uuid, 1, 'No-show', 10, 'ACTIVE', NOW() - INTERVAL '1 day');

    -- Insert violations for Student 4: Đỗ Hải Đăng
    INSERT INTO violation_records (id, student_id, created_by, rule_id, violation_reason, penalty_points, status, created_at)
    VALUES 
        (gen_random_uuid(), student4_uuid, librarian_uuid, 2, 'Trả chỗ muộn 1 giờ', 5, 'ACTIVE', NOW() - INTERVAL '8 days'),
        (gen_random_uuid(), student4_uuid, librarian_uuid, 3, 'Gây mất trật tự', 15, 'ACTIVE', NOW() - INTERVAL '3 days');

    -- Insert violations for Student 5: Hoàng Thùy Linh
    INSERT INTO violation_records (id, student_id, created_by, rule_id, violation_reason, penalty_points, status, created_at)
    VALUES 
        (gen_random_uuid(), student5_uuid, librarian_uuid, 4, 'Sử dụng thức ăn', 10, 'APPEALED', NOW() - INTERVAL '9 days'),
        (gen_random_uuid(), student5_uuid, librarian_uuid, 3, 'Nói chuyện điện thoại', 15, 'CANCELLED', NOW() - INTERVAL '5 days'),
        (gen_random_uuid(), student5_uuid, librarian_uuid, 1, 'No-show', 10, 'ACTIVE', NOW() - INTERVAL '2 days');

    -- Insert appeals
    INSERT INTO violation_appeals (id, violation_id, student_id, appeal_reason, status, created_at)
    SELECT 
        gen_random_uuid(),
        vr.id,
        student1_uuid,
        'Tôi không gây ồn ào, chỉ thảo luận nhỏ với bạn về bài tập',
        'PENDING',
        NOW() - INTERVAL '4 days'
    FROM violation_records vr
    WHERE vr.student_id = student1_uuid AND vr.violation_reason = 'Gây mất trật tự trong thư viện'
    LIMIT 1;

    INSERT INTO violation_appeals (id, violation_id, student_id, appeal_reason, status, created_at)
    SELECT 
        gen_random_uuid(),
        vr.id,
        student1_uuid,
        'Đó là chai nước không phải thức ăn, em xin khiếu nại',
        'PENDING',
        NOW() - INTERVAL '2 days'
    FROM violation_records vr
    WHERE vr.student_id = student1_uuid AND vr.violation_reason = 'Sử dụng thức ăn trong thư viện'
    LIMIT 1;

    INSERT INTO violation_appeals (id, violation_id, student_id, appeal_reason, status, created_at)
    SELECT 
        gen_random_uuid(),
        vr.id,
        student2_uuid,
        'Em bị ốm đột ngột nên không thể đến được, có giấy xác nhận từ bác sĩ',
        'PENDING',
        NOW() - INTERVAL '6 days'
    FROM violation_records vr
    WHERE vr.student_id = student2_uuid AND vr.violation_reason = 'Không đến sau khi đặt chỗ'
    LIMIT 1;

    INSERT INTO violation_appeals (id, violation_id, student_id, appeal_reason, status, reviewed_by, review_notes, reviewed_at, created_at)
    SELECT 
        gen_random_uuid(),
        vr.id,
        student3_uuid,
        'Em không gây ồn ào, có thể thủ thư nhầm với người khác',
        'APPROVED',
        librarian_uuid,
        'Sau khi kiểm tra camera, xác nhận đã nhầm người',
        NOW() - INTERVAL '8 days',
        NOW() - INTERVAL '9 days'
    FROM violation_records vr
    WHERE vr.student_id = student3_uuid AND vr.status = 'DISMISSED'
    LIMIT 1;

    INSERT INTO violation_appeals (id, violation_id, student_id, appeal_reason, status, created_at)
    SELECT 
        gen_random_uuid(),
        vr.id,
        student5_uuid,
        'Đó là nước uống không phải thức ăn, em khẩn cầu xem xét lại',
        'PENDING',
        NOW() - INTERVAL '8 days'
    FROM violation_records vr
    WHERE vr.student_id = student5_uuid AND vr.violation_reason = 'Sử dụng thức ăn'
    LIMIT 1;

    INSERT INTO violation_appeals (id, violation_id, student_id, appeal_reason, status, reviewed_by, review_notes, reviewed_at, created_at)
    SELECT 
        gen_random_uuid(),
        vr.id,
        student5_uuid,
        'Em không nói chuyện điện thoại, chỉ nghe nhạc',
        'REJECTED',
        librarian_uuid,
        'Có bằng chứng camera ghi nhận đang gọi điện',
        NOW() - INTERVAL '4 days',
        NOW() - INTERVAL '5 days'
    FROM violation_records vr
    WHERE vr.student_id = student5_uuid AND vr.status = 'CANCELLED'
    LIMIT 1;

    RAISE NOTICE 'Successfully seeded violation data for 5 students';
END $$;
