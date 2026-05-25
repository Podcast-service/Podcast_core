package podcastService.podcast;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PodcastMigrationContractTest {

    @Test
    void migrationAddsAudioFileAndSpeakersConstraints() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V3__add_podcast_audio_file_and_speakers.sql"
        ));

        assertThat(migration).contains("audio_url_file text");
        assertThat(migration).contains("audio_size_file bigint");
        assertThat(migration).contains("num_speakers integer NOT NULL DEFAULT 1");
        assertThat(migration).contains("chk_podcasts_audio_url_file_not_blank");
        assertThat(migration).contains("audio_size_file IS NULL OR audio_size_file >= 0");
        assertThat(migration).contains("num_speakers > 0 AND num_speakers <= 32");
    }
}
