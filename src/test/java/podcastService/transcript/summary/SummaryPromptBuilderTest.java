package podcastService.transcript.summary;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryPromptBuilderTest {

    private static final UUID PODCAST_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Test
    void defaultPromptsRenderDirectSummaryMessages() {
        SummaryPromptBuilder builder = new SummaryPromptBuilder(new SummaryPromptProperties(null, null, null, null));

        List<OpenRouterMessage> messages = builder.buildDirectSummaryMessages(
                "Текст transcript для проверки prompt builder.",
                "RU",
                PODCAST_ID
        );

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).role()).isEqualTo("system");
        assertThat(messages.get(0).content()).contains("RU");
        assertThat(messages.get(1).role()).isEqualTo("user");
        assertThat(messages.get(1).content()).contains("Текст transcript");
        assertThat(messages.get(1).content()).doesNotContain("{transcript}");
    }

    @Test
    void customPromptFromConfigIsUsedAndVariablesAreSubstituted() {
        SummaryPromptBuilder builder = new SummaryPromptBuilder(new SummaryPromptProperties(
                "system {language} {podcast_id}",
                "direct {transcript} {language} {podcast_id}",
                "chunk {chunk} {language} {podcast_id}",
                "final {partial_summaries} {language} {podcast_id}"
        ));

        List<OpenRouterMessage> direct = builder.buildDirectSummaryMessages("original transcript", "EN", PODCAST_ID);
        List<OpenRouterMessage> chunk = builder.buildChunkSummaryMessages("chunk text", "RU", PODCAST_ID);
        List<OpenRouterMessage> finalMessages = builder.buildFinalSummaryMessages(List.of("one", "two"), "RU", PODCAST_ID);

        assertThat(direct.get(0).content()).isEqualTo("system EN " + PODCAST_ID);
        assertThat(direct.get(1).content()).isEqualTo("direct original transcript EN " + PODCAST_ID);
        assertThat(chunk.get(1).content()).isEqualTo("chunk chunk text RU " + PODCAST_ID);
        assertThat(finalMessages.get(1).content()).contains("one").contains("two");
    }

    @Test
    void builderDoesNotMutateInputText() {
        String transcript = "  transcript with spacing  ";
        SummaryPromptBuilder builder = new SummaryPromptBuilder(new SummaryPromptProperties(
                "system",
                "direct [{transcript}]",
                "chunk",
                "final"
        ));

        builder.buildDirectSummaryMessages(transcript, "RU", PODCAST_ID);

        assertThat(transcript).isEqualTo("  transcript with spacing  ");
    }
}
