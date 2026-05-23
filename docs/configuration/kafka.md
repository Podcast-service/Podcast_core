# Конфигурация Kafka

| Настройка | Значение |
|---|---|
| Bootstrap servers | `PODCAST_KAFKA_BOOTSTRAP_SERVERS` |
| Consumer group | `PODCAST_KAFKA_CONSUMER_GROUP` |
| Users topic | `PODCAST_KAFKA_TOPIC_USERS` |
| Deserializer | `ErrorHandlingDeserializer` + `JsonDeserializer` |
| Default value type | `EventEnvelope` |
| Trusted packages | `podcastService.infrastructure.messaging.event,podcastService.user.dto` |
| Producer value serializer | `JsonSerializer` |

Kafka UI для local/dev:

```text
http://localhost:8081
```
