# Kafka

Kafka используется для входящих событий от внешних сервисов. Микросервис подкастов читает raw JSON, десериализует его в typed DTO, валидирует контракт и передаёт событие в handler registry или topic-specific service.

## Роли сервисов

| Сервис | Роль |
|---|---|
| `auth-service` | producer topic `podcast.user.register` |
| media upload service | producer topic `media.upload` |
| media worker | producer topic `media.worker` |
| subtitles/STT service | producer topic `media.subtitle` |
| TTS service | producer topics `tts.start`, `tts.failed` |
| `podcast-core` | consumer всех входящих topics |
| `podcast-core` error handler | producer в DLT topics |

## Runtime настройки

| Настройка | Значение по умолчанию |
|---|---|
| Bootstrap servers | `PODCAST_KAFKA_BOOTSTRAP_SERVERS=localhost:9092` |
| Consumer group | `PODCAST_KAFKA_CONSUMER_GROUP=podcast-service` |
| User register topic | `PODCAST_KAFKA_TOPIC_USER_REGISTER=podcast.user.register` |
| Media upload topic | `PODCAST_KAFKA_TOPIC_MEDIA_UPLOAD=media.upload` |
| Media worker topic | `PODCAST_KAFKA_TOPIC_MEDIA_WORKER=media.worker` |
| Subtitle topic | `PODCAST_KAFKA_TOPIC_MEDIA_SUBTITLE=media.subtitle` |
| TTS start topic | `PODCAST_KAFKA_TOPIC_TTS_START=tts.start` |
| TTS failed topic | `PODCAST_KAFKA_TOPIC_TTS_FAILED=tts.failed` |
| Retry backoff | `PODCAST_KAFKA_RETRY_BACKOFF_MS=1000` |
| Retry attempts | `PODCAST_KAFKA_RETRY_MAX_ATTEMPTS=3` |
| DLT suffix | `PODCAST_KAFKA_DLT_SUFFIX=.DLT` |

## Consumer flow

1. Kafka listener получает raw JSON.
2. `KafkaMessageReader` десериализует payload в DTO конкретного topic.
3. DTO содержит enum-поля для `object_type` и `event`.
4. `media.upload` и `media.worker` маршрутизируются по ключу `(object_type,event)`.
5. Handler вызывает доменный service.
6. Service обновляет PostgreSQL внутри транзакции.
7. Offset коммитится после успешного завершения listener.

Подробности форматов: [message-format.md](message-format.md).
