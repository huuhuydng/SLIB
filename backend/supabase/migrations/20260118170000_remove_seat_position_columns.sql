-- Migration: Remove unused seat columns (position_x, position_y, width, height)
-- These fields are no longer used - position is now calculated from row_number/column_number in frontend

ALTER TABLE "public"."seats" DROP COLUMN IF EXISTS "position_x";

ALTER TABLE "public"."seats" DROP COLUMN IF EXISTS "position_y";

ALTER TABLE "public"."seats" DROP COLUMN IF EXISTS "width";

ALTER TABLE "public"."seats" DROP COLUMN IF EXISTS "height";