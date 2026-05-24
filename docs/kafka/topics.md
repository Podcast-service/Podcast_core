# Kafka-топики

| Topic | Направление | Назначение |
|---|---|---|
| `podcast.user.register` | входящий | создание или обновление локального `user_profiles` |
| `media.upload` | входящий | начало загрузки, успешная загрузка и ошибки загрузки |
| `media.worker` | входящий | начало обработки, завершение обработки и ошибки обработки |
| `media.subtitle` | входящий | сохранение subtitle `content` в `podcast_transcripts.content` |
| `tts.start` | входящий | сохранение TTS `content` в `podcast_transcripts.content` |
| `<source>.DLT` | исходящий | сообщения, не обработанные основным consumer |

## Рекомендуемые ключи сообщений

| Topic | Kafka key |
|---|---|
| `podcast.user.register` | `user_id` |
| `media.upload` | `object_id` |
| `media.worker` | `object_id` |
| `media.subtitle` | `podcast_id` |
| `tts.start` | `podcast_id` |

`podcast-core` использует group id из `PODCAST_KAFKA_CONSUMER_GROUP`, по умолчанию `podcast-service`.

> Требует уточнения: production количество partitions, replication factor и retention для каждого topic.
