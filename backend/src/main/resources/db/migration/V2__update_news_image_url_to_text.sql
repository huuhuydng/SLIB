-- Update image_url column to TEXT to support base64 images
ALTER TABLE news ALTER COLUMN image_url TYPE TEXT;
