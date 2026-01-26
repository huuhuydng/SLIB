-- V3__seed_categories.sql
-- Thêm dữ liệu ban đầu cho bảng categories
-- Các ID này tương ứng với frontend hardcode trong NewCreate.jsx

INSERT INTO
    categories (
        id,
        name,
        color_code,
        created_at
    )
VALUES (
        1,
        'Sự kiện',
        '#3B82F6',
        NOW()
    ),
    (
        2,
        'Thông báo quan trọng',
        '#EF4444',
        NOW()
    ),
    (
        3,
        'Sách mới',
        '#10B981',
        NOW()
    ),
    (4, 'Ưu đãi', '#F59E0B', NOW())
ON CONFLICT (id) DO NOTHING;

-- Reset sequence để tránh conflict khi thêm category mới
SELECT setval( 'categories_id_seq', ( SELECT MAX(id) FROM categories ) );