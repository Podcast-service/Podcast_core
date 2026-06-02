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

Доменные операции микросервиса подкастов не публикуют бизнес-события напрямую в Kafka. Для recommendation MVP событий Podcast Core записывает события в `outbox_events`, если включён `PODCAST_RECOMMENDATION_EVENTS_ENABLED=true`.

Outbox publisher выключен по умолчанию и отправляет события только если одновременно включены `PODCAST_OUTBOX_PUBLISHER_ENABLED=true` и `PODCAST_KAFKA_PRODUCER_ENABLED=true`. Publisher в короткой транзакции выбирает `NEW`/`FAILED` записи через `FOR UPDATE SKIP LOCKED`, коммитит их в `PROCESSING`, отправляет Kafka messages вне DB-транзакции и отдельными короткими транзакциями переводит rows в `SENT` или `FAILED`. Отправленные rows не удаляются. Stale `PROCESSING` rows автоматически восстанавливаются в `FAILED`.

| Event type | Trigger | aggregate_type | event_key |
|---|---|---|---|
| `podcast.published.v1` | публикация подкаста | `PODCAST` | `podcastId` |
| `podcast.updated.v1` | обновление опубликованного подкаста с категорией | `PODCAST` | `podcastId` |
| `podcast.deleted.v1` | архивирование или tombstone после очистки категории | `PODCAST` | `podcastId` |
| `podcast.play_finished.v1` | переход listen history в completed | `USER_ACTIVITY` | `userId` |
| `podcast.liked.v1` | новый LIKE или переход DISLIKE -> LIKE | `USER_ACTIVITY` | `userId` |
| `podcast.disliked.v1` | новый DISLIKE или переход LIKE -> DISLIKE | `USER_ACTIVITY` | `userId` |
| `author.followed.v1` | новая подписка на автора | `AUTHOR` | `authorId` |
| `author.unfollowed.v1` | удаление существующей подписки | `AUTHOR` | `authorId` |
| `playlist.created.v1` | создание плейлиста | `PLAYLIST` | `playlistId` |
| `playlist.updated.v1` | обновление метаданных или состава плейлиста | `PLAYLIST` | `playlistId` |
| `playlist.deleted.v1` | удаление плейлиста | `PLAYLIST` | `playlistId` |

`podcast.deleted.v1` привязан к существующему `archive` use case, потому что hard delete подкастов в текущем публичном API отсутствует. Он также используется как recommendation tombstone, если опубликованный подкаст теряет обязательную для Recommendation Service категорию.

### Routing recommendation events

| Event types | Topic |
|---|---|
| `podcast.play_finished.v1`, `podcast.liked.v1`, `podcast.disliked.v1`, `author.followed.v1`, `author.unfollowed.v1` | `podcast.activity.events.v1` |
| `podcast.published.v1`, `podcast.updated.v1`, `podcast.deleted.v1`, `playlist.created.v1`, `playlist.updated.v1`, `playlist.deleted.v1` | `podcast.content.events.v1` |
| future `podcast.search.*.v1` | `podcast.search.events.v1` |

Неконтрактные event types не отправляются в произвольный topic: outbox publisher помечает такую запись как `FAILED`, чтобы ошибка была видна через retry metadata.

Полный контракт, flags, retry flow и SQL для эксплуатации: [../recommendation-events.md](../recommendation-events.md).

## Producer DLT

`DeadLetterPublishingRecoverer` публикует сообщения в topic `<sourceTopic><PODCAST_KAFKA_DLT_SUFFIX>` после исчерпания политики повторных попыток или при неретрайных ошибках.
