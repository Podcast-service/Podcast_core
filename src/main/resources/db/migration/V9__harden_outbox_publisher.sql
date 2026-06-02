ALTER TABLE outbox_events
  ADD COLUMN IF NOT EXISTS processing_started_at timestamptz;

UPDATE outbox_events
SET processing_started_at = created_at
WHERE status = 'PROCESSING'
  AND processing_started_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_outbox_events_processing_started_at
  ON outbox_events (processing_started_at)
  WHERE status = 'PROCESSING';
