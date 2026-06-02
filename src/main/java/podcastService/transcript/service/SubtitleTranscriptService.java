package podcastService.transcript.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;
import podcastService.media.messaging.contract.SubtitleContentDto;
import podcastService.transcript.service.VttSpeakerBlockParser.SpeakerBlock;
import podcastService.transcript.summary.SubtitleObjectClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubtitleTranscriptService {

    private final SubtitleObjectClient subtitleObjectClient;
    private final VttSpeakerBlockParser vttSpeakerBlockParser;
    private final ObjectMapper objectMapper;

    public JsonNode buildSpeakerBlocks(JsonNode content) {
        SubtitleContentDto subtitleContent = readContent(content);
        String vttObjectKey = subtitleContent.vttObjectKey();
        if (vttObjectKey == null || vttObjectKey.isBlank()) {
            throw new InvalidKafkaMessageException("media.subtitle content has missing vtt_object_key");
        }

        List<SpeakerBlock> speakerBlocks = vttSpeakerBlockParser.parse(subtitleObjectClient.fetch(vttObjectKey));
        if (speakerBlocks.isEmpty()) {
            throw new InvalidKafkaMessageException("Subtitle VTT does not contain speaker blocks");
        }
        return objectMapper.valueToTree(speakerBlocks);
    }

    private SubtitleContentDto readContent(JsonNode content) {
        if (content == null || !content.isObject()) {
            throw new InvalidKafkaMessageException("media.subtitle content must be an object");
        }
        try {
            return objectMapper.treeToValue(content, SubtitleContentDto.class);
        } catch (JsonProcessingException exception) {
            throw new InvalidKafkaMessageException("Failed to deserialize media.subtitle content", exception);
        }
    }
}
