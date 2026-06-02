# Kafka-топики

| Topic | Направление | Назначение |
|---|---|---|
| `podcast.user.register` | входящий | создание или обновление локального `user_profiles` |
| `media.upload` | входящий | начало загрузки, успешная загрузка и ошибки загрузки |
| `media.worker` | входящий | начало обработки, завершение обработки и ошибки обработки |
| `media.subtitle` | входящий | загрузка VTT и сохранение объединённых subtitle-блоков в `podcast_transcripts.content` |
| `tts.start` | входящий | сохранение TTS `content` в `podcast_transcripts.content` и перевод подкаста в `UPLOADING` |
| `tts.failed` | входящий | фиксация ошибки TTS-flow и перевод подкаста в `FAILED` |
| `podcast.activity.events.v1` | исходящий | recommendation activity events из `outbox_events` |
| `podcast.content.events.v1` | исходящий | recommendation content events из `outbox_events` |
| `podcast.search.events.v1` | исходящий | зарезервирован для будущих search events |
| `<source>.DLT` | исходящий | сообщения, не обработанные основным consumer |
| `podcast.activity.events.v1.DLT` | исходящий, зарезервирован | DLT имя для activity events; outbox publisher на текущем этапе туда не пишет |
| `podcast.content.events.v1.DLT` | исходящий, зарезервирован | DLT имя для content events; outbox publisher на текущем этапе туда не пишет |
| `podcast.search.events.v1.DLT` | исходящий, зарезервирован | DLT имя для search events; outbox publisher на текущем этапе туда не пишет |

## Рекомендуемые ключи сообщений

| Topic | Kafka key |
|---|---|
| `podcast.user.register` | `user_id` |
| `media.upload` | `object_id` |
| `media.worker` | `object_id` |
| `media.subtitle` | `podcast_id` |
| `tts.start` | `podcast_id` |
| `tts.failed` | `object_id` |
| `podcast.activity.events.v1` | `userId` |
| `podcast.content.events.v1` | `podcastId` или `playlistId` |
| `podcast.search.events.v1` | зависит от будущего search event |

`podcast-core` использует group id из `PODCAST_KAFKA_CONSUMER_GROUP`, по умолчанию `podcast-service`.

> Требует уточнения: production количество partitions, replication factor и retention для каждого topic.
