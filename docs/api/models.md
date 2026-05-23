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
| `PodcastCard` | `id`, `title`, `author`, `category`, `coverImageUrl`, `durationSeconds`, `status`, `viewsCount`, `likesCount`, `dislikesCount`, `publishedAt`, `createdAt`, `currentUserVote`, `progressSeconds`, `progressPercent` |
| `PodcastDetailResponse` | поля карточки, `description`, `audioUrl`, `updatedAt` |
| `CreatePodcastRequest` | `title`, `description`, `categoryId`, `coverImageUrl` |
| `UpdatePodcastRequest` | `title`, `description`, `categoryId`, `coverImageUrl` |
| `PodcastTranscriptResponse` | `podcastId`, `language`, `content`, `generatedAt` |
| `PodcastSummaryResponse` | `podcastId`, `language`, `content`, `generatedAt` |

`PodcastStatus`: `DRAFT`, `PROCESSING`, `PUBLISHED`, `FAILED`, `ARCHIVED`.

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
