# Модели данных

Типы указаны по REST DTO и OpenAPI. `date-time` означает ISO-8601 timestamp.

## Общие модели

### PageResponse

| Поле | Тип | Обяз. | Описание |
|---|---|---:|---|
| `items` | array | да | Элементы текущей страницы |
| `meta` | `PageMeta` | да | Метаданные пагинации |

### PageMeta

| Поле | Тип | Обяз. |
|---|---|---:|
| `page` | integer | да |
| `size` | integer | да |
| `totalElements` | integer | да |
| `totalPages` | integer | да |

### Enum

| Enum | Значения |
|---|---|
| `VoteType` | `LIKE`, `DISLIKE` |
| `PodcastStatus` | `DRAFT`, `PROCESSING`, `PUBLISHED`, `FAILED`, `ARCHIVED` |
| `SortPodcasts` / `SortPodcast` | `DATE_DESC`, `DATE_ASC`, `RATING`, `VIEWS` |
| `SortPlaylists` | `DATE_DESC`, `DATE_ASC`, `RATING` |
| `SearchType` | `ALL`, `PODCAST`, `AUTHOR`, `PLAYLIST` |
| `SearchSort` | `RELEVANCE`, `DATE`, `RATING`, `VIEWS` |
| `Theme` | `DARK`, `LIGHT` |
| `Language` | `RU`, `EN` |

## User

### UserProfilePrivateResponse

| Поле | Тип | Обяз. | Описание |
|---|---|---:|---|
| `id` | uuid | да | ID профиля в `podcast-core` |
| `userId` | uuid | да | ID пользователя из `auth-service` |
| `username` | string | да | Логин |
| `avatarUrl` | string/null | нет | URL аватара |
| `theme` | enum | да | `DARK` или `LIGHT` |
| `language` | enum | да | `RU` или `EN` |
| `createdAt` | date-time | да | Дата создания |

### UpdateUserProfileRequest

| Поле | Тип | Обяз. | Валидация |
|---|---|---:|---|
| `username` | string | нет | `3..50`, regex `^[a-zA-Z0-9_]+$` |
| `avatarUrl` | string/null | нет | Максимальная длина не задана в DTO; поле хранится как `text` |

Пример:

```json
{
  "username": "podcast_user",
  "avatarUrl": "https://cdn.example.com/avatar.png"
}
```

### UpdateUserSettingsRequest

| Поле | Тип | Обяз. |
|---|---|---:|
| `theme` | `DARK`/`LIGHT` | нет |
| `language` | `RU`/`EN` | нет |

## Author

### AuthorProfileResponse

| Поле | Тип | Обяз. |
|---|---|---:|
| `id` | uuid | да |
| `userId` | uuid | да |
| `authorName` | string | да |
| `avatarUrl` | string/null | нет |
| `description` | string/null | нет |
| `subscribersCount` | integer | да |
| `isSubscribed` | boolean/null | нет |
| `createdAt` | date-time | да |

### CreateAuthorProfileRequest

| Поле | Тип | Обяз. | Валидация |
|---|---|---:|---|
| `authorName` | string | да | `2..100`, not blank |
| `description` | string/null | нет | `<= 1000` |

### UpdateAuthorProfileRequest

Все поля опциональны. Код различает отсутствующее поле и явный `null`.

| Поле | Тип | Валидация |
|---|---|---|
| `authorName` | string/null | если передан, `2..100` |
| `description` | string/null | если передан, `<= 1000` |

## Category

### CategoryResponse

| Поле | Тип | Обяз. |
|---|---|---:|
| `id` | uuid | да |
| `name` | string | да |
| `position` | integer | да |

### CreateCategoryRequest / UpdateCategoryRequest

| Поле | Тип | Обяз. при create | Валидация |
|---|---|---:|---|
| `name` | string | да | create: not blank, `2..100`; update: если передан, `2..100` |
| `position` | integer | нет | `>= 0` |

## Podcast

### PodcastCard

| Поле | Тип | Описание |
|---|---|---|
| `id` | uuid | ID подкаста |
| `title` | string | Название |
| `author` | `AuthorCard` | Автор |
| `category` | `CategoryResponse`/null | Категория |
| `coverImageUrl` | string/null | Обложка |
| `durationSeconds` | integer/null | Длительность |
| `status` | `PodcastStatus` | Статус |
| `viewsCount` | integer | Просмотры |
| `likesCount` | integer | Лайки |
| `dislikesCount` | integer | Дизлайки |
| `publishedAt` | date-time/null | Дата публикации |
| `createdAt` | date-time | Дата создания |
| `currentUserVote` | `VoteType`/null | Голос текущего пользователя |
| `progressSeconds` | integer/null | Прогресс текущего пользователя |
| `progressPercent` | integer/null | Прогресс в процентах |

### PodcastDetailResponse

Включает все поля `PodcastCard`, а также:

| Поле | Тип |
|---|---|
| `description` | string/null |
| `audioUrl` | string/null |
| `hasTranscript` | boolean |
| `hasSummary` | boolean |

### CreatePodcastRequest

| Поле | Тип | Обяз. | Валидация |
|---|---|---:|---|
| `title` | string | да | not blank, `3..255` |
| `description` | string/null | нет | `<= 5000` |
| `categoryId` | uuid/null | нет |
| `coverImageUrl` | string/null | нет |

### UpdatePodcastRequest

Все поля опциональны; код различает отсутствующее поле и явный `null`.

| Поле | Тип | Валидация |
|---|---|---|
| `title` | string/null | если передан, `3..255` |
| `description` | string/null | если передан, `<= 5000` |
| `categoryId` | uuid/null | существующая категория или `null` |
| `coverImageUrl` | string/null | Максимальная длина не задана в DTO; поле хранится как `text` |

### PodcastTranscriptResponse / PodcastSummaryResponse

| Поле | Тип |
|---|---|
| `podcastId` | uuid |
| `language` | string |
| `content` | string |
| `generatedAt` | date-time |

## Playlist

### PlaylistCard

| Поле | Тип |
|---|---|
| `id` | uuid |
| `title` | string |
| `coverImageUrl` | string/null |
| `owner` | `PlaylistOwnerResponse` |
| `isPublic` | boolean |
| `podcastsCount` | integer |
| `likesCount` | integer |
| `dislikesCount` | integer |
| `createdAt` | date-time |
| `currentUserVote` | `VoteType`/null |

### PlaylistDetailResponse

Включает поля `PlaylistCard`, а также:

| Поле | Тип |
|---|---|
| `description` | string/null |
| `updatedAt` | date-time |
| `podcasts` | array of `PlaylistPodcastItem` |

### CreatePlaylistRequest

| Поле | Тип | Обяз. | Валидация |
|---|---|---:|---|
| `title` | string | да | not blank, `<= 255` |
| `description` | string/null | нет | `<= 1000` |
| `coverImageUrl` | string/null | нет | `<= 2048` |
| `isPublic` | boolean/null | нет | если не передан, сервис применяет дефолт |

### UpdatePlaylistRequest

| Поле | Тип | Валидация |
|---|---|---|
| `title` | string/null | если передан, `1..255` |
| `description` | string/null | если передан, `<= 1000` |
| `coverImageUrl` | string/null | если передан, `<= 2048` |
| `isPublic` | boolean/null | опционально |

### AddPodcastToPlaylistRequest

| Поле | Тип | Обяз. |
|---|---|---:|
| `podcastId` | uuid | да |

### ReorderPlaylistRequest

| Поле | Тип | Обяз. | Валидация |
|---|---|---:|---|
| `items` | array | да | not empty |

`items[]`:

| Поле | Тип | Обяз. | Валидация |
|---|---|---:|---|
| `podcastId` | uuid | да | Идентификатор подкаста из текущего плейлиста |
| `position` | integer | да | `>= 1` |

## Votes

### VoteRequest

| Поле | Тип | Обяз. |
|---|---|---:|
| `voteType` | `LIKE`/`DISLIKE` | да |

### VoteResponse

| Поле | Тип |
|---|---|
| `targetId` | uuid |
| `targetType` | string, например `PODCAST` или `PLAYLIST` |
| `likesCount` | integer |
| `dislikesCount` | integer |
| `currentUserVote` | `VoteType`/null |

## Subscriptions

### SubscriptionResponse

| Поле | Тип |
|---|---|
| `author` | `AuthorCard` |
| `subscribedAt` | date-time |

### AuthorSubscriptionResponse

| Поле | Тип |
|---|---|
| `authorId` | uuid |
| `subscribersCount` | integer |
| `isSubscribed` | boolean |

## Listen history

### SaveProgressRequest

| Поле | Тип | Обяз. | Валидация |
|---|---|---:|---|
| `progressSeconds` | integer | да | `>= 0` |

### ListenHistoryItem

| Поле | Тип |
|---|---|
| `podcast` | `PodcastCard` |
| `progressSeconds` | integer |
| `progressPercent` | integer |
| `completed` | boolean |
| `lastListenedAt` | date-time |

## Search

### SearchSuggestItem

| Поле | Тип |
|---|---|
| `type` | `PODCAST`/`AUTHOR`/`PLAYLIST` |
| `id` | uuid |
| `label` | string |
| `coverUrl` | string/null |

### SearchResponse

| Поле | Тип |
|---|---|
| `podcasts` | `PageResponse<PodcastCard>` |
| `authors` | `PageResponse<AuthorCard>` |
| `playlists` | `PageResponse<PlaylistCard>` |

## Основные доменные сущности

| Таблица | Сущность | Ключевые ограничения |
|---|---|---|
| `user_profiles` | `UserProfileEntity` | unique `user_id`, unique username в БД, defaults `theme=DARK`, `language=RU` |
| `author_profiles` | `AuthorEntity` | один author profile на user profile, `author_name` `2..100`, description `<=1000` |
| `categories` | `CategoryEntity` | unique `name`, unique `position`, `position >= 0` |
| `podcasts` | `PodcastEntity` | status enum, counters `>=0`, publish consistency |
| `playlists` | `PlaylistEntity` | owner required, counters `>=0`, public flag |
| `playlist_podcasts` | `PlaylistPodcastEntity` | unique podcast per playlist, unique position per playlist |
| `subscriptions` | `SubscriptionEntity` | unique subscriber-author pair, no self-subscribe trigger |
| `podcast_votes` | `PodcastVoteEntity` | one vote per user per podcast |
| `playlist_votes` | `PlaylistVoteEntity` | one vote per user per playlist |
| `listen_history` | `ListenHistoryEntity` | one progress row per user per podcast, `progress_seconds >= 0` |
