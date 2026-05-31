CREATE TABLE IF NOT EXISTS saved_playlists (
    user_profile_id uuid NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    playlist_id uuid NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
    saved_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_saved_playlists PRIMARY KEY (user_profile_id, playlist_id)
);

CREATE INDEX IF NOT EXISTS idx_saved_playlists_user_saved_at
    ON saved_playlists (user_profile_id, saved_at DESC);

CREATE INDEX IF NOT EXISTS idx_saved_playlists_playlist
    ON saved_playlists (playlist_id);
