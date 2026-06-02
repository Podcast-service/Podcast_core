package podcastService.transcript.summary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
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
import podcastService.transcript.repository.PodcastSummaryRepository;
import podcastService.transcript.repository.PodcastTranscriptRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryGenerationService {

    public static final String DEFAULT_LANGUAGE = "RU";
    private static final long MIN_USEFUL_LETTERS = 20;

    private final PodcastRepository podcastRepository;
    private final AuthorRepository authorRepository;
    private final PodcastTranscriptRepository podcastTranscriptRepository;
    private final PodcastSummaryRepository podcastSummaryRepository;
    private final OpenRouterProperties openRouterProperties;
    private final SummaryGenerationProperties summaryGenerationProperties;
    private final SummaryPromptBuilder promptBuilder;
    private final TranscriptChunkingService chunkingService;
    private final OpenRouterClient openRouterClient;
    private final TranscriptTextResolver transcriptTextResolver;

    public PodcastSummaryResponse generate(UUID podcastId, boolean force) {
        if (!openRouterProperties.enabled()) {
            throw new SummaryGenerationException("Summary generation is disabled");
        }

        PodcastEntity podcast = podcastRepository.findById(podcastId)
                .orElseThrow(() -> new NotFoundException("Podcast not found"));
        if (podcast.getStatus() == Status.ARCHIVED) {
            throw new NotFoundException("Podcast not found");
        }

        if (!force) {
            PodcastSummaryEntity existing = podcastSummaryRepository
                    .findByIdPodcastIdAndIdLanguage(podcastId, DEFAULT_LANGUAGE)
                    .orElse(null);
            if (existing != null) {
                log.info("Podcast summary already exists, podcastId={}, language={}, force=false",
                        podcastId, DEFAULT_LANGUAGE);
                return toResponse(existing);
            }
        }

        PodcastTranscriptEntity transcript = podcastTranscriptRepository
                .findByIdPodcastIdAndIdLanguage(podcastId, DEFAULT_LANGUAGE)
                .orElseThrow(() -> new NotFoundException("Podcast transcript not found"));

        String transcriptText = normalizeTranscript(transcript.getContent());
        log.info("Podcast summary generation started, podcastId={}, language={}, force={}, transcriptLength={}",
                podcastId, DEFAULT_LANGUAGE, force, transcriptText.length());

        String content = generateContent(podcastId, DEFAULT_LANGUAGE, transcriptText);
        return saveFinalSummary(podcast, content, force);
    }

    public PodcastSummaryResponse generateForAuthor(UUID podcastId, UUID currentUserId, boolean force) {
        PodcastEntity podcast = podcastRepository.findDetailedById(podcastId)
                .orElseThrow(() -> new NotFoundException("Podcast not found"));
        if (podcast.getStatus() == Status.ARCHIVED) {
            throw new NotFoundException("Podcast not found");
        }
        UUID currentAuthorId = authorRepository.findByUserProfileUserId(currentUserId)
                .map(AuthorEntity::getId)
                .orElseThrow(() -> new ForbiddenOperationException("Current user does not have author profile"));
        if (!podcast.getAuthor().getId().equals(currentAuthorId)) {
            throw new ForbiddenOperationException("You don't have permission to modify this resource");
        }
        return generate(podcastId, force);
    }

    public void generateIfMissing(UUID podcastId, String language) {
        String effectiveLanguage = language == null || language.isBlank() ? DEFAULT_LANGUAGE : language;
        if (!DEFAULT_LANGUAGE.equals(effectiveLanguage)) {
            log.info("Podcast summary auto-generation skipped for unsupported language, podcastId={}, language={}",
                    podcastId, effectiveLanguage);
            return;
        }
        if (podcastSummaryRepository.findByIdPodcastIdAndIdLanguage(podcastId, DEFAULT_LANGUAGE).isPresent()) {
            log.info("Podcast summary auto-generation skipped because summary already exists, podcastId={}, language={}",
                    podcastId, DEFAULT_LANGUAGE);
            return;
        }
        generate(podcastId, false);
    }

    private String generateContent(UUID podcastId, String language, String transcriptText) {
        if (transcriptText.length() <= summaryGenerationProperties.directMaxChars()) {
            return openRouterClient.complete(
                    promptBuilder.buildDirectSummaryMessages(transcriptText, language, podcastId)
            );
        }

        List<String> chunks = chunkingService.split(transcriptText, summaryGenerationProperties.chunkSizeChars());
        if (chunks.isEmpty()) {
            throw new SummaryGenerationException("Podcast transcript is not usable for summary generation");
        }

        log.info("Podcast transcript is long; chunked summary generation started, podcastId={}, language={}, chunkCount={}",
                podcastId, language, chunks.size());

        List<String> partialSummaries = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            String chunk = chunks.get(index);
            log.info("Podcast transcript chunk summary started, podcastId={}, language={}, chunkIndex={}, chunkLength={}",
                    podcastId, language, index + 1, chunk.length());
            String partialSummary = openRouterClient.complete(
                    promptBuilder.buildChunkSummaryMessages(chunk, language, podcastId)
            );
            partialSummaries.add(partialSummary);
            log.info("Podcast transcript chunk summary finished, podcastId={}, language={}, chunkIndex={}",
                    podcastId, language, index + 1);
        }

        return openRouterClient.complete(
                promptBuilder.buildFinalSummaryMessages(partialSummaries, language, podcastId)
        );
    }

    private PodcastSummaryResponse saveFinalSummary(PodcastEntity podcast, String content, boolean force) {
        String normalizedContent = normalizeSummary(content);
        UUID podcastId = podcast.getId();

        if (!force) {
            PodcastSummaryEntity existing = podcastSummaryRepository
                    .findByIdPodcastIdAndIdLanguage(podcastId, DEFAULT_LANGUAGE)
                    .orElse(null);
            if (existing != null) {
                log.info("Podcast summary appeared during generation, podcastId={}, language={}",
                        podcastId, DEFAULT_LANGUAGE);
                return toResponse(existing);
            }
        }

        PodcastSummaryEntity summary = podcastSummaryRepository
                .findByIdPodcastIdAndIdLanguage(podcastId, DEFAULT_LANGUAGE)
                .orElseGet(() -> {
                    PodcastSummaryEntity created = new PodcastSummaryEntity();
                    created.setId(new PodcastSummaryId(podcastId, DEFAULT_LANGUAGE));
                    created.setPodcast(podcast);
                    return created;
                });

        summary.setContent(normalizedContent);
        summary.setGeneratedAt(OffsetDateTime.now());

        try {
            PodcastSummaryEntity saved = podcastSummaryRepository.saveAndFlush(summary);
            log.info("Podcast summary saved, podcastId={}, language={}, force={}, summaryLength={}",
                    podcastId, DEFAULT_LANGUAGE, force, normalizedContent.length());
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            PodcastSummaryEntity existing = podcastSummaryRepository
                    .findByIdPodcastIdAndIdLanguage(podcastId, DEFAULT_LANGUAGE)
                    .orElseThrow(() -> exception);
            log.info("Podcast summary save conflict resolved by loading existing summary, podcastId={}, language={}",
                    podcastId, DEFAULT_LANGUAGE);
            return toResponse(existing);
        }
    }

    private String normalizeTranscript(String content) {
        if (content == null || content.isBlank()) {
            throw new SummaryGenerationException("Podcast transcript is blank");
        }
        String normalized = transcriptTextResolver.resolve(content);
        long letterCount = normalized.codePoints()
                .filter(Character::isLetter)
                .count();
        if (letterCount < MIN_USEFUL_LETTERS) {
            throw new SummaryGenerationException("Podcast transcript is not usable for summary generation");
        }
        return normalized;
    }

    private String normalizeSummary(String content) {
        if (content == null || content.isBlank()) {
            throw new OpenRouterClientException("OpenRouter returned blank summary");
        }
        return content.trim();
    }

    private PodcastSummaryResponse toResponse(PodcastSummaryEntity summary) {
        return new PodcastSummaryResponse(
                summary.getId().getPodcastId(),
                summary.getId().getLanguage(),
                summary.getContent(),
                summary.getGeneratedAt()
        );
    }
}
