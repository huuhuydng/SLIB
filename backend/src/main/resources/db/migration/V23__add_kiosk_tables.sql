-- =============================================
-- V23: Kiosk Tables (Slideshow + Library Map)
-- =============================================

-- 1. Bảng lưu ảnh slideshow cho Kiosk
CREATE TABLE IF NOT EXISTS kiosk_images (
    id SERIAL PRIMARY KEY,
    image_name VARCHAR(200) NOT NULL,
    image_url TEXT NOT NULL,
    public_id VARCHAR(255),
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    duration_seconds INTEGER NOT NULL DEFAULT 10,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng bản đồ thư viện
CREATE TABLE IF NOT EXISTS library_maps (
    id SERIAL PRIMARY KEY,
    map_name VARCHAR(200) NOT NULL,
    map_image_url TEXT NOT NULL,
    public_id VARCHAR(255),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Bảng zone trên bản đồ
CREATE TABLE IF NOT EXISTS zone_maps (
    id SERIAL PRIMARY KEY,
    library_map_id INTEGER NOT NULL REFERENCES library_maps (id) ON DELETE CASCADE,
    zone_name VARCHAR(150) NOT NULL,
    zone_type VARCHAR(100) NOT NULL,
    x_position INTEGER NOT NULL,
    y_position INTEGER NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    color_code VARCHAR(20),
    is_interactive BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index cho performance
CREATE INDEX IF NOT EXISTS idx_kiosk_images_active ON kiosk_images (is_active);

CREATE INDEX IF NOT EXISTS idx_kiosk_images_order ON kiosk_images (display_order);

CREATE INDEX IF NOT EXISTS idx_zone_maps_library_map ON zone_maps (library_map_id);