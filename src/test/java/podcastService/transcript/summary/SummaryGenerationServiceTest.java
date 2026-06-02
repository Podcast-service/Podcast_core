package podcastService.transcript.summary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.repository.AuthorRepository;
import podcastService.common.exception.ForbiddenOperationException;
import podcastService.common.exception.NotFoundException;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.transcript.dto.PodcastSummaryResponse;
import podcastService.transcript.entity.PodcastSummaryEntity;
import podcastService.transcript.entity.PodcastSummaryId;
import podcastService.transcript.entity.PodcastTranscriptEntity;
import podcastService.transcript.entity.PodcastTranscriptId;
import podcastService.transcript.repository.PodcastSummaryRepository;
import podcastService.transcript.repository.PodcastTranscriptRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryGenerationServiceTest {

    private static final UUID PODCAST_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID AUTHOR_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_AUTHOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

    @Mock private PodcastRepository podcastRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private PodcastTranscriptRepository podcastTranscriptRepository;
    @Mock private PodcastSummaryRepository podcastSummaryRepository;
    @Mock private OpenRouterClient openRouterClient;
    @Mock private TranscriptTextResolver transcriptTextResolver;

    private SummaryGenerationService service;

    @BeforeEach
    void setUp() {
        service = new SummaryGenerationService(
                podcastRepository,
                authorRepository,
                podcastTranscriptRepository,
                podcastSummaryRepository,
                openRouterProperties(true),
                new SummaryGenerationProperties(80, 40),
                new SummaryPromptBuilder(new SummaryPromptProperties(
                        "system {language}",
                        "direct {transcript}",
                        "chunk {chunk}",
                        "final {partial_summaries}"
                )),
                new TranscriptChunkingService(),
                openRouterClient,
                transcriptTextResolver
        );
    }

    @Test
    void doesNotGenerateSummaryIfSummaryAlreadyExistsAndForceFalse() {
        PodcastSummaryEntity existing = summary("Existing summary");
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast()));
        when(podcastSummaryRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.of(existing));

        PodcastSummaryResponse response = service.generate(PODCAST_ID, false);

        assertThat(response.content()).isEqualTo("Existing summary");
        verify(openRouterClient, never()).complete(anyList());
        verify(podcastSummaryRepository, never()).saveAndFlush(any());
    }

    @Test
    void generatesAndSavesSummaryIfTranscriptExistsAndSummaryMissing() {
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast()));
        when(podcastSummaryRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.empty());
        when(podcastTranscriptRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.of(transcript("Это достаточно длинный полезный transcript для генерации краткого summary.")));
        when(transcriptTextResolver.resolve("Это достаточно длинный полезный transcript для генерации краткого summary."))
                .thenReturn("Это достаточно длинный полезный transcript для генерации краткого summary.");
        when(openRouterClient.complete(anyList())).thenReturn("Generated summary");
        when(podcastSummaryRepository.saveAndFlush(any(PodcastSummaryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PodcastSummaryResponse response = service.generate(PODCAST_ID, false);

        assertThat(response.content()).isEqualTo("Generated summary");
        verify(podcastSummaryRepository).saveAndFlush(any(PodcastSummaryEntity.class));
    }

    @Test
    void regeneratesSummaryWhenForceTrue() {
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast()));
        when(podcastSummaryRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.of(summary("Old summary")));
        when(podcastTranscriptRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.of(transcript("Это достаточно длинный полезный transcript для повторной генерации summary.")));
        when(transcriptTextResolver.resolve("Это достаточно длинный полезный transcript для повторной генерации summary."))
                .thenReturn("Это достаточно длинный полезный transcript для повторной генерации summary.");
        when(openRouterClient.complete(anyList())).thenReturn("New summary");
        when(podcastSummaryRepository.saveAndFlush(any(PodcastSummaryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PodcastSummaryResponse response = service.generate(PODCAST_ID, true);

        assertThat(response.content()).isEqualTo("New summary");
        verify(openRouterClient).complete(anyList());
    }

    @Test
    void throwsBusinessErrorIfTranscriptMissing() {
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast()));
        when(podcastSummaryRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.empty());
        when(podcastTranscriptRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(PODCAST_ID, false))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Podcast transcript not found");
    }

    @Test
    void throwsBusinessErrorIfTranscriptBlank() {
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast()));
        when(podcastSummaryRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.empty());
        when(podcastTranscriptRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.of(transcript("   ")));

        assertThatThrownBy(() -> service.generate(PODCAST_ID, false))
                .isInstanceOf(SummaryGenerationException.class)
                .hasMessage("Podcast transcript is blank");
    }

    @Test
    void longTranscriptUsesChunkBasedPipeline() {
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast()));
        when(podcastSummaryRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.empty());
        when(podcastTranscriptRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.of(transcript("""
                        Первый большой фрагмент transcript содержит полезный текст и важные мысли.
                        Второй большой фрагмент transcript содержит продолжение, детали и выводы.
                        Третий большой фрагмент transcript завершает выпуск и добавляет контекст.
                        """)));
        when(transcriptTextResolver.resolve(any()))
                .thenReturn("""
                        Первый большой фрагмент transcript содержит полезный текст и важные мысли.
                        Второй большой фрагмент transcript содержит продолжение, детали и выводы.
                        Третий большой фрагмент transcript завершает выпуск и добавляет контекст.
                        """);
        when(openRouterClient.complete(anyList()))
                .thenReturn("partial one")
                .thenReturn("partial two")
                .thenReturn("partial three")
                .thenReturn("Final summary");
        when(podcastSummaryRepository.saveAndFlush(any(PodcastSummaryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PodcastSummaryResponse response = service.generate(PODCAST_ID, false);

        assertThat(response.content()).isEqualTo("Final summary");
        verify(openRouterClient, org.mockito.Mockito.atLeast(2)).complete(anyList());
    }

    @Test
    void failedChunkGenerationDoesNotSaveFinalSummary() {
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast()));
        when(podcastSummaryRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.empty());
        when(podcastTranscriptRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.of(transcript("""
                        Первый большой фрагмент transcript содержит полезный текст и важные мысли.
                        Второй большой фрагмент transcript содержит продолжение, детали и выводы.
                        Третий большой фрагмент transcript завершает выпуск и добавляет контекст.
                        """)));
        when(transcriptTextResolver.resolve(any()))
                .thenReturn("""
                        Первый большой фрагмент transcript содержит полезный текст и важные мысли.
                        Второй большой фрагмент transcript содержит продолжение, детали и выводы.
                        Третий большой фрагмент transcript завершает выпуск и добавляет контекст.
                        """);
        when(openRouterClient.complete(anyList()))
                .thenThrow(new OpenRouterClientException("OpenRouter service is unavailable"));

        assertThatThrownBy(() -> service.generate(PODCAST_ID, false))
                .isInstanceOf(OpenRouterClientException.class);
        verify(podcastSummaryRepository, never()).saveAndFlush(any());
    }

    @Test
    void generationDisabledThrowsBusinessErrorAndDoesNotReadTranscript() {
        SummaryGenerationService disabledService = new SummaryGenerationService(
                podcastRepository,
                authorRepository,
                podcastTranscriptRepository,
                podcastSummaryRepository,
                openRouterProperties(false),
                new SummaryGenerationProperties(80, 40),
                new SummaryPromptBuilder(new SummaryPromptProperties(null, null, null, null)),
                new TranscriptChunkingService(),
                openRouterClient,
                transcriptTextResolver
        );

        assertThatThrownBy(() -> disabledService.generate(PODCAST_ID, false))
                .isInstanceOf(SummaryGenerationException.class)
                .hasMessage("Summary generation is disabled");
        verify(podcastTranscriptRepository, never()).findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU");
    }

    @Test
    void generationReturnsNotFoundForArchivedPodcast() {
        PodcastEntity podcast = podcast();
        podcast.setStatus(Status.ARCHIVED);
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast));

        assertThatThrownBy(() -> service.generate(PODCAST_ID, false))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Podcast not found");
        verify(podcastTranscriptRepository, never()).findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU");
        verify(openRouterClient, never()).complete(anyList());
    }

    @Test
    void manualGenerationRequiresPodcastOwner() {
        PodcastEntity podcast = podcast();
        podcast.setAuthor(author(AUTHOR_ID));
        when(podcastRepository.findDetailedById(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(authorRepository.findByUserProfileUserId(USER_ID)).thenReturn(Optional.of(author(OTHER_AUTHOR_ID)));

        assertThatThrownBy(() -> service.generateForAuthor(PODCAST_ID, USER_ID, false))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("You don't have permission to modify this resource");
        verify(openRouterClient, never()).complete(anyList());
    }

    @Test
    void technicalSubtitleJsonIsRejectedAsUnusableTranscript() {
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast()));
        when(podcastSummaryRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.empty());
        when(podcastTranscriptRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.of(transcript("""
                        {"vtt_object_key":"media/podcast/subtitles.vtt","srt_object_key":"media/podcast/subtitles.srt","ready_at":"2026-06-01T00:00:00Z"}
                        """)));
        when(transcriptTextResolver.resolve(any()))
                .thenThrow(new SummaryGenerationException("Podcast transcript is not usable for summary generation"));

        assertThatThrownBy(() -> service.generate(PODCAST_ID, false))
                .isInstanceOf(SummaryGenerationException.class)
                .hasMessage("Podcast transcript is not usable for summary generation");
        verify(openRouterClient, never()).complete(anyList());
    }

    private OpenRouterProperties openRouterProperties(boolean enabled) {
        return new OpenRouterProperties(
                enabled,
                "fake-key",
                "https://openrouter.example/api/v1",
                "openrouter/free",
                "https://example.test",
                "Podcast Summary Bot",
                BigDecimal.valueOf(0.3),
                500,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                1,
                Duration.ZERO
        );
    }

    private PodcastEntity podcast() {
        PodcastEntity podcast = new PodcastEntity();
        podcast.setId(PODCAST_ID);
        return podcast;
    }

    private AuthorEntity author(UUID authorId) {
        AuthorEntity author = new AuthorEntity();
        author.setId(authorId);
        return author;
    }

    private PodcastTranscriptEntity transcript(String content) {
        PodcastTranscriptEntity transcript = new PodcastTranscriptEntity();
        transcript.setId(new PodcastTranscriptId(PODCAST_ID, "RU"));
        transcript.setContent(content);
        return transcript;
    }

    private PodcastSummaryEntity summary(String content) {
        PodcastSummaryEntity summary = new PodcastSummaryEntity();
        summary.setId(new PodcastSummaryId(PODCAST_ID, "RU"));
        summary.setContent(content);
        summary.setGeneratedAt(OffsetDateTime.parse("2026-06-01T00:00:00Z"));
        return summary;
    }
}
