# Kafka

Kafka используется для синхронизации пользователей из `auth-service` в `podcast-core`. В коде также есть общая инфраструктура producer/routing, но фактический доменный producer сейчас не вызывается сервисами.

## Топики

| Логическое имя | Переменная | Значение по умолчанию | Назначение |
|---|---|---|---|
| `users` | `KAFKA_TOPIC_USERS` | `podcasts.users` | События пользователей из `auth-service` |
| `podcasts` | `KAFKA_TOPIC_PODCASTS` | `podcasts.podcasts` | Зарезервировано конфигом; текущий код не публикует доменные события подкастов |

DLT создаётся по правилу `<source-topic>.DLT`, например `podcasts.users.DLT`.

## Consumer

| Consumer | Топик | Group id | Что делает |
|---|---|---|---|
| `UserProfileConsumer` | `${app.kafka.topics.users}` | `${spring.kafka.consumer.group-id}` | Принимает `EventEnvelope`, делегирует в `UserProfileEventHandler` |

## Producer

| Компонент | Статус |
|---|---|
| `KafkaEventPublisher` | Обёртка над `KafkaTemplate<String, EventEnvelope>` |
| `KafkaDomainEventProducer` | Создаёт envelope и выбирает topic через routing registry |
| Фактические вызовы из доменных сервисов | В текущем коде не обнаружены |

> Требует уточнения: набор исходящих событий `podcast-core` для публикации подкастов, голосов, подписок и обновлений профиля.

## Формат сообщения

Все события читаются как `EventEnvelope`.

```json
{
  "eventType": "user.created",
  "occurredAt": "2026-05-20T10:00:00Z",
  "payload": {
    "userId": "00000000-0000-0000-0000-000000000001",
    "username": "demo_user"
  }
}
```

| Поле | Тип | Обяз. | Проверка |
|---|---|---:|---|
| `eventType` | enum/string | да | Сейчас поддерживается только `user.created` |
| `occurredAt` | date-time | да | Значение присутствует |
| `payload` | object | да | Значение присутствует |

Payload `user.created` преобразуется в `CreateUserRequest`.

| Поле | Тип | Обяз. |
|---|---|---:|
| `userId` | uuid | да |
| `username` | string | да, not blank |

## Обработка `user.created`

1. `UserProfileConsumer` получает envelope.
2. `UserProfileEventHandler` проверяет envelope.
3. Payload конвертируется через Jackson `ObjectMapper`.
4. Проверяются `userId` и `username`.
5. `UserProfileService.createNewUser()` создаёт профиль пользователя.
6. Успешная обработка логируется на `INFO`.

Профиль автора через Kafka не создаётся. Авторский профиль создаётся REST-ручкой `POST /authors/me`, когда у пользователя уже есть user profile и роль `author`.

## Retry и DLT

`KafkaErrorHandlerConfig` использует `DefaultErrorHandler`:

| Параметр | Значение |
|---|---|
| Backoff | `FixedBackOff(1000 ms, 3 attempts)` |
| DLT | `<topic>.DLT` в той же partition |
| Retry logging | Каждая попытка логируется через `KafkaExceptionLogger` |
| DLT logging | Отправка в DLT логируется |

Не retryable exceptions:

- `InvalidKafkaMessageException`
- `KafkaDeserializationException`
- `KafkaMessageValidationException`
- `IllegalArgumentException`

Остальные ошибки будут ретраиться по backoff-настройке и затем уйдут в DLT.

## Возможные ошибки

| Ошибка | Причина | Поведение |
|---|---|---|
| `InvalidKafkaMessageException` | `null` envelope, пустой payload, неизвестный event type, пустой username | Без retry, отправка в DLT |
| `KafkaDeserializationException` | Payload не конвертируется в ожидаемый DTO | Без retry, отправка в DLT |
| `KafkaMessageValidationException` | Зарезервировано для validation ошибок | Без retry, отправка в DLT |
| `KafkaRetryableProcessingException` | Зарезервировано для временных ошибок | Retry, затем DLT |
| `DataIntegrityViolationException` | Дубликат или нарушение constraints при записи | Retry по умолчанию, затем DLT |

## Настройки consumer/producer

Consumer:

- key/value deserializer: `ErrorHandlingDeserializer`;
- value delegate: `JsonDeserializer`;
- `spring.json.use.type.headers=false`;
- default value type: `podcastService.infrastructure.messaging.event.EventEnvelope`;
- trusted packages: `podcastService.infrastructure.messaging.event,podcastService.user.dto`.

Producer:

- key serializer: `StringSerializer`;
- value serializer: `JsonSerializer`.

## Интеграция с auth-service

- `auth-service` публикует событие именно в `podcasts.users` или значение `KAFKA_TOPIC_USERS` синхронизировано в обоих сервисах.
- `eventType` равен `user.created`.
- `payload.userId` совпадает с JWT claim `user_id`, который потом приходит в `podcast-core`.
- `payload.username` проходит ограничения БД и сервиса.
- DLT topic существует или auto-create topics включён.

> Требует уточнения: идемпотентная политика для повторного `user.created` при конфликте уникальных ограничений.
