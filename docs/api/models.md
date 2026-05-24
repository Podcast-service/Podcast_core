# Модели API

## Общие модели

| Модель | Назначение |
|---|---|
| `PageResponse<T>` | список с `items` и `meta` |
| `PageMeta` | `page`, `size`, `totalElements`, `totalPages` |
| `ErrorResponse` | единый формат ошибок |

## Пользователи

| Модель | Поля |
|---|---|
| `UserProfileResponse` | `id`, `userId`, `username`, `avatarUrl`, `createdAt` |
| `UserProfilePrivateResponse` | публичные поля профиля, `theme`, `language` |
| `UpdateUserProfileRequest` | `username`, `avatarUrl` |
| `UpdateSettingsRequest` | `theme`, `language` |
| `UserSettingsResponse` | `theme`, `language` |

Enums: `Theme` = `DARK`, `LIGHT`; `Language` = `RU`, `EN`.

## Авторы

| Модель | Поля |
|---|---|
| `AuthorCard` | `id`, `authorName`, `avatarUrl`, `subscribersCount`, `isSubscribed` |
| `AuthorProfileResponse` | `id`, `userId`, `authorName`, `avatarUrl`, `description`, `subscribersCount`, `isSubscribed`, `createdAt` |
| `CreateAuthorProfileRequest` | `authorName`, `description` |
| `UpdateAuthorProfileRequest` | `authorName`, `description` |

## Категории

| Модель | Поля |
|---|---|
| `CategoryResponse` | `id`, `name`, `position` |
| `CreateCategoryRequest` | `name`, `position` |
| `UpdateCategoryRequest` | `name`, `position` |

## Подкасты

| Модель | Поля |
|---|---|
| `PodcastCard` | `id`, `title`, `author`, `category`, `coverImageUrl`, `durationSeconds`, `num_speakers`, `status`, `viewsCount`, `likesCount`, `dislikesCount`, `publishedAt`, `createdAt`, `currentUserVote`, `progressSeconds`, `progressPercent` |
| `PodcastDetailResponse` | поля карточки, `description`, `audioUrl`, `audio_url_file`, `audio_size_file`, `hasTranscript`, `hasSummary` |
| `PodcastSpeakersResponse` | `podcastId`, `num_speakers` |
| `CreatePodcastRequest` | `title`, `description`, `categoryId`, `coverImageUrl`, `num_speakers` |
| `UpdatePodcastRequest` | `title`, `description`, `categoryId`, `coverImageUrl` |
| `PodcastTranscriptResponse` | `podcastId`, `language`, `content`, `generatedAt` |
| `PodcastSummaryResponse` | `podcastId`, `language`, `content`, `generatedAt` |

`PodcastStatus`: `DRAFT`, `PROCESSING`, `PUBLISHED`, `FAILED`, `ARCHIVED`.

### Поля подкаста

| Поле | Тип | Nullable | Ограничения | Назначение |
|---|---|---:|---|---|
| `num_speakers` | integer | нет | `1..32` | количество спикеров в выпуске |
| `audio_url_file` | string | да | пустая строка не допускается на уровне БД | URL или путь к аудиофайлу выпуска |
| `audio_size_file` | integer int64 | да | `>= 0` | размер аудиофайла в байтах |

`CreatePodcastRequest` принимает `num_speakers` в snake case. Для совместимости с Java-клиентами backend также принимает alias `numSpeakers`.

```json
{
  "title": "Архитектура поиска",
  "description": "Разговор о полнотекстовом поиске и ранжировании",
  "categoryId": "48b67732-5676-36bd-a97f-44d01de91376",
  "coverImageUrl": "https://cdn.example.local/covers/search.png",
  "num_speakers": 3
}
```

## Плейлисты

| Модель | Поля |
|---|---|
| `PlaylistCard` | `id`, `title`, `coverImageUrl`, `owner`, `isPublic`, `podcastsCount`, `likesCount`, `dislikesCount`, `createdAt`, `currentUserVote` |
| `PlaylistDetailResponse` | поля карточки, `description`, `updatedAt`, `podcasts` |
| `PlaylistOwnerResponse` | `id`, `username`, `avatarUrl` |
| `PlaylistPodcastItem` | `podcast`, `position`, `addedAt` |
| `CreatePlaylistRequest` | `title`, `description`, `coverImageUrl`, `isPublic` |
| `UpdatePlaylistRequest` | `title`, `description`, `coverImageUrl`, `isPublic` |
| `AddPodcastToPlaylistRequest` | `podcastId` |
| `ReorderPlaylistRequest` | `items` |

## Социальные действия

| Модель | Поля |
|---|---|
| `VoteRequest` | `voteType` |
| `VoteResponse` | `targetId`, `targetType`, `likesCount`, `dislikesCount`, `currentUserVote` |
| `SubscriptionResponse` | `author`, `subscribedAt` |
| `AuthorSubscriptionResponse` | `authorId`, `subscribersCount`, `isSubscribed` |
| `ListenHistoryItem` | `podcast`, `progressSeconds`, `progressPercent`, `completed`, `lastListenedAt` |
| `SaveProgressRequest` | `progressSeconds` |

`VoteType`: `LIKE`, `DISLIKE`.

## Поиск

| Модель | Поля |
|---|---|
| `SearchResponse` | `podcasts`, `authors`, `playlists` |
| `SearchSuggestItem` | `type`, `id`, `label`, `coverUrl` |

Search type: `ALL`, `PODCAST`, `AUTHOR`, `PLAYLIST`.
