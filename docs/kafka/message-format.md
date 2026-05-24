# Формат сообщений Kafka

## `podcast.user.register`

```json
{
  "user_id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "testuser"
}
```

| Поле | Тип | Обязательность | Описание |
|---|---|---|---|
| `user_id` | uuid | да | внешний идентификатор пользователя из auth-service |
| `username` | string | да | username локального профиля |

## `media`

### `start_upload`

```json
{
  "type": "podcast_file",
  "event": "start_upload",
  "object_id": "550e8400-e29b-41d4-a716-446655440000",
  "started_at": "2026-03-22T12:34:00Z"
}
```

### `uploaded`

```json
{
  "type": "podcast_file",
  "event": "uploaded",
  "object_id": "550e8400-e29b-41d4-a716-446655440000",
  "audio_url_file": "/media/podcasts/audio.mp3",
  "audio_size_file": "123312",
  "uploaded_at": "2026-03-22T12:34:10Z"
}
```

### `error`

```json
{
  "type": "podcast_file",
  "event": "error",
  "object_id": "550e8400-e29b-41d4-a716-446655440000",
  "error_message": "upload failed",
  "timestamp": "2026-03-22T12:34:20Z"
}
```

| Поле | Тип | Обязательность | Описание |
|---|---|---|---|
| `type` | string | да | `playlists`, `avatar`, `podcast_file`, `podcast_cover` |
| `event` | string | да | `start_upload`, `uploaded`, `error` |
| `object_id` | uuid | да | идентификатор целевой сущности |
| `audio_url_file` | string | для `uploaded` | путь или URL медиа-файла |
| `audio_size_file` | string/integer | нет | размер файла в байтах |
| `error_message` | string | для `error` | текст ошибки от media-service |

Контракт topic `media` допускает появление новых полей. Consumer сохраняет raw JSON на уровне обработки и использует только поля, необходимые конкретному handler.

## Headers

Consumer читает следующие headers, если они переданы producer:

| Header | Назначение |
|---|---|
| `correlation_id` / `correlationId` | трассировка сквозного запроса |
| `request_id` | альтернативный request id |
| `message_id` / `messageId` | идентификатор сообщения |

## Ключ сообщения

Код consumer не требует конкретного Kafka key. Для production ordering рекомендуется использовать `user_id` для `podcast.user.register` и `object_id` для `media`.
