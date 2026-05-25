# Конфигурация Kafka

| Настройка | Значение |
|---|---|
| Bootstrap servers | `PODCAST_KAFKA_BOOTSTRAP_SERVERS` |
| Consumer group | `PODCAST_KAFKA_CONSUMER_GROUP` |
| User registration topic | `PODCAST_KAFKA_TOPIC_USER_REGISTER` |
| Media upload topic | `PODCAST_KAFKA_TOPIC_MEDIA_UPLOAD` |
| Media worker topic | `PODCAST_KAFKA_TOPIC_MEDIA_WORKER` |
| Subtitle topic | `PODCAST_KAFKA_TOPIC_MEDIA_SUBTITLE` |
| TTS topic | `PODCAST_KAFKA_TOPIC_TTS_START` |
| Consumer auto commit | disabled |
| Listener ack mode | `record` |
| Deserializer | `ErrorHandlingDeserializer` + `StringDeserializer` |
| Producer value serializer | `StringSerializer` |
| Retry backoff | `PODCAST_KAFKA_RETRY_BACKOFF_MS` |
| Retry attempts | `PODCAST_KAFKA_RETRY_MAX_ATTEMPTS` |
| DLT suffix | `PODCAST_KAFKA_DLT_SUFFIX` |

Kafka UI для local/dev:

```text
http://localhost:8081
```
