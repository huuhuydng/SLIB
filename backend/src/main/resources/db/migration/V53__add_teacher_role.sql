-- flyway:executeInTransaction=false

ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'TEACHER';
