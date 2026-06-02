CREATE TABLE IF NOT EXISTS outbox_events (
  id uuid PRIMARY KEY,
  aggregate_type varchar(64) NOT NULL,
  aggregate_id uuid,
  event_type varchar(128) NOT NULL,
  event_version int NOT NULL DEFAULT 1,
  event_key varchar(128),
  payload jsonb NOT NULL,
  headers jsonb,
  status varchar(32) NOT NULL DEFAULT 'NEW',
  retry_count int NOT NULL DEFAULT 0,
  last_error text,
  created_at timestamptz NOT NULL DEFAULT now(),
  available_at timestamptz NOT NULL DEFAULT now(),
  sent_at timestamptz
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status_available_at
  ON outbox_events (status, available_at);

CREATE INDEX IF NOT EXISTS idx_outbox_events_created_at
  ON outbox_events (created_at);

CREATE INDEX IF NOT EXISTS idx_outbox_events_event_type
  ON outbox_events (event_type);
