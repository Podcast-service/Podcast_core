package podcastService.transcript.summary;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TranscriptChunkingService {

    public List<String> split(String transcript, int maxChunkChars) {
        if (transcript == null || transcript.isBlank()) {
            return List.of();
        }
        if (maxChunkChars <= 0) {
            throw new IllegalArgumentException("maxChunkChars must be positive");
        }

        String normalized = transcript.trim();
        if (normalized.length() <= maxChunkChars) {
            return List.of(normalized);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + maxChunkChars, normalized.length());
            if (end < normalized.length()) {
                end = findSafeSplit(normalized, start, end);
            }

            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            start = end;
            while (start < normalized.length() && Character.isWhitespace(normalized.charAt(start))) {
                start++;
            }
        }
        return chunks;
    }

    private int findSafeSplit(String text, int start, int limit) {
        int paragraphSplit = text.lastIndexOf("\n\n", limit);
        if (paragraphSplit > start) {
            return paragraphSplit;
        }

        int sentenceSplit = Math.max(
                Math.max(text.lastIndexOf(". ", limit), text.lastIndexOf("! ", limit)),
                text.lastIndexOf("? ", limit)
        );
        if (sentenceSplit > start) {
            return sentenceSplit + 1;
        }

        int whitespaceSplit = -1;
        for (int index = limit; index > start; index--) {
            if (Character.isWhitespace(text.charAt(index - 1))) {
                whitespaceSplit = index - 1;
                break;
            }
        }
        return whitespaceSplit > start ? whitespaceSplit : limit;
    }
}
