-- ============================================================
-- SLIB - Smart Library Ecosystem
-- Migration: Remove color columns and add is_locked for factories
-- Date: 2026-01-16
-- Description: 
--   - Remove color column from area_factories (using fixed gray now)
--   - Remove color column from zones (using fixed gray now)
--   - Add is_locked column to area_factories for locking movement
-- ============================================================

-- ============================================================
-- 1. Remove color column from area_factories
-- ============================================================
ALTER TABLE "public"."area_factories" DROP COLUMN IF EXISTS "color";

-- ============================================================
-- 2. Remove color column from zones
-- ============================================================
ALTER TABLE "public"."zones" DROP COLUMN IF EXISTS "color";

-- ============================================================
-- 3. Add is_locked column to area_factories
-- ============================================================
ALTER TABLE "public"."area_factories" 
ADD COLUMN IF NOT EXISTS "is_locked" BOOLEAN DEFAULT FALSE NOT NULL;

COMMENT ON COLUMN "public"."area_factories"."is_locked" IS 'Khóa di chuyển vật cản';

-- ============================================================
-- MIGRATION COMPLETE
-- ============================================================
DO $$
BEGIN
    RAISE NOTICE 'Migration completed: Removed color columns, added is_locked to area_factories';
END $$;
