-- Script cleanup messages cũ để fix humanSessionId
-- Chạy 1 lần để update messages đã tạo trước khi sửa logic

-- 1. Xem trước các conversation có human session
SELECT c.id, c.current_human_session, c.status, (
        SELECT COUNT(*)
        FROM messages m
        WHERE
            m.conversation_id = c.id
    ) as msg_count
FROM conversations c
WHERE
    c.current_human_session > 0;

-- 2. Update LIBRARIAN messages cũ có humanSessionId = NULL
-- Gán về human_session = 1 (vì trước đây chưa có tracking)
UPDATE messages
SET
    human_session_id = 1
WHERE
    sender_type = 'LIBRARIAN'
    AND human_session_id IS NULL;

-- 3. QUAN TRỌNG: Để clean hoàn toàn, có thể xóa tất cả messages cũ
-- CẢNH BÁO: Chỉ chạy nếu muốn reset toàn bộ chat history!
-- DELETE FROM messages WHERE conversation_id IN (SELECT id FROM conversations);

-- 4. Reset human session counter về 0 để bắt đầu fresh
-- UPDATE conversations SET current_human_session = 0;

-- 5. Verify sau khi cleanup
SELECT m.id, m.sender_type, m.human_session_id, LEFT(m.content, 30) as preview
FROM messages m
WHERE
    m.sender_type = 'LIBRARIAN'
ORDER BY m.created_at DESC
LIMIT 20;