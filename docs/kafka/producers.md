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

Доменные операции микросервиса подкастов не публикуют бизнес-события напрямую в Kafka. Для recommendation MVP событий Podcast Core только записывает события в `outbox_events`, если включён `PODCAST_RECOMMENDATION_EVENTS_ENABLED=true`; отдельный Kafka publisher в текущем этапе не добавлен.

| Event type | Trigger | aggregate_type | event_key |
|---|---|---|---|
| `podcast.published.v1` | публикация подкаста | `PODCAST` | `podcastId` |
| `podcast.updated.v1` | обновление подкаста | `PODCAST` | `podcastId` |
| `podcast.deleted.v1` | архивирование подкаста для исключения из рекомендаций | `PODCAST` | `podcastId` |
| `podcast.play_finished.v1` | переход listen history в completed | `USER_ACTIVITY` | `userId` |
| `podcast.liked.v1` | новый LIKE или переход DISLIKE -> LIKE | `USER_ACTIVITY` | `userId` |
| `podcast.disliked.v1` | новый DISLIKE или переход LIKE -> DISLIKE | `USER_ACTIVITY` | `userId` |
| `author.followed.v1` | новая подписка на автора | `AUTHOR` | `authorId` |
| `author.unfollowed.v1` | удаление существующей подписки | `AUTHOR` | `authorId` |
| `playlist.created.v1` | создание плейлиста | `PLAYLIST` | `playlistId` |
| `playlist.updated.v1` | обновление плейлиста | `PLAYLIST` | `playlistId` |
| `playlist.deleted.v1` | удаление плейлиста | `PLAYLIST` | `playlistId` |

`podcast.deleted.v1` привязан к существующему `archive` use case, потому что hard delete подкастов в текущем публичном API отсутствует.

Единственная исходящая Kafka-запись сейчас выполняется инфраструктурным `DeadLetterPublishingRecoverer` при отправке необработанных сообщений в DLT.

## Producer DLT

`DeadLetterPublishingRecoverer` публикует сообщения в topic `<sourceTopic><PODCAST_KAFKA_DLT_SUFFIX>` после исчерпания политики повторных попыток или при неретрайных ошибках.
