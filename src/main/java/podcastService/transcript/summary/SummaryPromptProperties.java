package podcastService.transcript.summary;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.summary.prompts")
public record SummaryPromptProperties(
        String system,
        String directSummary,
        String chunkSummary,
        String finalSummary
) {
    public SummaryPromptProperties {
        if (system == null || system.isBlank()) {
            system = """
                    Ты сервис генерации summary для подкастов.
                    Отвечай на языке: {language}.
                    Не добавляй факты, которых нет в transcript.
                    Не упоминай, что текст был сгенерирован моделью.
                    Не используй markdown.
                    """;
        }
        if (directSummary == null || directSummary.isBlank()) {
            directSummary = """
                    Сделай краткое summary этого transcript для карточки подкаста.
                    Summary должно быть 3-6 предложений.
                    Сохрани основную тему, ключевые идеи и полезный контекст.
                    Не добавляй факты, которых нет в transcript.

                    Transcript:
                    {transcript}
                    """;
        }
        if (chunkSummary == null || chunkSummary.isBlank()) {
            chunkSummary = """
                    Сделай краткое промежуточное summary этого фрагмента transcript.
                    Сохрани только важные факты, темы, выводы и контекст.
                    Игнорируй повторы, filler words, междометия и технический мусор.
                    Не добавляй факты, которых нет во фрагменте.

                    Fragment:
                    {chunk}
                    """;
        }
        if (finalSummary == null || finalSummary.isBlank()) {
            finalSummary = """
                    Ниже даны промежуточные summary частей одного podcast transcript.
                    Объедини их в одно итоговое summary для карточки подкаста.
                    Итог должен быть 3-6 предложений.
                    Не используй markdown.
                    Не добавляй новых фактов.
                    Не упоминай, что summary было сгенерировано по частям.

                    Partial summaries:
                    {partial_summaries}
                    """;
        }
    }
}
