ALTER TABLE podcasts
  ADD COLUMN IF NOT EXISTS audio_url_file text,
  ADD COLUMN IF NOT EXISTS audio_size_file bigint,
  ADD COLUMN IF NOT EXISTS num_speakers integer NOT NULL DEFAULT 1;

ALTER TABLE podcasts
  ADD CONSTRAINT chk_podcasts_audio_url_file_not_blank
    CHECK (audio_url_file IS NULL OR btrim(audio_url_file) <> ''),
  ADD CONSTRAINT chk_podcasts_audio_size_file_non_negative
    CHECK (audio_size_file IS NULL OR audio_size_file >= 0),
  ADD CONSTRAINT chk_podcasts_num_speakers_range
    CHECK (num_speakers > 0 AND num_speakers <= 32);
