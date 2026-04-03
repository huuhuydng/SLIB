-- Script xoa tất cả user và dữ liệu liên quan
-- NGOẠI TRỪ 5 user có ID sau:
-- 4e3f4237-868c-4c22-a2ed-b355b329fb7f
-- fade597b-7d99-45ce-b38f-25e8b6fd4c6c
-- bc58d92f-deca-41ab-9526-3489af68d6a3
-- 211ef11c-5b39-4e95-ace3-03d422992330
-- 764662cc-7c32-4070-8a83-5774cc428fcd

-- Chay trong transaction de dam bao an toan
BEGIN;

-- Danh sach user KHONG BI XOA
DO $$
DECLARE
    protected_users UUID[] := ARRAY[
        '4e3f4237-868c-4c22-a2ed-b355b329fb7f'::uuid,
        'fade597b-7d99-45ce-b38f-25e8b6fd4c6c'::uuid,
        'bc58d92f-deca-41ab-9526-3489af68d6a3'::uuid,
        '211ef11c-5b39-4e95-ace3-03d422992330'::uuid,
        '764662cc-7c32-4070-8a83-5774cc428fcd'::uuid
    ];
    deleted_count INTEGER;
BEGIN
    -- 1. Xoa chat messages (sender_id, receiver_id la user)
    DELETE FROM messages WHERE sender_id NOT IN (SELECT unnest(protected_users))
        OR receiver_id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % messages', deleted_count;
    
    -- 2. Xoa conversations (student_id, librarian_id la user)
    DELETE FROM conversations 
    WHERE student_id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % conversations', deleted_count;
    
    -- 3. Xoa AI chat messages (phu thuoc session)
    DELETE FROM chat_messages WHERE session_id IN (
        SELECT id FROM chat_sessions WHERE user_id NOT IN (SELECT unnest(protected_users))
    );
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % chat_messages (AI)', deleted_count;
    
    -- 4. Xoa AI chat sessions
    DELETE FROM chat_sessions WHERE user_id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % chat_sessions', deleted_count;
    
    -- 5. Xoa reservations (bookings)
    DELETE FROM reservations WHERE user_id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % reservations', deleted_count;
    
    -- 6. Xoa notifications
    DELETE FROM notifications WHERE user_id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % notifications', deleted_count;
    
    -- 7. Xoa complaints
    DELETE FROM complaints WHERE user_id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % complaints', deleted_count;
    
    -- 8. Xoa feedbacks
    DELETE FROM feedbacks WHERE user_id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % feedbacks', deleted_count;
    
    -- 9. Xoa seat status reports
    DELETE FROM seat_status_reports WHERE user_id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % seat_status_reports', deleted_count;
    
    -- 10. Xoa seat violation reports (reporter_id, violator_id la user)
    DELETE FROM seat_violation_reports WHERE reporter_id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % seat_violation_reports', deleted_count;
    
    -- 11. Xoa point transactions (user_id la UUID truc tiep)
    DELETE FROM point_transactions WHERE user_id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % point_transactions', deleted_count;
    
    -- 12. Xoa system logs
    DELETE FROM system_logs WHERE user_id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % system_logs', deleted_count;
    
    -- 13. Xoa access logs (HCE)
    DELETE FROM access_logs WHERE user_id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % access_logs', deleted_count;
    
    -- 14. Xoa refresh tokens
    DELETE FROM refresh_tokens WHERE user_id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % refresh_tokens', deleted_count;
    
    -- 15. Xoa student profiles
    DELETE FROM student_profiles WHERE user_id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % student_profiles', deleted_count;
    
    -- 16. Xoa user settings
    DELETE FROM user_settings WHERE user_id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % user_settings', deleted_count;
    
    -- 17. Xoa user import staging (neu co)
    DELETE FROM user_import_staging;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % user_import_staging', deleted_count;
    
    -- 18. Cuoi cung: Xoa users
    DELETE FROM users WHERE id NOT IN (SELECT unnest(protected_users));
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Da xoa % users', deleted_count;
    
    RAISE NOTICE '=== HOAN THANH ===';
END $$;

-- Kiem tra ket qua
SELECT 'Users con lai: ' || COUNT(*) as result FROM users;

-- Neu moi thu OK, commit
COMMIT;

-- Neu co loi, uncomment dong duoi de rollback
-- ROLLBACK;