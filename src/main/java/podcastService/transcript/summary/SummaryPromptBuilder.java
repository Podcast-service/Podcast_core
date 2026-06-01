package podcastService.transcript.summary;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SummaryPromptBuilder {

    private final SummaryPromptProperties properties;

    public List<OpenRouterMessage> buildDirectSummaryMessages(String transcript, String language, UUID podcastId) {
        Map<String, String> variables = variables(language, podcastId);
        variables.put("transcript", transcript);
        return messages(render(properties.directSummary(), variables), variables);
    }

    public List<OpenRouterMessage> buildChunkSummaryMessages(String chunk, String language, UUID podcastId) {
        Map<String, String> variables = variables(language, podcastId);
        variables.put("chunk", chunk);
        return messages(render(properties.chunkSummary(), variables), variables);
    }

    public List<OpenRouterMessage> buildFinalSummaryMessages(List<String> partialSummaries, String language, UUID podcastId) {
        Map<String, String> variables = variables(language, podcastId);
        variables.put("partial_summaries", String.join("\n\n", partialSummaries));
        return messages(render(properties.finalSummary(), variables), variables);
    }

    private List<OpenRouterMessage> messages(String userPrompt, Map<String, String> variables) {
        return List.of(
                new OpenRouterMessage("system", render(properties.system(), variables)),
                new OpenRouterMessage("user", userPrompt)
        );
    }

    private Map<String, String> variables(String language, UUID podcastId) {
        return new java.util.HashMap<>(Map.of(
                "language", language == null ? "" : language,
                "podcast_id", podcastId == null ? "" : podcastId.toString()
        ));
    }

    private String render(String template, Map<String, String> variables) {
        String rendered = template == null ? "" : template;
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            rendered = rendered.replace("{" + variable.getKey() + "}", variable.getValue());
        }
        return rendered.trim();
    }
}
