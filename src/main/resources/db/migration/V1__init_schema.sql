CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS pgcrypto;


CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION normalize_search_text(input text)
RETURNS text
LANGUAGE sql
STABLE
AS $$
  SELECT lower(coalesce(input, ''));
$$;


CREATE OR REPLACE FUNCTION user_profiles_search_vector_tg()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.search_vector :=
    setweight(to_tsvector('simple', normalize_search_text(NEW.username)), 'A');
  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION author_profiles_search_vector_tg()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.search_vector :=
       setweight(to_tsvector('simple', normalize_search_text(NEW.author_name)), 'A')
    || setweight(to_tsvector('simple', normalize_search_text(NEW.description)), 'B');
  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION podcasts_search_vector_tg()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.search_vector :=
       setweight(to_tsvector('simple', normalize_search_text(NEW.title)), 'A')
    || setweight(to_tsvector('simple', normalize_search_text(NEW.description)), 'B');
  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION playlists_search_vector_tg()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.search_vector :=
       setweight(to_tsvector('simple', normalize_search_text(NEW.title)), 'A')
    || setweight(to_tsvector('simple', normalize_search_text(NEW.description)), 'B');
  RETURN NEW;
END;
$$;


CREATE OR REPLACE FUNCTION podcasts_state_tg()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.status = 'PUBLISHED' AND NEW.published_at IS NULL THEN
    NEW.published_at := now();
  END IF;

  IF NEW.status IN ('DRAFT', 'PROCESSING', 'FAILED') THEN
    NEW.published_at := NULL;
  END IF;

  IF NEW.status = 'PUBLISHED' THEN
    IF NEW.audio_url IS NULL THEN
      RAISE EXCEPTION 'podcast cannot be published without audio_url';
    END IF;

    IF NEW.duration_seconds IS NULL OR NEW.duration_seconds <= 0 THEN
      RAISE EXCEPTION 'podcast cannot be published without positive duration_seconds';
    END IF;
  END IF;

  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION subscriptions_no_self_subscribe_tg()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
  author_owner_profile_id uuid;
BEGIN
  SELECT ap.user_profile_id
    INTO author_owner_profile_id
  FROM author_profiles ap
  WHERE ap.id = NEW.author_id;

  IF author_owner_profile_id IS NULL THEN
    RAISE EXCEPTION 'author_profile % does not exist', NEW.author_id;
  END IF;

  IF author_owner_profile_id = NEW.subscriber_profile_id THEN
    RAISE EXCEPTION 'user cannot subscribe to own author profile';
  END IF;

  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION listen_history_validate_tg()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
  p_duration integer;
BEGIN
  SELECT duration_seconds
    INTO p_duration
  FROM podcasts
  WHERE id = NEW.podcast_id;

  IF p_duration IS NOT NULL THEN
    IF NEW.progress_seconds > p_duration THEN
      NEW.progress_seconds := p_duration;
    END IF;

    IF NEW.progress_seconds >= p_duration AND p_duration > 0 THEN
      NEW.completed := true;
    END IF;
  END IF;

  IF NEW.completed AND NEW.progress_seconds = 0 THEN
    RAISE EXCEPTION 'completed listen history cannot have zero progress';
  END IF;

  NEW.last_listened_at := now();
  RETURN NEW;
END;
$$;


CREATE TABLE IF NOT EXISTS user_profiles (
  id uuid PRIMARY KEY DEFAULT uuidv7(),
  user_id uuid NOT NULL UNIQUE,
  username varchar(50) NOT NULL CHECK (btrim(username) <> ''),
  avatar_url text,
  theme varchar(10) NOT NULL DEFAULT 'DARK' CHECK (theme IN ('DARK', 'LIGHT')),
  language varchar(5) NOT NULL DEFAULT 'RU' CHECK (language IN ('RU', 'EN')),
  search_vector tsvector NOT NULL DEFAULT ''::tsvector,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_user_profiles_username_ci
  ON user_profiles (lower(username));

CREATE INDEX gin_user_profiles_search_vector
  ON user_profiles USING GIN (search_vector);

CREATE INDEX gin_user_profiles_username_trgm
  ON user_profiles USING GIN (lower(username) gin_trgm_ops);

CREATE TRIGGER trg_user_profiles_set_updated_at
BEFORE UPDATE ON user_profiles
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_user_profiles_search_vector
BEFORE INSERT OR UPDATE OF username
ON user_profiles
FOR EACH ROW
EXECUTE FUNCTION user_profiles_search_vector_tg();


CREATE TABLE IF NOT EXISTS author_profiles (
  id uuid PRIMARY KEY DEFAULT uuidv7(),
  user_profile_id uuid NOT NULL UNIQUE REFERENCES user_profiles(id) ON DELETE CASCADE,
  author_name varchar(100) NOT NULL CHECK (btrim(author_name) <> ''),
  description text,
  subscribers_count bigint NOT NULL DEFAULT 0 CHECK (subscribers_count >= 0),
  search_vector tsvector NOT NULL DEFAULT ''::tsvector,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX gin_author_profiles_search_vector
  ON author_profiles USING GIN (search_vector);

CREATE INDEX gin_author_profiles_author_name_trgm
  ON author_profiles USING GIN (lower(author_name) gin_trgm_ops);

CREATE TRIGGER trg_author_profiles_set_updated_at
BEFORE UPDATE ON author_profiles
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_author_profiles_search_vector
BEFORE INSERT OR UPDATE OF author_name, description
ON author_profiles
FOR EACH ROW
EXECUTE FUNCTION author_profiles_search_vector_tg();

CREATE TABLE IF NOT EXISTS categories (
    id uuid PRIMARY KEY,
    name varchar(100) NOT NULL CHECK (btrim(name) <> ''),
    position integer NOT NULL CHECK (position >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_categories_position UNIQUE (position) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE UNIQUE INDEX uq_categories_name_ci
  ON categories (lower(name));

CREATE INDEX idx_categories_position
  ON categories (position, created_at);


CREATE TABLE IF NOT EXISTS podcasts (
  id uuid PRIMARY KEY DEFAULT uuidv7(),
  author_id uuid NOT NULL REFERENCES author_profiles(id) ON DELETE RESTRICT,
  category_id uuid REFERENCES categories(id) ON DELETE SET NULL,
  title varchar(255) NOT NULL CHECK (btrim(title) <> ''),
  description text,
  cover_image_url text,
  audio_url text,
  duration_seconds integer CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
  status varchar(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PROCESSING', 'PUBLISHED', 'FAILED', 'ARCHIVED')),
  views_count bigint NOT NULL DEFAULT 0 CHECK (views_count >= 0),
  likes_count bigint NOT NULL DEFAULT 0 CHECK (likes_count >= 0),
  dislikes_count bigint NOT NULL DEFAULT 0 CHECK (dislikes_count >= 0),
  search_vector tsvector NOT NULL DEFAULT ''::tsvector,
  published_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),

  CONSTRAINT chk_podcasts_publish_consistency
    CHECK (
      status <> 'PUBLISHED'
      OR (audio_url IS NOT NULL AND duration_seconds IS NOT NULL AND duration_seconds > 0)
    )
);

CREATE INDEX idx_podcasts_author_id
  ON podcasts (author_id);

CREATE INDEX idx_podcasts_category_id
  ON podcasts (category_id);

CREATE INDEX idx_podcasts_status
  ON podcasts (status);

CREATE INDEX idx_podcasts_created_at_desc
  ON podcasts (created_at DESC);

CREATE INDEX idx_podcasts_published_at_desc
  ON podcasts (published_at DESC);

CREATE INDEX idx_podcasts_feed
  ON podcasts (published_at DESC, id)
  WHERE status = 'PUBLISHED';

CREATE INDEX idx_podcasts_author_feed
  ON podcasts (author_id, published_at DESC, id)
  WHERE status = 'PUBLISHED';

CREATE INDEX gin_podcasts_search_vector
  ON podcasts USING GIN (search_vector);

CREATE INDEX gin_podcasts_title_trgm
  ON podcasts USING GIN (lower(title) gin_trgm_ops);

CREATE TRIGGER trg_podcasts_set_updated_at
BEFORE UPDATE ON podcasts
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_podcasts_search_vector
BEFORE INSERT OR UPDATE OF title, description
ON podcasts
FOR EACH ROW
EXECUTE FUNCTION podcasts_search_vector_tg();

CREATE TRIGGER trg_podcasts_state
BEFORE INSERT OR UPDATE OF status, audio_url, duration_seconds, published_at
ON podcasts
FOR EACH ROW
EXECUTE FUNCTION podcasts_state_tg();


CREATE TABLE IF NOT EXISTS podcast_transcripts (
  podcast_id uuid NOT NULL REFERENCES podcasts(id) ON DELETE CASCADE,
  language varchar(5) NOT NULL DEFAULT 'RU',
  content text NOT NULL CHECK (btrim(content) <> ''),
  generated_at timestamptz NOT NULL DEFAULT now(),

  PRIMARY KEY (podcast_id, language)
);

CREATE INDEX gin_podcast_transcripts_content
  ON podcast_transcripts
  USING GIN (to_tsvector('simple', normalize_search_text(content)));


CREATE TABLE IF NOT EXISTS podcast_summaries (
  podcast_id uuid NOT NULL REFERENCES podcasts(id) ON DELETE CASCADE,
  language varchar(5) NOT NULL DEFAULT 'RU',
  content text NOT NULL CHECK (btrim(content) <> ''),
  generated_at timestamptz NOT NULL DEFAULT now(),

  PRIMARY KEY (podcast_id, language)
);

CREATE INDEX gin_podcast_summaries_content
  ON podcast_summaries
  USING GIN (to_tsvector('simple', normalize_search_text(content)));


CREATE TABLE IF NOT EXISTS playlists (
  id uuid PRIMARY KEY DEFAULT uuidv7(),
  owner_profile_id uuid NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
  title varchar(255) NOT NULL CHECK (btrim(title) <> ''),
  description text,
  cover_image_url text,
  is_public boolean NOT NULL DEFAULT false,
  likes_count bigint NOT NULL DEFAULT 0 CHECK (likes_count >= 0),
  dislikes_count bigint NOT NULL DEFAULT 0 CHECK (dislikes_count >= 0),
  search_vector tsvector NOT NULL DEFAULT ''::tsvector,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_playlists_owner_profile_id
  ON playlists (owner_profile_id, created_at DESC);

CREATE INDEX idx_playlists_public
  ON playlists (created_at DESC, id)
  WHERE is_public = true;

CREATE INDEX gin_playlists_search_vector
  ON playlists USING GIN (search_vector);

CREATE INDEX gin_playlists_title_trgm
  ON playlists USING GIN (lower(title) gin_trgm_ops);

CREATE TRIGGER trg_playlists_set_updated_at
BEFORE UPDATE ON playlists
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_playlists_search_vector
BEFORE INSERT OR UPDATE OF title, description
ON playlists
FOR EACH ROW
EXECUTE FUNCTION playlists_search_vector_tg();


CREATE TABLE IF NOT EXISTS playlist_podcasts (
  playlist_id uuid NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
  podcast_id uuid NOT NULL REFERENCES podcasts(id) ON DELETE CASCADE,
  position integer NOT NULL CHECK (position >= 1),
  added_at timestamptz NOT NULL DEFAULT now(),

  PRIMARY KEY (playlist_id, podcast_id),

  CONSTRAINT uq_playlist_podcasts_position
    UNIQUE (playlist_id, position)
);

CREATE INDEX idx_playlist_podcasts_playlist_position
  ON playlist_podcasts (playlist_id, position);

CREATE INDEX idx_playlist_podcasts_podcast_id
  ON playlist_podcasts (podcast_id);


CREATE TABLE IF NOT EXISTS subscriptions (
  subscriber_profile_id uuid NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
  author_id uuid NOT NULL REFERENCES author_profiles(id) ON DELETE CASCADE,
  subscribed_at timestamptz NOT NULL DEFAULT now(),

  PRIMARY KEY (subscriber_profile_id, author_id)
);

CREATE INDEX idx_subscriptions_author_id
  ON subscriptions (author_id, subscribed_at DESC);

CREATE TRIGGER trg_subscriptions_no_self_subscribe
BEFORE INSERT OR UPDATE ON subscriptions
FOR EACH ROW
EXECUTE FUNCTION subscriptions_no_self_subscribe_tg();


CREATE TABLE IF NOT EXISTS podcast_votes (
  user_profile_id uuid NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
  podcast_id uuid NOT NULL REFERENCES podcasts(id) ON DELETE CASCADE,
  vote_type varchar(10) NOT NULL CHECK (vote_type IN ('LIKE', 'DISLIKE')),
  created_at      timestamptz NOT NULL DEFAULT now(),

  PRIMARY KEY (user_profile_id, podcast_id)
);

CREATE INDEX idx_podcast_votes_podcast_id
  ON podcast_votes (podcast_id);

CREATE INDEX idx_podcast_votes_created_at
  ON podcast_votes (created_at DESC);


CREATE TABLE IF NOT EXISTS playlist_votes (
  user_profile_id uuid NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
  playlist_id uuid NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
  vote_type varchar(10) NOT NULL CHECK (vote_type IN ('LIKE', 'DISLIKE')),
  created_at timestamptz NOT NULL DEFAULT now(),

  PRIMARY KEY (user_profile_id, playlist_id)
);

CREATE INDEX idx_playlist_votes_playlist_id
  ON playlist_votes (playlist_id);

CREATE INDEX idx_playlist_votes_created_at
  ON playlist_votes (created_at DESC);


CREATE TABLE IF NOT EXISTS listen_history (
  user_profile_id uuid NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
  podcast_id uuid NOT NULL REFERENCES podcasts(id) ON DELETE CASCADE,
  progress_seconds integer NOT NULL DEFAULT 0 CHECK (progress_seconds >= 0),
  completed boolean NOT NULL DEFAULT false,
  last_listened_at timestamptz NOT NULL DEFAULT now(),

  PRIMARY KEY (user_profile_id, podcast_id)
);

CREATE INDEX idx_listen_history_user_last_listened
  ON listen_history (user_profile_id, last_listened_at DESC);

CREATE INDEX idx_listen_history_podcast_id
  ON listen_history (podcast_id);

CREATE TRIGGER trg_listen_history_validate
BEFORE INSERT OR UPDATE OF progress_seconds, completed, podcast_id
ON listen_history
FOR EACH ROW
EXECUTE FUNCTION listen_history_validate_tg();