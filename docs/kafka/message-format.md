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
  "duration_seconds": 2580,
  "timestamp": "2026-03-22T12:35:56Z"
}
```

| Поле | Обязательность | Назначение |
|---|---:|---|
| `audio_url_file` | да | путь или URL исходного аудиофайла |
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

Для `podcast_cover_url`, `avatar` и `playlist` поле `image_url` используется как путь к загруженному изображению. Поле `event` для успешной загрузки изображения опционально: если оно отсутствует, событие трактуется как `uploaded`. Аудио-поля `audio_url_file` и `duration_seconds` для этих `object_type` не требуются.

Consumer принимает каноничные значения контракта и совместимые алиасы от соседних media-сервисов:

| Каноничное поле или значение | Совместимые алиасы |
|---|---|
| `object_type=podcast_file_url` | `podcast_file`, `podcast_audio`, `audio` |
| `object_type=podcast_cover_url` | `podcast_cover`, `cover` |
| `object_type=playlist` | `playlists` |
| `event=start_upload` | `upload_started`, `uploading` |
| `event=uploaded` | `upload_complete`, `upload_completed` |
| `audio_url_file` | `audio_file_url`, `audioUrlFile` |
| `duration_seconds` | `durationSeconds`, `duration`, `audio_duration_seconds` |

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

Для `media.worker` поддерживаются совместимые алиасы:

| Каноничное поле или значение | Совместимые алиасы |
|---|---|
| `object_type=podcast_file_url` | `podcast_file`, `podcast_audio`, `audio` |
| `event=start_processing` | `processing_started`, `processing` |
| `event=processed` | `processing_done`, `processing_completed`, `completed`, `done` |
| `event=processing_failed` | `failed` |
| `audio_url` | `audioUrl`, `hls_url`, `hlsUrl` |

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

Поле `content` сохраняется в `podcast_transcripts.content` в исходном виде. Если producer присылает строку, сохраняется строка. Если producer присылает JSON-объект или массив, сохраняется компактная JSON-строка.

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

`duration_seconds` из Kafka-контракта сохраняется в поле `podcasts.duration_seconds`.
