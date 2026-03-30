-- V22__rename_categories_to_news_categories.sql
-- Đổi tên bảng categories thành news_categories
-- Nếu bảng đã được rename thủ công thì bỏ qua

DO $$
BEGIN
    -- Chỉ rename nếu bảng categories còn tồn tại
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'categories') THEN
        -- Drop bảng news_categories rỗng nếu Hibernate đã tạo
        DROP TABLE IF EXISTS news_categories CASCADE;
        -- Rename bảng cũ
        ALTER TABLE categories RENAME TO news_categories;
    END IF;

    -- Rename sequence nếu còn tên cũ
    IF EXISTS (SELECT FROM pg_sequences WHERE sequencename = 'categories_id_seq') THEN
        ALTER SEQUENCE categories_id_seq RENAME TO news_categories_id_seq;
    END IF;
END $$;