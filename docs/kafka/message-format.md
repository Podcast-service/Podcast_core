# Формат сообщений Kafka

## `podcast.user.register`

```json
{
  "user_id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "testuser"
}
```

## `media.upload`

```json
{
  "object_type": "podcast_file_url",
  "object_id": "550e8400-e29b-41d4-a716-446655440000",
  "event": "uploaded",
  "audio_url_file": "https://storage.example.local/media/podcasts/source-file.mp3",
  "audio_file_size": 12345678,
  "timestamp": "2026-03-22T12:35:56Z"
}
```

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
| `event` | enum | событие внутри topic |
| `podcast_id` | uuid | идентификатор подкаста для subtitle/TTS/error contracts |
| `timestamp` | datetime | время события producer-а |

`audio_file_size` из Kafka-контракта сохраняется в поле `podcasts.audio_size_file`.
