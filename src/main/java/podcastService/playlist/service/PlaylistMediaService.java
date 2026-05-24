package podcastService.playlist.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;
import podcastService.infrastructure.messaging.error.KafkaRetryableProcessingException;
import podcastService.playlist.repository.PlaylistRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistMediaService {

    private final PlaylistRepository playlistRepository;

    @Transactional
    public void updateCoverFromMediaEvent(UUID playlistId, String coverImageUrl) {
        if (playlistId == null) {
            throw new InvalidKafkaMessageException("Received null playlist object_id in Kafka");
        }

        String normalizedCover = normalizeMediaPath(coverImageUrl, "playlist cover");
        int updated = playlistRepository.updateCoverImageUrl(playlistId, normalizedCover);
        if (updated == 0) {
            throw new KafkaRetryableProcessingException(
                    "Playlist not found for media event, playlistId=" + playlistId,
                    null
            );
        }

        log.info("Playlist cover updated from Kafka media event, playlistId={}", playlistId);
    }

    private String normalizeMediaPath(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidKafkaMessageException("Received blank " + fieldName + " in Kafka");
        }
        return value.trim();
    }
}
