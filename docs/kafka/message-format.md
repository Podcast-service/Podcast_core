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
  "timestamp": "2026-03-22T12:35:56Z"
}
```

| Поле | Обязательность | Назначение |
|---|---:|---|
| `audio_url_file` | да | путь или URL исходного аудиофайла |

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

Consumer принимает только каноничные значения контракта. Неконтрактные `object_type`, `event` и имена полей считаются невалидным сообщением, логируются и не изменяют состояние БД.

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
  "duration_seconds": "2580",
  "audio_file_size": "11232332",
  "timestamp": "2026-03-22T12:35:56Z"
}
```

| Поле | Обязательность | Назначение |
|---|---:|---|
| `audio_url` | да | HLS/processed URL, сохраняется в `podcasts.audio_url` |
| `duration_seconds` | да | длительность обработанного аудио в секундах, сохраняется в `podcasts.duration_seconds` |
| `audio_file_size` | да | размер обработанного аудиофайла в байтах, сохраняется в `podcasts.audio_size_file` |

Для `media.worker` поддерживаются только события `start_processing`, `processed`, `processing_failed` и `error`. `duration_seconds` и `audio_file_size` принимаются как JSON number или строка с целым числом. Дробные значения не входят в контракт и отклоняются как невалидное сообщение.

## `media.subtitle`

```json
{
  "podcast_id": "550e8400-e29b-41d4-a716-446655440000",
  "content": {
    "vtt_object_key": "https://storage.example/media/uuid/subtitles.vtt",
    "srt_object_key": "https://storage.example/media/uuid/subtitles.srt"
  },
  "ready_at": "2026-03-22T12:35:56Z"
}
```

`vtt_object_key` обязателен. Consumer скачивает VTT по абсолютному HTTP(S) URL или относительному object key через `PODCAST_SUBTITLE_STORAGE_BASE_URL`, последовательно объединяет соседние cue одного спикера и сохраняет в `podcast_transcripts.content` компактный JSON-массив:

```json
[{"text":"Первая реплика. Продолжение.","voice":"speaker_00"},{"text":"Ответ.","voice":"speaker_01"}]
```

`GET /podcasts/{podcastId}/transcript` возвращает сохранённый массив без обращения к subtitle storage. Summary generation также извлекает текст из сохранённых блоков без повторного чтения VTT.

## `tts.start`

```json
{
  "podcast_id": "550e8400-e29b-41d4-a716-446655440000",
  "content": [
    {
      "text": "Привет, как у тебя дела?",
      "voice": "aidar"
    },
    {
      "text": "У меня всё хорошо, а у тебя?",
      "voice": "kseniya"
    }
  ],
  "timestamp": "2026-03-22T12:35:56Z"
}
```

`content` сохраняется в `podcast_transcripts.content`. Значение может быть строкой, JSON-объектом или JSON-массивом реплик. Строка сохраняется как текст, объект или массив сохраняется компактной JSON-строкой. После успешной обработки события подкаст переводится в `UPLOADING`, так как дальнейший аудиофайл создаётся TTS-flow и приходит через media pipeline.

## `tts.failed`

```json
{
  "object_type": "podcast_file_url",
  "object_id": "550e8400-e29b-41d4-a716-446655440000",
  "event": "error",
  "error": "processing failed",
  "timestamp": "2026-03-22T12:35:56Z"
}
```

`object_id` указывает на подкаст. При корректном сообщении статус подкаста переводится в `FAILED`, кроме случаев, когда media lifecycle уже завершён статусом `PROCESSED`, `PUBLISHED` или `ARCHIVED`.

## Общие поля

| Поле | Тип | Описание |
|---|---|---|
| `object_type` | enum | `playlist`, `podcast_file_url`, `podcast_cover_url`, `avatar` |
| `object_id` | uuid | идентификатор целевой записи |
| `event` | enum | событие внутри topic; обязательно для аудио lifecycle, опционально для успешной загрузки изображений |
| `podcast_id` | uuid | идентификатор подкаста для subtitle/TTS/error contracts |
| `timestamp` | datetime | время события producer-а |

`duration_seconds` и `audio_file_size` приходят из `media.worker` после обработки файла и сохраняются в `podcasts.duration_seconds` и `podcasts.audio_size_file`.

## Recommendation outbox envelope

Recommendation MVP события сначала сохраняются в `outbox_events.payload` как общий envelope при `PODCAST_RECOMMENDATION_EVENTS_ENABLED=true`. Kafka-публикация выполняется только через outbox publisher и только при `PODCAST_OUTBOX_PUBLISHER_ENABLED=true` вместе с `PODCAST_KAFKA_PRODUCER_ENABLED=true`:

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "podcast.liked.v1",
  "eventVersion": 1,
  "producer": "podcast-core",
  "occurredAt": "2026-06-01T10:15:30Z",
  "correlationId": null,
  "causationId": null,
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "payload": {
    "podcastId": "550e8400-e29b-41d4-a716-446655440002",
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "likedAt": "2026-06-01T10:15:30Z"
  }
}
```

Все recommendation event types версионированы суффиксом `.v1`. Поля `correlationId` и `causationId` остаются `null`, пока в Podcast Core нет общего request/correlation context.

Полный payload contract, topics, keys и rollout: [../recommendation-events.md](../recommendation-events.md).
