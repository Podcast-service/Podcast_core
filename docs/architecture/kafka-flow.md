# Поток Kafka-сообщений

## Регистрация пользователя

`auth-service` публикует raw JSON в topic `podcast.user.register`. Микросервис подкастов создаёт или обновляет локальный `user_profiles`.

## Media lifecycle

Файлы через Kafka не передаются. Kafka содержит только метаданные загрузки, обработки, subtitle и TTS-flow.

| Topic | Назначение |
|---|---|
| `media.upload` | загрузка исходного файла, обложки, аватара или плейлиста |
| `media.worker` | обработка аудиофайла подкаста |
| `media.subtitle` | результат генерации субтитров |
| `tts.start` | текст, из которого TTS генерирует аудио |
| `tts.failed` | ошибка TTS-flow |

## Статусы подкаста

| Событие | Поведение |
|---|---|
| `media.upload/start_upload` | `DRAFT` переходит в `UPLOADING` |
| `media.upload/uploaded` | сохраняется `audio_url_file`; статус `UPLOADED` |
| `media.worker/start_processing` | статус `PROCESSING` |
| `media.worker/processed` | сохраняется processed/HLS `audio_url`; статус `PROCESSED` |
| error event | статус `FAILED` |

`start_upload` после `PROCESSED` не откатывает статус назад. `processed` после `processed` безопасен. Опубликованные и архивные подкасты не переводятся в media lifecycle статусы.

Для `media.upload` контракт разделён по `object_type`: аудио использует `audio_url_file` и обязательный `event`; изображения используют `image_url`, а отсутствие `event` трактуется как успешная загрузка. Длительность и размер аудиофайла приходят после обработки в `media.worker/processed`.

## Transcript storage

`media.subtitle` и `tts.start` сохраняют данные в `podcast_transcripts.content`. Для subtitle в `content` сохраняется JSON с ключами `vtt_object_key`, `srt_object_key`, `ready_at`; это ссылки на subtitle objects в storage, а не сам текст transcript. Для генерации summary Podcast Core читает `.srt`/`.vtt` объект через `PODCAST_SUBTITLE_STORAGE_BASE_URL` и очищает subtitle-разметку. Для TTS сохраняется строка, JSON-объект или JSON-массив из Kafka payload. `tts.start` также переводит подкаст в `UPLOADING`, а `tts.failed` переводит подкаст в `FAILED` через общий media lifecycle.
