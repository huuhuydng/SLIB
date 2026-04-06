DELETE FROM layout_drafts
WHERE updated_by_user_id IS NULL;

DELETE FROM layout_drafts older
USING layout_drafts newer
WHERE older.updated_by_user_id = newer.updated_by_user_id
  AND older.draft_id < newer.draft_id;

CREATE UNIQUE INDEX IF NOT EXISTS ux_layout_drafts_updated_by_user_id
ON layout_drafts(updated_by_user_id)
WHERE updated_by_user_id IS NOT NULL;
