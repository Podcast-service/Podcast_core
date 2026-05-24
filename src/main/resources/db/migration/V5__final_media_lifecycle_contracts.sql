DO $$
DECLARE
  status_constraint_name text;
BEGIN
  SELECT conname
    INTO status_constraint_name
  FROM pg_constraint
  WHERE conrelid = 'podcasts'::regclass
    AND contype = 'c'
    AND pg_get_constraintdef(oid) LIKE '%status%'
    AND pg_get_constraintdef(oid) LIKE '%DRAFT%'
  LIMIT 1;

  IF status_constraint_name IS NOT NULL THEN
    EXECUTE format('ALTER TABLE podcasts DROP CONSTRAINT %I', status_constraint_name);
  END IF;
END $$;

UPDATE podcasts
   SET status = 'UPLOADED'
 WHERE status = 'READY_TO_PUBLISH';

UPDATE podcasts
   SET status = 'FAILED'
 WHERE status = 'UPLOAD_ERROR';

ALTER TABLE podcasts
  ADD CONSTRAINT chk_podcasts_status
  CHECK (status IN ('DRAFT', 'UPLOADING', 'UPLOADED', 'PROCESSING', 'PROCESSED', 'PUBLISHED', 'FAILED', 'ARCHIVED'));

ALTER TABLE podcasts
  DROP CONSTRAINT IF EXISTS chk_podcasts_publish_consistency;

ALTER TABLE podcasts
  ADD CONSTRAINT chk_podcasts_publish_consistency
  CHECK (
    status <> 'PUBLISHED'
    OR (
      audio_url IS NOT NULL
      AND btrim(audio_url) <> ''
      AND duration_seconds IS NOT NULL
      AND duration_seconds > 0
    )
  );

CREATE OR REPLACE FUNCTION podcasts_state_tg()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.status = 'PUBLISHED' AND NEW.published_at IS NULL THEN
    NEW.published_at := now();
  END IF;

  IF NEW.status IN ('DRAFT', 'UPLOADING', 'UPLOADED', 'PROCESSING', 'PROCESSED', 'FAILED') THEN
    NEW.published_at := NULL;
  END IF;

  IF NEW.status = 'PUBLISHED' THEN
    IF NEW.audio_url IS NULL OR btrim(NEW.audio_url) = '' THEN
      RAISE EXCEPTION 'podcast cannot be published without audio_url';
    END IF;

    IF NEW.duration_seconds IS NULL OR NEW.duration_seconds <= 0 THEN
      RAISE EXCEPTION 'podcast cannot be published without positive duration_seconds';
    END IF;
  END IF;

  RETURN NEW;
END;
$$;
