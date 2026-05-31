# Kafka-продюсеры

## Внешние продюсеры

| Producer | Topic | Назначение |
|---|---|---|
| `auth-service` | `podcast.user.register` | передаёт регистрацию пользователя для локального `user_profiles` |
| media upload service | `media.upload` | передаёт метаданные загрузки файлов, обложек и аватаров |
| media worker | `media.worker` | передаёт статусы обработки аудиофайла |
| subtitles/STT service | `media.subtitle` | передаёт результат генерации субтитров |
| TTS service | `tts.start` | передаёт текст TTS-flow |
| TTS service | `tts.failed` | передаёт ошибку TTS-flow |

## Внутренние продюсеры

Доменные операции микросервиса подкастов не публикуют бизнес-события в Kafka. Единственная исходящая запись выполняется инфраструктурным `DeadLetterPublishingRecoverer` при отправке необработанных сообщений в DLT.

## Producer DLT

`DeadLetterPublishingRecoverer` публикует сообщения в topic `<sourceTopic><PODCAST_KAFKA_DLT_SUFFIX>` после исчерпания политики повторных попыток или при неретрайных ошибках.
