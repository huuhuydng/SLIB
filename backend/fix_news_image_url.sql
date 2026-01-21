-- Fix image_url column to support base64 images
ALTER TABLE public.news ALTER COLUMN image_url TYPE TEXT;
