package podcastService.transcript.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VttSpeakerBlockParser {

    private static final Pattern SPEAKER_PREFIX = Pattern.compile("(?i)^(SPEAKER[_-]?\\d+)\\s*:\\s*(.*)$");

    public List<SpeakerBlock> parse(String vttContent) {
        if (vttContent == null || vttContent.isBlank()) {
            return List.of();
        }

        List<SpeakerBlockBuilder> blocks = new ArrayList<>();
        String[] lines = vttContent.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            if (!lines[index].contains("-->")) {
                continue;
            }

            String cueSpeaker = null;
            while (++index < lines.length && !lines[index].isBlank()) {
                String line = cleanText(lines[index]);
                if (line.isBlank()) {
                    continue;
                }

                Matcher matcher = SPEAKER_PREFIX.matcher(line);
                if (matcher.matches()) {
                    cueSpeaker = matcher.group(1).toLowerCase(Locale.ROOT);
                    append(blocks, cueSpeaker, matcher.group(2));
                } else if (cueSpeaker != null) {
                    append(blocks, cueSpeaker, line);
                }
            }
        }

        return blocks.stream()
                .map(SpeakerBlockBuilder::build)
                .toList();
    }

    private void append(List<SpeakerBlockBuilder> blocks, String voice, String text) {
        String normalizedText = text.trim().replaceAll("\\s+", " ");
        if (normalizedText.isBlank()) {
            return;
        }

        if (blocks.isEmpty() || !blocks.getLast().voice.equals(voice)) {
            blocks.add(new SpeakerBlockBuilder(voice));
        }
        blocks.getLast().append(normalizedText);
    }

    private String cleanText(String line) {
        return line.replaceAll("<[^>]+>", "").trim();
    }

    public record SpeakerBlock(String text, String voice) {
    }

    private static class SpeakerBlockBuilder {

        private final String voice;
        private final StringBuilder text = new StringBuilder();

        private SpeakerBlockBuilder(String voice) {
            this.voice = voice;
        }

        private void append(String fragment) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(fragment);
        }

        private SpeakerBlock build() {
            return new SpeakerBlock(text.toString(), voice);
        }
    }
}
