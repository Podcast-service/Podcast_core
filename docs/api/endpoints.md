# Endpoints

Документ описывает endpoint'ы из `openapi-2.yaml` и текущих контроллеров. Общие ошибки для всех ручек: `400 VALIDATION_ERROR`, `401 UNAUTHORIZED`, `403 FORBIDDEN`, `404 RESOURCE_NOT_FOUND`, `409 CONFLICT`, `422 BUSINESS_RULE_VIOLATION`, `500 INTERNAL_ERROR`. Конкретный набор зависит от ручки.

## Сводная таблица

| Метод | Путь | Доступ | Назначение |
|---|---|---|---|
| GET | `/users/me/profile` | Authenticated | Получить свой профиль |
| PUT | `/users/me/profile` | Authenticated | Обновить свой профиль |
| GET | `/users/me/settings` | Authenticated | Получить настройки |
| PUT | `/users/me/settings` | Authenticated | Обновить настройки |
| GET | `/users/me/playlists` | Authenticated | Мои плейлисты |
| GET | `/users/me/subscriptions` | Authenticated | Мои подписки |
| GET | `/users/me/subscriptions/feed` | Authenticated | Лента подписок |
| GET | `/users/me/history` | Authenticated | История прослушивания |
| POST | `/authors/me` | Author | Создать мой профиль автора |
| GET | `/authors/me` | Author | Получить мой профиль автора |
| PUT | `/authors/me` | Author | Обновить мой профиль автора |
| GET | `/authors/{authorId}` | Public, optional token | Публичный профиль автора |
| GET | `/authors/{authorId}/podcasts` | Public, optional token | Подкасты автора |
| GET | `/authors/{authorId}/playlists` | Public, optional token | Плейлисты автора |
| GET | `/categories` | Public | Список категорий |
| POST | `/categories` | Admin | Создать категорию |
| PUT | `/categories/{categoryId}` | Admin | Обновить категорию |
| DELETE | `/categories/{categoryId}` | Admin | Удалить категорию |
| GET | `/podcasts` | Public, optional token | Каталог подкастов |
| POST | `/podcasts` | Author | Создать подкаст |
| GET | `/podcasts/{podcastId}` | Public, optional token | Детали подкаста |
| PUT | `/podcasts/{podcastId}` | Author owner | Обновить подкаст |
| DELETE | `/podcasts/{podcastId}` | Author owner | Архивировать подкаст |
| POST | `/podcasts/{podcastId}/publish` | Author owner | Отправить на публикацию |
| POST | `/podcasts/{podcastId}/progress` | Authenticated | Сохранить прогресс |
| GET | `/podcasts/{podcastId}/transcript` | Public | Получить transcript |
| GET | `/podcasts/{podcastId}/summary` | Public | Получить summary |
| POST | `/podcasts/{podcastId}/vote` | Authenticated | Поставить/сменить голос |
| DELETE | `/podcasts/{podcastId}/vote` | Authenticated | Удалить голос |
| GET | `/playlists` | Public, optional token | Публичные плейлисты |
| POST | `/playlists` | Authenticated | Создать плейлист |
| GET | `/playlists/{playlistId}` | Public/owner | Детали плейлиста |
| PUT | `/playlists/{playlistId}` | Owner | Обновить плейлист |
| DELETE | `/playlists/{playlistId}` | Owner | Удалить плейлист |
| POST | `/playlists/{playlistId}/podcasts` | Owner | Добавить подкаст |
| DELETE | `/playlists/{playlistId}/podcasts/{podcastId}` | Owner | Удалить подкаст из плейлиста |
| PUT | `/playlists/{playlistId}/podcasts/reorder` | Owner | Изменить порядок |
| POST | `/playlists/{playlistId}/vote` | Authenticated | Поставить/сменить голос |
| DELETE | `/playlists/{playlistId}/vote` | Authenticated | Удалить голос |
| POST | `/authors/{authorId}/subscribe` | Authenticated | Подписаться на автора |
| DELETE | `/authors/{authorId}/subscribe` | Authenticated | Отписаться |
| GET | `/search/suggest` | Public | Подсказки поиска |
| GET | `/search` | Public, optional token | Поиск |

## Users

### GET `/users/me/profile`

Получает приватный профиль текущего пользователя.

| Параметры | Описание |
|---|---|
| Header `Authorization` | `Bearer <access_token>`, обязательно |

Ответ `200`: `UserProfilePrivateResponse`.

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "demo_user",
  "avatarUrl": "https://cdn.example.com/u/demo.png",
  "theme": "DARK",
  "language": "RU",
  "createdAt": "2026-05-20T10:00:00Z"
}
```

Ошибки: `401`, `404`.

### PUT `/users/me/profile`

Обновляет username/avatar текущего пользователя.

Request body: `UpdateUserProfileRequest`.

```json
{
  "username": "demo_user_2",
  "avatarUrl": "https://cdn.example.com/u/demo2.png"
}
```

Ответ `200`: `UserProfilePrivateResponse`. Ошибки: `400`, `401`, `404`, `409`.

### GET `/users/me/settings`

Возвращает тему и язык текущего пользователя. Ответ `200`:

```json
{
  "theme": "DARK",
  "language": "RU"
}
```

Ошибки: `401`, `404`.

### PUT `/users/me/settings`

Request body:

```json
{
  "theme": "LIGHT",
  "language": "EN"
}
```

Ответ `200`: `UserSettingsResponse`. Ошибки: `400`, `401`, `404`.

## Authors

### POST `/authors/me`

Создаёт профиль автора для текущего пользователя. Нужна роль `author`.

Request body:

```json
{
  "authorName": "Backend Talks",
  "description": "Подкаст о backend-разработке"
}
```

Ответ `201`: `AuthorProfileResponse`. Ошибки: `400`, `401`, `403`, `404`, `409`.

### GET `/authors/me`

Получает профиль автора текущего пользователя. Нужна роль `author`.

Ответ `200`: `AuthorProfileResponse`. Ошибки: `401`, `403`, `404`.

### PUT `/authors/me`

Обновляет профиль автора. Нужна роль `author`.

```json
{
  "authorName": "Backend Talks Updated",
  "description": "Новые выпуски каждую неделю"
}
```

Ответ `200`: `AuthorProfileResponse`. Ошибки: `400`, `401`, `403`, `404`, `409`.

### GET `/authors/{authorId}`

Публичный профиль автора. Токен опционален: с токеном заполняется `isSubscribed`.

| Path | Тип |
|---|---|
| `authorId` | uuid |

Ответ `200`:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440100",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "authorName": "Backend Talks",
  "avatarUrl": "https://cdn.example.com/u/demo.png",
  "description": "Подкаст о backend-разработке",
  "subscribersCount": 42,
  "isSubscribed": false,
  "createdAt": "2026-05-20T10:00:00Z"
}
```

Ошибки: `404`.

### GET `/authors/{authorId}/podcasts`

Список опубликованных подкастов автора.

| Query | Тип | По умолчанию |
|---|---|---|
| `q` | string | нет |
| `sort` | `DATE_DESC`, `DATE_ASC`, `RATING`, `VIEWS` | `DATE_DESC` |
| `page` | integer | `1` |
| `size` | integer | `20` |

Ответ `200`: `PageResponse<PodcastCard>`. Ошибки: `400`, `404`.

### GET `/authors/{authorId}/playlists`

Список публичных плейлистов автора.

| Query | Тип | По умолчанию |
|---|---|---|
| `page` | integer | `1` |
| `size` | integer | `20` |

Ответ `200`: `PageResponse<PlaylistCard>`. Ошибки: `400`, `404`.

## Categories

### GET `/categories`

Возвращает категории, отсортированные по позиции.

Ответ `200`:

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440200",
    "name": "Technology",
    "position": 1
  }
]
```

### POST `/categories`

Создаёт категорию. Нужна роль `admin`.

```json
{
  "name": "Science",
  "position": 2
}
```

Ответ `201`: `CategoryResponse`. Ошибки: `400`, `401`, `403`, `409`.

### PUT `/categories/{categoryId}`

Обновляет категорию. Нужна роль `admin`.

```json
{
  "name": "Science & Tech",
  "position": 3
}
```

Ответ `200`: `CategoryResponse`. Ошибки: `400`, `401`, `403`, `404`, `409`.

### DELETE `/categories/{categoryId}`

Удаляет категорию. Нужна роль `admin`.

Ответ `204`, body отсутствует. Ошибки: `401`, `403`, `404`, `409`.

## Podcasts

### GET `/podcasts`

Возвращает каталог опубликованных подкастов. Токен опционален.

| Query | Тип | По умолчанию |
|---|---|---|
| `q` | string | нет |
| `categoryId` | uuid | нет |
| `authorId` | uuid | нет |
| `sort` | `DATE_DESC`, `DATE_ASC`, `RATING`, `VIEWS` | `DATE_DESC` |
| `page` | integer | `1` |
| `size` | integer | `20` |

Ответ `200`: `PageResponse<PodcastCard>`.

```json
{
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440300",
      "title": "Spring Security без боли",
      "author": {
        "id": "550e8400-e29b-41d4-a716-446655440100",
        "authorName": "Backend Talks",
        "avatarUrl": null,
        "subscribersCount": 42,
        "isSubscribed": false
      },
      "category": {
        "id": "550e8400-e29b-41d4-a716-446655440200",
        "name": "Technology",
        "position": 1
      },
      "coverImageUrl": null,
      "durationSeconds": 1800,
      "status": "PUBLISHED",
      "viewsCount": 100,
      "likesCount": 10,
      "dislikesCount": 1,
      "publishedAt": "2026-05-20T10:00:00Z",
      "createdAt": "2026-05-20T09:00:00Z",
      "currentUserVote": null,
      "progressSeconds": null,
      "progressPercent": null
    }
  ],
  "meta": {
    "page": 1,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

Ошибки: `400`.

### POST `/podcasts`

Создаёт черновик подкаста для текущего автора. Нужна роль `author` и существующий author profile.

```json
{
  "title": "Spring Security без боли",
  "description": "Разбираем JWT, роли и фильтры.",
  "categoryId": "550e8400-e29b-41d4-a716-446655440200",
  "coverImageUrl": "https://cdn.example.com/podcast.png"
}
```

Ответ `201`: `PodcastDetailResponse`. Ошибки: `400`, `401`, `403`, `404`, `409`.

### GET `/podcasts/{podcastId}`

Возвращает детали подкаста. Публично доступны опубликованные подкасты; владелец с токеном может получить свой неопубликованный подкаст.

Ответ `200`: `PodcastDetailResponse`. Ошибки: `404`.

### PUT `/podcasts/{podcastId}`

Обновляет подкаст. Нужна роль `author` и владение подкастом.

```json
{
  "title": "Spring Security на практике",
  "description": "Обновлённое описание",
  "categoryId": "550e8400-e29b-41d4-a716-446655440200",
  "coverImageUrl": null
}
```

Ответ `200`: `PodcastDetailResponse`. Ошибки: `400`, `401`, `403`, `404`, `422`.

### DELETE `/podcasts/{podcastId}`

Архивирует подкаст. Нужна роль `author` и владение.

Ответ `204`, body отсутствует. Ошибки: `401`, `403`, `404`, `422`.

### POST `/podcasts/{podcastId}/publish`

Переводит подкаст в публикационный flow. Нужна роль `author` и владение.

Ответ `202`: `PodcastDetailResponse`. Возможные бизнес-ошибки: нет audio URL, неподходящий статус. Ошибки: `401`, `403`, `404`, `422`.

### POST `/podcasts/{podcastId}/progress`

Сохраняет прогресс прослушивания текущего пользователя.

```json
{
  "progressSeconds": 600
}
```

Ответ `204`, body отсутствует. Ошибки: `400`, `401`, `404`, `422`.

### GET `/podcasts/{podcastId}/transcript`

Возвращает transcript подкаста.

Ответ `200`:

```json
{
  "podcastId": "550e8400-e29b-41d4-a716-446655440300",
  "language": "ru",
  "content": "Текстовая расшифровка выпуска...",
  "generatedAt": "2026-05-20T10:00:00Z"
}
```

Ошибки: `404`.

### GET `/podcasts/{podcastId}/summary`

Возвращает summary подкаста. Ответ `200`: `PodcastSummaryResponse`. Ошибки: `404`.

### POST `/podcasts/{podcastId}/vote`

Ставит или меняет голос текущего пользователя.

```json
{
  "voteType": "LIKE"
}
```

Ответ `200`:

```json
{
  "targetId": "550e8400-e29b-41d4-a716-446655440300",
  "targetType": "PODCAST",
  "likesCount": 11,
  "dislikesCount": 1,
  "currentUserVote": "LIKE"
}
```

Ошибки: `400`, `401`, `404`, `422`.

### DELETE `/podcasts/{podcastId}/vote`

Удаляет голос текущего пользователя.

Ответ `200`: `VoteResponse` с `currentUserVote: null`. Ошибки: `401`, `404`.

Несоответствие: описание в `openapi-2.yaml` допускает `204`, если пользователь не голосовал, но текущий контроллер возвращает `200 OK`.

## Playlists

### GET `/users/me/playlists`

Список плейлистов текущего пользователя.

| Query | Тип | По умолчанию |
|---|---|---|
| `page` | integer | `1` |
| `size` | integer | `20` |

Ответ `200`: `PageResponse<PlaylistCard>`. Ошибки: `400`, `401`.

### GET `/playlists`

Возвращает публичные плейлисты. Токен опционален.

| Query | Тип | По умолчанию |
|---|---|---|
| `q` | string | нет |
| `sort` | `DATE_DESC`, `DATE_ASC`, `RATING` | `DATE_DESC` |
| `page` | integer | `1` |
| `size` | integer | `20` |

Ответ `200`: `PageResponse<PlaylistCard>`.

### POST `/playlists`

Создаёт плейлист текущего пользователя.

```json
{
  "title": "Backend essentials",
  "description": "Выпуски для спокойного погружения",
  "coverImageUrl": "https://cdn.example.com/playlist.png",
  "isPublic": true
}
```

Ответ `201`: `PlaylistDetailResponse`. Ошибки: `400`, `401`, `404`.

### GET `/playlists/{playlistId}`

Возвращает детали плейлиста. Публичный плейлист доступен всем; приватный доступен владельцу.

Ответ `200`:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440400",
  "title": "Backend essentials",
  "coverImageUrl": null,
  "owner": {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "username": "demo_user"
  },
  "isPublic": true,
  "podcastsCount": 1,
  "likesCount": 5,
  "dislikesCount": 0,
  "createdAt": "2026-05-20T10:00:00Z",
  "currentUserVote": null,
  "description": "Выпуски для спокойного погружения",
  "updatedAt": "2026-05-20T10:00:00Z",
  "podcasts": []
}
```

Ошибки: `404`.

### PUT `/playlists/{playlistId}`

Обновляет плейлист владельца.

```json
{
  "title": "Backend essentials 2026",
  "description": null,
  "coverImageUrl": null,
  "isPublic": false
}
```

Ответ `200`: `PlaylistDetailResponse`. Ошибки: `400`, `401`, `403`, `404`.

### DELETE `/playlists/{playlistId}`

Удаляет плейлист владельца.

Ответ `204`, body отсутствует. Ошибки: `401`, `403`, `404`.

### POST `/playlists/{playlistId}/podcasts`

Добавляет опубликованный подкаст в плейлист владельца.

```json
{
  "podcastId": "550e8400-e29b-41d4-a716-446655440300"
}
```

Ответ `200`: `PlaylistDetailResponse`. Ошибки: `400`, `401`, `403`, `404`, `409`, `422`.

### DELETE `/playlists/{playlistId}/podcasts/{podcastId}`

Удаляет подкаст из плейлиста владельца.

Ответ `204`, body отсутствует. Ошибки: `401`, `403`, `404`.

### PUT `/playlists/{playlistId}/podcasts/reorder`

Меняет порядок подкастов в плейлисте.

```json
{
  "items": [
    {
      "podcastId": "550e8400-e29b-41d4-a716-446655440300",
      "position": 1
    }
  ]
}
```

Ответ `200`: `PlaylistDetailResponse`. Ошибки: `400`, `401`, `403`, `404`, `422`.

### POST `/playlists/{playlistId}/vote`

Ставит или меняет голос за плейлист.

```json
{
  "voteType": "DISLIKE"
}
```

Ответ `200`: `VoteResponse`. Ошибки: `400`, `401`, `404`.

### DELETE `/playlists/{playlistId}/vote`

Удаляет голос за плейлист.

Ответ `200`: `VoteResponse` с `currentUserVote: null`. Ошибки: `401`, `404`.

## Subscriptions

### GET `/users/me/subscriptions`

Список авторов, на которых подписан текущий пользователь.

| Query | Тип | По умолчанию |
|---|---|---|
| `page` | integer | `1` |
| `size` | integer | `20` |

Ответ `200`: `PageResponse<SubscriptionResponse>`. Ошибки: `400`, `401`.

### GET `/users/me/subscriptions/feed`

Лента опубликованных подкастов авторов, на которых подписан текущий пользователь.

| Query | Тип | По умолчанию |
|---|---|---|
| `sort` | `DATE_DESC`, `DATE_ASC`, `RATING`, `VIEWS` | `DATE_DESC` |
| `page` | integer | `1` |
| `size` | integer | `20` |

Ответ `200`: `PageResponse<PodcastCard>`. Ошибки: `400`, `401`.

### POST `/authors/{authorId}/subscribe`

Подписывает текущего пользователя на автора.

Ответ `200`:

```json
{
  "authorId": "550e8400-e29b-41d4-a716-446655440100",
  "subscribersCount": 43,
  "isSubscribed": true
}
```

Ошибки: `401`, `404`, `409`, `422`.

### DELETE `/authors/{authorId}/subscribe`

Отписывает текущего пользователя.

Ответ `200`: `AuthorSubscriptionResponse` с `isSubscribed: false`. Ошибки: `401`, `404`.

## Listen history

### GET `/users/me/history`

История прослушивания текущего пользователя.

| Query | Тип | По умолчанию |
|---|---|---|
| `page` | integer | `1` |
| `size` | integer | `20` |

Ответ `200`:

```json
{
  "items": [
    {
      "podcast": {
        "id": "550e8400-e29b-41d4-a716-446655440300",
        "title": "Spring Security без боли",
        "author": null,
        "category": null,
        "coverImageUrl": null,
        "durationSeconds": 1800,
        "status": "PUBLISHED",
        "viewsCount": 100,
        "likesCount": 10,
        "dislikesCount": 1,
        "publishedAt": "2026-05-20T10:00:00Z",
        "createdAt": "2026-05-20T09:00:00Z",
        "currentUserVote": null,
        "progressSeconds": 600,
        "progressPercent": 33
      },
      "progressSeconds": 600,
      "progressPercent": 33,
      "completed": false,
      "lastListenedAt": "2026-05-20T10:30:00Z"
    }
  ],
  "meta": {
    "page": 1,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

Ошибки: `400`, `401`.

## Search

### GET `/search/suggest`

Возвращает autocomplete-подсказки.

| Query | Тип | Обяз. | Валидация |
|---|---|---:|---|
| `q` | string | да | минимум 1 символ |

Ответ `200`:

```json
[
  {
    "type": "PODCAST",
    "id": "550e8400-e29b-41d4-a716-446655440300",
    "label": "Spring Security без боли",
    "coverUrl": null
  }
]
```

Ошибки: `400`.

### GET `/search`

Полнотекстовый поиск по подкастам, авторам и плейлистам. Токен опционален.

| Query | Тип | По умолчанию |
|---|---|---|
| `q` | string, обязательно | нет |
| `type` | `ALL`, `PODCAST`, `AUTHOR`, `PLAYLIST` | `ALL` |
| `categoryId` | uuid | нет |
| `sort` | `RELEVANCE`, `DATE`, `RATING`, `VIEWS` | `RELEVANCE` |
| `page` | integer | `1` |
| `size` | integer | `20` |

Ответ `200`:

```json
{
  "podcasts": {
    "items": [],
    "meta": {
      "page": 1,
      "size": 20,
      "totalElements": 0,
      "totalPages": 0
    }
  },
  "authors": {
    "items": [],
    "meta": {
      "page": 1,
      "size": 20,
      "totalElements": 0,
      "totalPages": 0
    }
  },
  "playlists": {
    "items": [],
    "meta": {
      "page": 1,
      "size": 20,
      "totalElements": 0,
      "totalPages": 0
    }
  }
}
```

Ошибки: `400`.
