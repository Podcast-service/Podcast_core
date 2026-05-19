DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_author_profiles_author_name_length'
  ) THEN
    ALTER TABLE author_profiles
      ADD CONSTRAINT chk_author_profiles_author_name_length
      CHECK (char_length(btrim(author_name)) BETWEEN 2 AND 100);
  END IF;
END;
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_author_profiles_description_length'
  ) THEN
    ALTER TABLE author_profiles
      ADD CONSTRAINT chk_author_profiles_description_length
      CHECK (description IS NULL OR char_length(description) <= 1000);
  END IF;
END;
$$;
