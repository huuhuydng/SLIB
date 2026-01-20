-- Insert default categories for news
INSERT INTO public.categories (id, name, color_code) VALUES
(1, 'Sự kiện', '#FF6B6B'),
(2, 'Thông báo quan trọng', '#F37021'),
(3, 'Sách mới', '#4ECDC4'),
(4, 'Ưu đãi', '#95E1D3')
ON CONFLICT (id) DO NOTHING;

-- Reset sequence to continue from 5
SELECT setval('public.categories_id_seq', (SELECT MAX(id) FROM public.categories));
