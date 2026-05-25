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

## Статусы подкаста

| Событие | Поведение |
|---|---|
| `media.upload/start_upload` | `DRAFT` переходит в `UPLOADING` |
| `media.upload/uploaded` | сохраняются `audio_url_file`, `audio_size_file`, `duration_seconds`; статус `UPLOADED` |
| `media.worker/start_processing` | статус `PROCESSING` |
| `media.worker/processed` | сохраняется processed/HLS `audio_url`; статус `PROCESSED` |
| error event | статус `FAILED` |

`start_upload` после `PROCESSED` не откатывает статус назад. `processed` после `processed` безопасен. Опубликованные и архивные подкасты не переводятся в media lifecycle статусы.

Для `media.upload` контракт разделён по `object_type`: аудио использует `audio_url_file`, `audio_file_size`, `duration_seconds` и обязательный `event`; изображения используют `image_url`, а отсутствие `event` трактуется как успешная загрузка.

## Transcript storage

`media.subtitle` и `tts.start` сохраняют данные в `podcast_transcripts.content`. Для subtitle в `content` сохраняется JSON с ключами `vtt_object_key`, `srt_object_key`, `ready_at`; для TTS сохраняется текст из Kafka payload.
