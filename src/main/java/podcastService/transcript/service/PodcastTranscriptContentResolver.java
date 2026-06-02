package podcastService.transcript.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import podcastService.transcript.service.VttSpeakerBlockParser.SpeakerBlock;
import podcastService.transcript.summary.SubtitleObjectClient;
import podcastService.transcript.summary.SubtitleObjectStorageException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PodcastTranscriptContentResolver {

    private final ObjectMapper objectMapper;
    private final SubtitleObjectClient subtitleObjectClient;
    private final VttSpeakerBlockParser vttSpeakerBlockParser;

    public JsonNode resolve(String content) {
        JsonNode storedContent = parseStoredContent(content);
        String objectKey = preferredSubtitleObjectKey(storedContent);
        if (objectKey == null) {
            return storedContent;
        }

        List<SpeakerBlock> speakerBlocks = vttSpeakerBlockParser.parse(subtitleObjectClient.fetch(objectKey));
        if (speakerBlocks.isEmpty()) {
            throw new SubtitleObjectStorageException("Subtitle object does not contain speaker blocks");
        }
        return objectMapper.valueToTree(speakerBlocks);
    }

    private JsonNode parseStoredContent(String content) {
        if (content == null || content.isBlank()) {
            return objectMapper.getNodeFactory().textNode(content);
        }
        try {
            return objectMapper.readTree(content);
        } catch (JsonProcessingException exception) {
            return objectMapper.getNodeFactory().textNode(content);
        }
    }

    private String preferredSubtitleObjectKey(JsonNode content) {
        if (content == null || !content.isObject()) {
            return null;
        }
        String vttObjectKey = textualValue(content, "vtt_object_key");
        return vttObjectKey != null ? vttObjectKey : textualValue(content, "srt_object_key");
    }

    private String textualValue(JsonNode content, String fieldName) {
        JsonNode value = content.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            return null;
        }
        return value.asText();
    }
}
