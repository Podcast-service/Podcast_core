# Kafka

Kafka используется для входящих событий от внешних сервисов. Микросервис подкастов читает сообщения как raw JSON, валидирует базовые поля, маршрутизирует событие в handler registry и применяет изменения в PostgreSQL внутри транзакции.

## Роли сервисов

| Сервис | Роль |
|---|---|
| `auth-service` | producer topic `podcast.user.register` |
| media-service | producer topic `media` |
| `podcast-core` | consumer topics `podcast.user.register`, `media` |
| `podcast-core` error handler | producer в DLT topics |

## Runtime настройки

| Настройка | Значение по умолчанию |
|---|---|
| Bootstrap servers | `PODCAST_KAFKA_BOOTSTRAP_SERVERS=localhost:9092` |
| Consumer group | `PODCAST_KAFKA_CONSUMER_GROUP=podcast-service` |
| User register topic | `PODCAST_KAFKA_TOPIC_USER_REGISTER=podcast.user.register` |
| Media topic | `PODCAST_KAFKA_TOPIC_MEDIA=media` |
| Retry backoff | `PODCAST_KAFKA_RETRY_BACKOFF_MS=1000` |
| Retry attempts | `PODCAST_KAFKA_RETRY_MAX_ATTEMPTS=3` |
| DLT suffix | `PODCAST_KAFKA_DLT_SUFFIX=.DLT` |

## Consumer flow

1. Kafka listener получает raw JSON message.
2. `KafkaJsonMessageParser` преобразует строку в `JsonNode`.
3. Parser конкретного topic валидирует обязательные поля.
4. Для `media` используется registry по ключу `(type,event)`.
5. Handler вызывает доменный сервис.
6. Доменный сервис пишет изменения в PostgreSQL в транзакции.
7. Offset коммитится после успешного завершения listener.

Подробности по форматам: [message-format.md](message-format.md).
