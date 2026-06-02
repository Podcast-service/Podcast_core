package podcastService.transcript.summary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TranscriptTextResolver {

    private static final List<String> TECHNICAL_JSON_FIELDS = List.of(
            "vtt_object_key",
            "srt_object_key",
            "ready_at",
            "timestamp",
            "voice"
    );

    private final ObjectMapper objectMapper;
    private final SubtitleObjectClient subtitleObjectClient;

    public String resolve(String content) {
        if (content == null || content.isBlank()) {
            throw new SummaryGenerationException("Podcast transcript is blank");
        }
        String normalized = content.trim();
        if (!looksLikeJson(normalized)) {
            return normalized;
        }
        try {
            JsonNode root = objectMapper.readTree(normalized);
            String objectKey = preferredSubtitleObjectKey(root);
            if (objectKey != null) {
                return cleanSubtitleText(subtitleObjectClient.fetch(objectKey));
            }
            return extractTextFromJson(root);
        } catch (JsonProcessingException exception) {
            return normalized;
        }
    }

    private String preferredSubtitleObjectKey(JsonNode root) {
        if (root == null || !root.isObject()) {
            return null;
        }
        JsonNode srtObjectKey = root.get("srt_object_key");
        if (srtObjectKey != null && srtObjectKey.isTextual() && !srtObjectKey.asText().isBlank()) {
            return srtObjectKey.asText();
        }
        JsonNode vttObjectKey = root.get("vtt_object_key");
        if (vttObjectKey != null && vttObjectKey.isTextual() && !vttObjectKey.asText().isBlank()) {
            return vttObjectKey.asText();
        }
        return null;
    }

    private String extractTextFromJson(JsonNode root) {
        List<String> values = new ArrayList<>();
        collectTextValues(root, null, values);
        return String.join("\n", values).trim();
    }

    private void collectTextValues(JsonNode node, String fieldName, List<String> values) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            String value = node.asText().trim();
            if (!value.isBlank() && !isTechnicalField(fieldName) && !looksLikeTechnicalValue(value)) {
                values.add(value);
            }
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectTextValues(child, fieldName, values));
            return;
        }
        if (node.isObject()) {
            for (Map.Entry<String, JsonNode> field : node.properties()) {
                collectTextValues(field.getValue(), field.getKey(), values);
            }
        }
    }

    private String cleanSubtitleText(String subtitleContent) {
        List<String> lines = subtitleContent.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.equalsIgnoreCase("WEBVTT"))
                .filter(line -> !line.matches("\\d+"))
                .filter(line -> !line.contains("-->"))
                .map(line -> line.replaceAll("<[^>]+>", "").trim())
                .filter(line -> !line.isBlank())
                .toList();
        return String.join("\n", lines).trim();
    }

    private boolean looksLikeJson(String content) {
        return (content.startsWith("{") && content.endsWith("}"))
                || (content.startsWith("[") && content.endsWith("]"));
    }

    private boolean isTechnicalField(String fieldName) {
        return fieldName != null && TECHNICAL_JSON_FIELDS.contains(fieldName);
    }

    private boolean looksLikeTechnicalValue(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.endsWith(".vtt")
                || lower.endsWith(".srt")
                || lower.matches("\\d{4}-\\d{2}-\\d{2}t.*");
    }
}
