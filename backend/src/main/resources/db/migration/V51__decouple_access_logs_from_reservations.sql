DO $$
DECLARE
    access_logs_fk_name text;
BEGIN
    SELECT c.conname
    INTO access_logs_fk_name
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    JOIN pg_namespace n ON n.oid = t.relnamespace
    JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY (c.conkey)
    WHERE c.contype = 'f'
      AND t.relname = 'access_logs'
      AND a.attname = 'reservation_id'
      AND n.nspname = current_schema()
    LIMIT 1;

    IF access_logs_fk_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE access_logs DROP CONSTRAINT %I', access_logs_fk_name);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY (c.conkey)
        WHERE c.contype = 'f'
          AND t.relname = 'access_logs'
          AND a.attname = 'reservation_id'
          AND n.nspname = current_schema()
    ) THEN
        ALTER TABLE access_logs
            ADD CONSTRAINT access_logs_reservation_id_fkey
            FOREIGN KEY (reservation_id)
            REFERENCES reservations (reservation_id)
            ON DELETE SET NULL;
    END IF;
END $$;
