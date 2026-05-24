# Формат сообщений Kafka

## `podcast.user.register`

```json
{
  "user_id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "testuser"
}
```

## `media.upload`

`media.upload` использует общий envelope и разные payload-ветки по `object_type`. Для аудио и изображений набор обязательных полей различается.

### Загрузка аудиофайла подкаста

```json
{
  "object_type": "podcast_file_url",
  "object_id": "550e8400-e29b-41d4-a716-446655440000",
  "event": "uploaded",
  "audio_url_file": "https://storage.example.local/media/podcasts/source-file.mp3",
  "audio_file_size": 12345678,
  "duration_seconds": 2580,
  "timestamp": "2026-03-22T12:35:56Z"
}
```

| Поле | Обязательность | Назначение |
|---|---:|---|
| `audio_url_file` | да | путь или URL исходного аудиофайла |
| `audio_file_size` | да | размер исходного файла в байтах; сохраняется в `podcasts.audio_size_file` |
| `duration_seconds` | да | длительность аудиофайла в секундах; сохраняется в `podcasts.duration_seconds` |

### Загрузка изображения

```json
{
  "object_type": "podcast_cover_url",
  "object_id": "550e8400-e29b-41d4-a716-446655440000",
  "image_url": "https://storage.example.local/media/covers/podcast-cover.jpg",
  "timestamp": "2026-03-22T12:35:56Z"
}
```

Для `podcast_cover_url`, `avatar` и `playlist` поле `image_url` используется как путь к загруженному изображению. Поле `event` для успешной загрузки изображения опционально: если оно отсутствует, событие трактуется как `uploaded`. Аудио-поля `audio_url_file`, `audio_file_size` и `duration_seconds` для этих `object_type` не требуются.

Для ошибок поддерживается базовый контракт:

```json
{
  "podcast_id": "550e8400-e29b-41d4-a716-446655440000",
  "error": "upload failed",
  "timestamp": "2026-03-22T12:35:56Z"
}
```

## `media.worker`

```json
{
  "object_type": "podcast_file_url",
  "object_id": "550e8400-e29b-41d4-a716-446655440000",
  "event": "processed",
  "audio_url": "https://cdn.example.local/hls/podcast-id/master.m3u8",
  "timestamp": "2026-03-22T12:35:56Z"
}
```

## `media.subtitle`

```json
{
  "podcast_id": "550e8400-e29b-41d4-a716-446655440000",
  "content": {
    "vtt_object_key": "media/uuid/subtitles.vtt",
    "srt_object_key": "media/uuid/subtitles.srt"
  },
  "ready_at": "2026-03-22T12:35:56Z"
}
```

## `tts.start`

```json
{
  "podcast_id": "550e8400-e29b-41d4-a716-446655440000",
  "content": "Текст, на основе которого TTS генерирует аудиофайл",
  "timestamp": "2026-03-22T12:35:56Z"
}
```

## Общие поля

| Поле | Тип | Описание |
|---|---|---|
| `object_type` | enum | `playlist`, `podcast_file_url`, `podcast_cover_url`, `avatar` |
| `object_id` | uuid | идентификатор целевой записи |
| `event` | enum | событие внутри topic; обязательно для аудио lifecycle, опционально для успешной загрузки изображений |
| `podcast_id` | uuid | идентификатор подкаста для subtitle/TTS/error contracts |
| `timestamp` | datetime | время события producer-а |

`audio_file_size` из Kafka-контракта сохраняется в поле `podcasts.audio_size_file`, `duration_seconds` сохраняется в `podcasts.duration_seconds`.
