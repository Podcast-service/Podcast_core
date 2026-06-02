ALTER TABLE listen_history
  ADD COLUMN IF NOT EXISTS view_counted boolean NOT NULL DEFAULT false;

UPDATE listen_history
SET view_counted = true
WHERE progress_seconds > 0
  AND view_counted = false;
