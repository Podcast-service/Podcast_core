package podcastService.transcript.summary;

import java.util.List;

record OpenRouterChatResponse(
        List<Choice> choices
) {
    record Choice(Message message) {
    }

    record Message(String content) {
    }
}
