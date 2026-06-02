package podcastService.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.admin.dto.AdminPageResponse;
import podcastService.admin.dto.AdminPodcastFilter;
import podcastService.admin.dto.AdminPodcastResponse;
import podcastService.admin.mapper.AdminMapper;
import podcastService.common.exception.NotFoundException;
import podcastService.infrastructure.outbox.recommendation.PodcastContentEventFactory;
import podcastService.infrastructure.outbox.recommendation.RecommendationOutboxEventService;
import podcastService.podcast.dto.SortPodcasts;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.podcast.specifications.PodcastSpecifications;
import podcastService.transcript.repository.PodcastSummaryRepository;
import podcastService.transcript.repository.PodcastTranscriptRepository;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPodcastService {

    private final PodcastRepository podcastRepository;
    private final PodcastTranscriptRepository podcastTranscriptRepository;
    private final PodcastSummaryRepository podcastSummaryRepository;
    private final RecommendationOutboxEventService recommendationOutboxEventService;
    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public AdminPageResponse<AdminPodcastResponse> getPodcasts(AdminPodcastFilter filter) {
        Specification<PodcastEntity> specification = Specification
                .where(PodcastSpecifications.fetchRelations())
                .and(PodcastSpecifications.withStatus(filter.status()))
                .and(PodcastSpecifications.withAuthorId(filter.authorId()))
                .and(PodcastSpecifications.withCategoryId(filter.categoryId()))
                .and(PodcastSpecifications.searchByText(filter.q()));

        Page<PodcastEntity> page = podcastRepository.findAll(
                specification,
                PageRequest.of(filter.normalizedPage(), filter.normalizedSize(), sort(filter.normalizedSort()))
        );

        Set<UUID> podcastIds = page.getContent().stream()
                .map(PodcastEntity::getId)
                .collect(Collectors.toSet());
        Set<UUID> podcastIdsWithTranscripts = podcastIds.isEmpty()
                ? Set.of()
                : podcastTranscriptRepository.findPodcastIdsWithContent(podcastIds);
        Set<UUID> podcastIdsWithSummaries = podcastIds.isEmpty()
                ? Set.of()
                : podcastSummaryRepository.findPodcastIdsWithSummary(podcastIds);

        return new AdminPageResponse<>(
                page.map(entity -> adminMapper.toPodcastResponse(
                        entity,
                        podcastIdsWithTranscripts,
                        podcastIdsWithSummaries
                )).getContent(),
                filter.normalizedPage(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AdminPodcastResponse getPodcast(UUID podcastId) {
        PodcastEntity podcast = podcastRepository.findDetailedById(podcastId)
                .orElseThrow(() -> new NotFoundException("Podcast not found"));

        Set<UUID> podcastIds = Set.of(podcast.getId());
        return adminMapper.toPodcastResponse(
                podcast,
                podcastTranscriptRepository.findPodcastIdsWithContent(podcastIds),
                podcastSummaryRepository.findPodcastIdsWithSummary(podcastIds)
        );
    }

    @Transactional
    public void deletePodcast(UUID podcastId, UUID currentAdminUserId) {
        PodcastEntity podcast = podcastRepository.findDetailedByIdForUpdate(podcastId)
                .orElseThrow(() -> new NotFoundException("Podcast not found"));

        if (podcast.getStatus() == Status.ARCHIVED) {
            log.info("Admin delete skipped for already archived podcast: podcastId={}, adminUserId={}",
                    podcastId, currentAdminUserId);
            return;
        }

        podcast.setStatus(Status.ARCHIVED);
        podcastRepository.saveAndFlush(podcast);
        recommendationOutboxEventService.savePodcastContentEvent(
                podcast.getId(),
                PodcastContentEventFactory.deleted(
                        podcast.getId(),
                        podcast.getAuthor().getId(),
                        podcast.getCategory() == null ? null : podcast.getCategory().getId(),
                        Instant.now(),
                        currentAdminUserId,
                        null,
                        null
                )
        );

        log.info("Podcast archived by admin: podcastId={}, adminUserId={}, ownerAuthorId={}",
                podcastId, currentAdminUserId, podcast.getAuthor().getId());
    }

    private Sort sort(SortPodcasts sort) {
        return switch (sort) {
            case DATE_ASC -> Sort.by(
                    Sort.Order.asc("createdAt"),
                    Sort.Order.asc("id")
            );
            case RATING -> Sort.by(
                    Sort.Order.desc("likesCount"),
                    Sort.Order.asc("dislikesCount"),
                    Sort.Order.desc("viewsCount"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
            case VIEWS -> Sort.by(
                    Sort.Order.desc("viewsCount"),
                    Sort.Order.desc("likesCount"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
            case DATE_DESC -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        };
    }
}
