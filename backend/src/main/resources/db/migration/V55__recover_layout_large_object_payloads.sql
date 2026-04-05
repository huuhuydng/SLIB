UPDATE layout_drafts d
SET snapshot_json = convert_from(lo_get(d.snapshot_json::oid), 'UTF8')
WHERE d.snapshot_json ~ '^[0-9]+$'
  AND EXISTS (
      SELECT 1
      FROM pg_largeobject_metadata m
      WHERE m.oid = d.snapshot_json::oid
  );

UPDATE layout_history h
SET snapshot_json = convert_from(lo_get(h.snapshot_json::oid), 'UTF8')
WHERE h.snapshot_json ~ '^[0-9]+$'
  AND EXISTS (
      SELECT 1
      FROM pg_largeobject_metadata m
      WHERE m.oid = h.snapshot_json::oid
  );

UPDATE layout_history h
SET summary = convert_from(lo_get(h.summary::oid), 'UTF8')
WHERE h.summary ~ '^[0-9]+$'
  AND EXISTS (
      SELECT 1
      FROM pg_largeobject_metadata m
      WHERE m.oid = h.summary::oid
  );
