package podcastService.transcript.summary;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptChunkingServiceTest {

    private final TranscriptChunkingService service = new TranscriptChunkingService();

    @Test
    void shortTranscriptReturnsOneChunk() {
        assertThat(service.split("short transcript", 100))
                .containsExactly("short transcript");
    }

    @Test
    void longTranscriptReturnsMultipleChunksWithinLimitWherePossible() {
        List<String> chunks = service.split("one two three four five six seven eight nine ten", 16);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(16));
    }

    @Test
    void chunkingDoesNotBreakWordsWherePossible() {
        List<String> chunks = service.split("alpha beta gamma delta", 12);

        assertThat(chunks).containsExactly("alpha beta", "gamma delta");
    }

    @Test
    void blankTranscriptReturnsNoChunks() {
        assertThat(service.split("  ", 10)).isEmpty();
    }
}
