package podcastService.transcript.summary;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

record OpenRouterChatRequest(
        String model,
        List<OpenRouterMessage> messages,
        BigDecimal temperature,
        @JsonProperty("max_tokens")
        int maxTokens
) {
}
