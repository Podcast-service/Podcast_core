# Recommendation Events

## Назначение

Podcast Core остаётся владельцем основной доменной модели и не рассчитывает рекомендации. Интеграция с Recommendation Service асинхронная:

```text
domain transaction -> outbox_events -> outbox publisher -> Kafka -> Recommendation Service
```

Outbox row записывается в той же транзакции, что и доменное изменение. Недоступность Kafka или Recommendation Service не блокирует REST-действия Core. Отправленные rows не удаляются.

## Envelope

Все события используют версионированный envelope:

```json
{
  "eventId": "01000000-0000-0000-0000-000000000001",
  "eventType": "podcast.liked.v1",
  "eventVersion": 1,
  "producer": "podcast-core",
  "occurredAt": "2026-06-01T10:15:30Z",
  "correlationId": null,
  "causationId": null,
  "userId": "50000000-0000-0000-0000-000000000001",
  "payload": {}
}
```

`eventId` обязателен и используется Recommendation Service для идемпотентности через `processed_events`. `occurredAt` сериализуется как UTC `Instant`. В Core пока нет общего HTTP request/correlation context, поэтому `correlationId` и `causationId` остаются `null`.

## Topics И Keys

| Topic | Event types | Kafka key |
|---|---|---|
| `podcast.activity.events.v1` | `podcast.play_finished.v1`, `podcast.liked.v1`, `podcast.disliked.v1` | `userId` |
| `podcast.activity.events.v1` | `author.followed.v1`, `author.unfollowed.v1` | `authorId` |
| `podcast.content.events.v1` | `podcast.published.v1`, `podcast.updated.v1`, `podcast.deleted.v1` | `podcastId` |
| `podcast.content.events.v1` | `playlist.created.v1`, `playlist.updated.v1`, `playlist.deleted.v1` | `playlistId` |
| `podcast.search.events.v1` | reserved future `podcast.search.*.v1` | `userId` |

## Payload Contracts

Все новые поля внутри v1 являются optional backward-compatible расширениями. Legacy timestamp-поля сохранены, потому что текущий Recommendation Service использует их.

| Event type | Payload |
|---|---|
| `podcast.published.v1` | `podcastId`, `authorId`, `categoryId`, `title`, `description`, `durationSeconds`, `publishedAt`, `language`, `tags`, `status`, `isExplicit` |
| `podcast.updated.v1` | поля published event и `updatedAt` |
| `podcast.deleted.v1` | `podcastId`, `authorId`, `categoryId`, `deletedAt`, `status=DELETED` |
| `podcast.play_finished.v1` | `podcastId`, `userId`, `authorId`, `categoryId`, `durationSeconds`, `progressSeconds`, `progressPercent`, `source`, `occurredAt`, `finishedAt` |
| `podcast.liked.v1` | `podcastId`, `userId`, `authorId`, `categoryId`, `occurredAt`, `likedAt` |
| `podcast.disliked.v1` | `podcastId`, `userId`, `authorId`, `categoryId`, `occurredAt`, `dislikedAt` |
| `author.followed.v1` | `authorId`, `userId`, `occurredAt`, `followedAt` |
| `author.unfollowed.v1` | `authorId`, `userId`, `occurredAt`, `unfollowedAt` |
| `playlist.created.v1` | `playlistId`, `ownerUserId`, `title`, `description`, `publicPlaylist`, `podcastIds`, `createdAt` |
| `playlist.updated.v1` | поля created event и `updatedAt` |
| `playlist.deleted.v1` | `playlistId`, `ownerUserId`, `deletedAt`, `status=DELETED` |

Для видимости плейлиста стабильное имя payload-поля: `publicPlaylist`. Recommendation Service преобразует его во внутреннюю read-model колонку `visibility`.

Core пока не хранит podcast `language`, `tags` и `isExplicit`, поэтому эти поля сериализуются как `null`. Recommendation Service v1 требует ненулевой `categoryId` для published/updated catalog payload. Core не ломает публикацию podcast без категории: initial recommendation sync пропускается с warn log, а очистка категории у опубликованного podcast отправляет tombstone для исключения snapshot из рекомендаций.

## Examples

### `podcast.published.v1`

```json
{"eventId":"01000000-0000-0000-0000-000000000001","eventType":"podcast.published.v1","eventVersion":1,"producer":"podcast-core","occurredAt":"2026-06-01T10:15:30Z","correlationId":null,"causationId":null,"userId":"50000000-0000-0000-0000-000000000001","payload":{"podcastId":"10000000-0000-0000-0000-000000000001","authorId":"20000000-0000-0000-0000-000000000001","categoryId":"30000000-0000-0000-0000-000000000001","title":"Podcast title","description":"Podcast description","durationSeconds":3600,"publishedAt":"2026-06-01T10:15:30Z","language":null,"tags":null,"status":"PUBLISHED","isExplicit":null}}
```

### `podcast.play_finished.v1`

```json
{"eventId":"01000000-0000-0000-0000-000000000002","eventType":"podcast.play_finished.v1","eventVersion":1,"producer":"podcast-core","occurredAt":"2026-06-01T10:15:30Z","correlationId":null,"causationId":null,"userId":"50000000-0000-0000-0000-000000000001","payload":{"podcastId":"10000000-0000-0000-0000-000000000001","userId":"50000000-0000-0000-0000-000000000001","authorId":"20000000-0000-0000-0000-000000000001","categoryId":"30000000-0000-0000-0000-000000000001","durationSeconds":3600,"progressSeconds":3420,"progressPercent":95.00,"source":"listen_history","occurredAt":"2026-06-01T10:15:30Z","finishedAt":"2026-06-01T10:15:30Z"}}
```

### `podcast.liked.v1`

```json
{"eventId":"01000000-0000-0000-0000-000000000003","eventType":"podcast.liked.v1","eventVersion":1,"producer":"podcast-core","occurredAt":"2026-06-01T10:15:30Z","correlationId":null,"causationId":null,"userId":"50000000-0000-0000-0000-000000000001","payload":{"podcastId":"10000000-0000-0000-0000-000000000001","userId":"50000000-0000-0000-0000-000000000001","authorId":"20000000-0000-0000-0000-000000000001","categoryId":"30000000-0000-0000-0000-000000000001","occurredAt":"2026-06-01T10:15:30Z","likedAt":"2026-06-01T10:15:30Z"}}
```

### `playlist.updated.v1`

```json
{"eventId":"01000000-0000-0000-0000-000000000004","eventType":"playlist.updated.v1","eventVersion":1,"producer":"podcast-core","occurredAt":"2026-06-01T10:15:30Z","correlationId":null,"causationId":null,"userId":"50000000-0000-0000-0000-000000000001","payload":{"playlistId":"40000000-0000-0000-0000-000000000001","ownerUserId":"50000000-0000-0000-0000-000000000001","title":"Updated playlist title","description":"Playlist description","publicPlaylist":true,"podcastIds":["10000000-0000-0000-0000-000000000001"],"createdAt":"2026-05-31T10:15:30Z","updatedAt":"2026-06-01T10:15:30Z"}}
```

Полные форматированные snapshots лежат в `src/test/resources/recommendation-events`.

## Feature Flags

| Variable | Default | Purpose |
|---|---|---|
| `PODCAST_RECOMMENDATION_EVENTS_ENABLED` | `false` | записывать recommendation events в outbox внутри доменной транзакции |
| `PODCAST_OUTBOX_PUBLISHER_ENABLED` | `false` | запускать scheduled publisher и recovery |
| `PODCAST_KAFKA_PRODUCER_ENABLED` | `false` | разрешать отправку из outbox в Kafka |
| `PODCAST_OUTBOX_BATCH_SIZE` | `100` | размер claim batch |
| `PODCAST_OUTBOX_PUBLISH_DELAY_MS` | `3000` | scheduler delay и retry backoff |
| `PODCAST_OUTBOX_MAX_RETRY_ATTEMPTS` | `10` | максимум автоматических попыток |
| `PODCAST_OUTBOX_PROCESSING_TIMEOUT_MS` | `600000` | timeout stale `PROCESSING` lease |
| `PODCAST_OUTBOX_SEND_TIMEOUT_MS` | `10000` | timeout ожидания Kafka send |

## Publisher Flow

1. Короткая транзакция выбирает доступные `NEW`/`FAILED` rows через `FOR UPDATE SKIP LOCKED`, проверяет retry limit, переводит rows в `PROCESSING`, ставит `processing_started_at` и коммитит.
2. Publisher вне DB-транзакции отправляет каждое событие в Kafka с timeout.
3. Отдельная короткая транзакция переводит row в `SENT` или в `FAILED` с увеличением `retry_count`, `last_error` и новым `available_at`.
4. Перед claim publisher восстанавливает stale `PROCESSING` rows в `FAILED` с ошибкой `stale processing recovered`.

Метрики: `outbox.events.sent`, `outbox.events.failed`, `outbox.events.retry`, `outbox.events.pending`, `outbox.events.processing.recovered`.

## Operations

Проверить outbox:

```sql
select status, count(*)
from outbox_events
group by status
order by status;
```

Посмотреть ошибки:

```sql
select id, event_type, retry_count, available_at, processing_started_at, last_error
from outbox_events
where status in ('FAILED', 'PROCESSING')
order by created_at;
```

Переотправить исчерпанные `FAILED` rows после устранения причины:

```sql
update outbox_events
set retry_count = 0, available_at = now(), last_error = null
where status = 'FAILED' and retry_count >= 10;
```

Восстановить stale `PROCESSING` вручную при выключенном publisher:

```sql
update outbox_events
set status = 'FAILED',
    retry_count = retry_count + 1,
    available_at = now(),
    processing_started_at = null,
    last_error = 'stale processing recovered manually'
where status = 'PROCESSING';
```

## Rollout

1. Deploy Core со всеми тремя flags `false`.
2. Включить `PODCAST_RECOMMENDATION_EVENTS_ENABLED=true`, проверить рост `NEW` rows.
3. В dev/stage включить `PODCAST_OUTBOX_PUBLISHER_ENABLED=true` и `PODCAST_KAFKA_PRODUCER_ENABLED=true`, проверить `SENT`, Kafka topics и consumer lag.
4. Включить consumers Recommendation Service.
5. Мониторить outbox metrics, `FAILED`, stale recovery и consumer duplicates.

Rollback не требует отката доменных изменений: выключить `PODCAST_RECOMMENDATION_EVENTS_ENABLED` для остановки новых writes и `PODCAST_OUTBOX_PUBLISHER_ENABLED` либо `PODCAST_KAFKA_PRODUCER_ENABLED` для остановки отправки.
