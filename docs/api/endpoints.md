# HTTP операции

Общие правила:

| Правило | Значение |
|---|---|
| Base path | `/podcast/v1` |
| Авторизация | `Authorization: Bearer <access_token>` |
| Публичные GET | доступны без токена |
| Опциональный токен | публичная ручка использует токен для персонализации ответа |
| Ошибки | формат `ApiErrorResponse` |

## Сводная таблица

| Метод | Путь | Доступ | Тело запроса | Ответ |
|---|---|---|---|---|
| `GET` | `/users/me/profile` | JWT | нет | `UserProfilePrivateResponse` |
| `PUT` | `/users/me/profile` | JWT | `UpdateUserProfileRequest` | `UserProfilePrivateResponse` |
| `GET` | `/users/me/settings` | JWT | нет | `UserSettingsResponse` |
| `PUT` | `/users/me/settings` | JWT | `UpdateSettingsRequest` | `UserSettingsResponse` |
| `GET` | `/users/me/playlists` | JWT | нет | `PageOfPlaylistCard` |
| `GET` | `/users/me/liked-podcasts` | JWT | нет | `PageOfPodcastCard` |
| `GET` | `/users/me/library/playlists` | JWT | нет | `PageOfPlaylistCard` |
| `GET` | `/users/me/subscriptions` | JWT | нет | `PageOfSubscription` |
| `GET` | `/users/me/subscriptions/feed` | JWT | нет | `PageOfPodcastCard` |
| `GET` | `/users/me/history` | JWT | нет | `PageOfListenHistory` |
| `POST` | `/authors/me` | JWT | `CreateAuthorProfileRequest` | `BecomeAuthorResponse` |
| `GET` | `/authors` | публичный, токен опционален | нет | `PageOfAuthorCard` |
| `GET` | `/authors/me` | JWT | нет | `AuthorProfileResponse` |
| `PUT` | `/authors/me` | роль `author` | `UpdateAuthorProfileRequest` | `AuthorProfileResponse` |
| `GET` | `/authors/me/podcasts` | роль `author` | нет | `PageOfPodcastDetailResponse` |
| `GET` | `/authors/{authorId}` | публичный, токен опционален | нет | `AuthorProfileResponse` |
| `GET` | `/authors/{authorId}/podcasts` | публичный, токен опционален | нет | `PageOfPodcastCard` |
| `GET` | `/authors/{authorId}/playlists` | публичный, токен опционален | нет | `PageOfPlaylistCard` |
| `GET` | `/categories` | публичный | нет | массив `CategoryResponse` |
| `POST` | `/categories` | роль `admin` | `CreateCategoryRequest` | `CategoryResponse` |
| `PUT` | `/categories/{categoryId}` | роль `admin` | `UpdateCategoryRequest` | `CategoryResponse` |
| `DELETE` | `/categories/{categoryId}` | роль `admin` | нет | `204` |
| `GET` | `/podcasts` | публичный, токен опционален | нет | `PageOfPodcastCard` |
| `POST` | `/podcasts` | роль `author` | `CreatePodcastRequest` | `PodcastDetailResponse` |
| `GET` | `/podcasts/{podcastId}` | публичный, токен опционален | нет | `PodcastDetailResponse` |
| `GET` | `/podcasts/{podcastId}/speakers` | публичный, токен опционален | нет | `PodcastSpeakersResponse` |
| `PUT` | `/podcasts/{podcastId}` | роль `author`, владелец | `UpdatePodcastRequest` | `PodcastDetailResponse` |
| `DELETE` | `/podcasts/{podcastId}` | роль `author`, владелец | нет | `204` |
| `POST` | `/podcasts/{podcastId}/publish` | роль `author`, владелец | нет | `PodcastDetailResponse` |
| `POST` | `/podcasts/{podcastId}/progress` | JWT | `SaveProgressRequest` | `204` |
| `GET` | `/podcasts/{podcastId}/transcript` | публичный | нет | `PodcastTranscriptResponse` |
| `GET` | `/podcasts/{podcastId}/summary` | публичный | нет | `PodcastSummaryResponse` |
| `POST` | `/podcasts/{podcastId}/vote` | JWT | `VoteRequest` | `VoteResponse` |
| `DELETE` | `/podcasts/{podcastId}/vote` | JWT | нет | `VoteResponse` |
| `GET` | `/playlists` | публичный, токен опционален | нет | `PageOfPlaylistCard` |
| `POST` | `/playlists` | JWT | `CreatePlaylistRequest` | `PlaylistDetailResponse` |
| `GET` | `/playlists/{playlistId}` | публичный или владелец | нет | `PlaylistDetailResponse` |
| `PUT` | `/playlists/{playlistId}` | владелец | `UpdatePlaylistRequest` | `PlaylistDetailResponse` |
| `DELETE` | `/playlists/{playlistId}` | владелец | нет | `204` |
| `POST` | `/playlists/{playlistId}/save` | JWT | нет | `PlaylistSaveResponse` |
| `DELETE` | `/playlists/{playlistId}/save` | JWT | нет | `PlaylistSaveResponse` |
| `POST` | `/playlists/{playlistId}/podcasts` | владелец | `AddPodcastToPlaylistRequest` | `PlaylistDetailResponse` |
| `DELETE` | `/playlists/{playlistId}/podcasts/{podcastId}` | владелец | нет | `204` |
| `PUT` | `/playlists/{playlistId}/podcasts/reorder` | владелец | `ReorderPlaylistRequest` | `PlaylistDetailResponse` |
| `POST` | `/playlists/{playlistId}/vote` | JWT | `VoteRequest` | `VoteResponse` |
| `DELETE` | `/playlists/{playlistId}/vote` | JWT | нет | `VoteResponse` |
| `POST` | `/authors/{authorId}/subscribe` | JWT | нет | `AuthorSubscriptionResponse` |
| `DELETE` | `/authors/{authorId}/subscribe` | JWT | нет | `AuthorSubscriptionResponse` |
| `GET` | `/search/suggest` | публичный | нет | массив `SearchSuggestItem` |
| `GET` | `/search` | публичный, токен опционален | нет | `SearchResponse` |

## Параметры списков

| Endpoint | Query параметры |
|---|---|
| `GET /podcasts` | `q`, `categoryId`, `authorId`, `sort`, `page`, `size` |
| `GET /authors` | `q`, `sort`, `page`, `size` |
| `GET /authors/me/podcasts` | `status`, `q`, `sort`, `page`, `size` |
| `GET /authors/{authorId}/podcasts` | `q`, `sort`, `page`, `size` |
| `GET /playlists` | `q`, `sort`, `page`, `size` |
| `GET /users/me/playlists` | `page`, `size` |
| `GET /users/me/liked-podcasts` | `sort`, `page`, `size` |
| `GET /users/me/library/playlists` | `page`, `size` |
| `GET /authors/{authorId}/playlists` | `page`, `size` |
| `GET /users/me/subscriptions` | `page`, `size` |
| `GET /users/me/subscriptions/feed` | `sort`, `page`, `size` |
| `GET /users/me/history` | `page`, `size` |
| `GET /search` | `q`, `type`, `categoryId`, `sort`, `page`, `size` |
| `GET /search/suggest` | `q` |

## Пользовательские профили

### `GET /users/me/profile`

Возвращает приватный профиль текущего пользователя. Требуется JWT.

```bash
curl -H "Authorization: Bearer ${TOKEN}" \
  http://localhost:8082/podcast/v1/users/me/profile
```

Коды: `200`, `401`, `404`.

### `PUT /users/me/profile`

Обновляет username и avatar URL текущего пользователя.

```json
{
  "username": "dev-user-updated",
  "avatarUrl": "https://cdn.example.local/users/dev.png"
}
```

Коды: `200`, `400`, `401`, `404`, `409`.

### `GET /users/me/settings`

Возвращает настройки интерфейса: `theme`, `language`. Коды: `200`, `401`, `404`.

### `PUT /users/me/settings`

```json
{
  "theme": "LIGHT",
  "language": "RU"
}
```

Коды: `200`, `400`, `401`, `404`.

### `GET /users/me/liked-podcasts`

Возвращает опубликованные подкасты, которым текущий пользователь поставил лайк. Сортировка `DATE_DESC` и `DATE_ASC` применяется к дате лайка.

```bash
curl -H "Authorization: Bearer ${TOKEN}" \
  "http://localhost:8082/podcast/v1/users/me/liked-podcasts?sort=DATE_DESC&page=1&size=20"
```

Коды: `200`, `400`, `401`.

### `GET /users/me/library/playlists`

Возвращает чужие публичные плейлисты, сохранённые пользователем в личной библиотеке.

```bash
curl -H "Authorization: Bearer ${TOKEN}" \
  "http://localhost:8082/podcast/v1/users/me/library/playlists?page=1&size=20"
```

Коды: `200`, `401`.

## Авторы

### `GET /authors`

Публичный список авторов. JWT опционален: при наличии токена `isSubscribed` рассчитывается для текущего пользователя.

Query параметры:

| Параметр | Значения |
|---|---|
| `q` | поиск по имени автора и описанию |
| `sort` | `POPULAR`, `SUBSCRIBERS`, `DATE_DESC` |
| `page`, `size` | стандартная пагинация |

Коды: `200`, `400`.

### `POST /authors/me`

Запускает становление текущего пользователя автором. Требуется обычный JWT пользователя.

Podcast-service передаёт текущий `Authorization: Bearer <access_token>` в auth-service, вызывает `POST /auth/me/update-roles` с ролью `author`, получает новый access token и после успешного ответа создаёт или возвращает локальный author profile. Повторный вызов идемпотентен.

```json
{
  "authorName": "Backend Kitchen",
  "description": "Практические выпуски о Java и сервисах"
}
```

Ответ:

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 1800,
  "author_profile": {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "authorName": "Backend Kitchen",
    "avatarUrl": null,
    "description": "Практические выпуски о Java и сервисах",
    "subscribersCount": 0,
    "isSubscribed": false,
    "createdAt": "2026-05-31T10:00:00Z"
  }
}
```

Коды: `200`, `201`, `400`, `401`, `403`, `404`, `502`.

### `GET /authors/me`

Возвращает авторский профиль текущего пользователя. Требуется валидный JWT. Если локальный профиль автора ещё не создан, возвращается `404`. Коды: `200`, `401`, `404`.

### `PUT /authors/me`

Обновляет авторский профиль текущего пользователя. Требуется роль `author`.

```json
{
  "authorName": "Backend Kitchen Updated",
  "description": "Новые выпуски каждую неделю"
}
```

Коды: `200`, `400`, `401`, `403`, `404`, `409`.

### `GET /authors/me/podcasts`

Возвращает все подкасты текущего автора, включая `DRAFT`, `UPLOADING`, `PROCESSING`, `FAILED`, `PUBLISHED` и `ARCHIVED`.

Query параметры:

| Параметр | Значения |
|---|---|
| `status` | значение `PodcastStatus` |
| `q` | поиск по названию и описанию |
| `sort` | `DATE_DESC`, `DATE_ASC`, `RATING`, `VIEWS` |
| `page`, `size` | стандартная пагинация |

Коды: `200`, `400`, `401`, `403`, `404 AUTHOR_PROFILE_NOT_FOUND`.

### `GET /authors/{authorId}`

Публичная страница автора. При наличии JWT поле `isSubscribed` рассчитывается для текущего пользователя. Коды: `200`, `404`.

### `GET /authors/{authorId}/podcasts`

Публичный список подкастов автора с фильтрацией `q` и сортировкой. Коды: `200`, `400`, `404`.

### `GET /authors/{authorId}/playlists`

Публичные плейлисты автора. Коды: `200`, `400`, `404`.

## Категории

### `GET /categories`

Возвращает категории по `position`.

```bash
curl http://localhost:8082/podcast/v1/categories
```

Коды: `200`.

### `POST /categories`

Создаёт категорию. Требуется роль `admin`.

```json
{
  "name": "Security",
  "position": 90
}
```

Коды: `201`, `400`, `401`, `403`, `409`.

### `PUT /categories/{categoryId}`

Обновляет категорию. Требуется роль `admin`. Коды: `200`, `400`, `401`, `403`, `404`, `409`.

### `DELETE /categories/{categoryId}`

Удаляет категорию. Требуется роль `admin`. Если категория используется бизнес-правилами, возвращается `422` или `409` в зависимости от причины. Коды: `204`, `401`, `403`, `404`, `422`.

## Подкасты и медиа

### `GET /podcasts`

Каталог опубликованных подкастов. JWT опционален.

```bash
curl "http://localhost:8082/podcast/v1/podcasts?q=Kafka&sort=DATE_DESC&page=1&size=20"
```

Коды: `200`, `400`.

### `POST /podcasts`

Создаёт черновик подкаста. Требуется роль `author` и существующий author profile.

```json
{
  "title": "Kafka в production",
  "description": "Разбор topic, consumer group и DLT",
  "categoryId": "48b67732-5676-36bd-a97f-44d01de91376",
  "coverImageUrl": "https://cdn.example.local/covers/kafka.png",
  "num_speakers": 2
}
```

`num_speakers` является обязательным целым числом в диапазоне от `1` до `32`. Значения `0`, отрицательные значения, пропущенное поле и нечисловой тип возвращают `400 VALIDATION_ERROR`.

Коды: `201`, `400`, `401`, `403`, `404`, `409`.

### `GET /podcasts/{podcastId}`

Возвращает детали выпуска. Неопубликованный выпуск доступен владельцу с JWT. Ответ содержит `num_speakers`, а также файловые поля `audio_url_file` и `audio_size_file`, если они заполнены на стороне медиа-обработки или seed data.

```bash
curl http://localhost:8082/podcast/v1/podcasts/22222222-2222-2222-2222-222222222222
```

```json
{
  "id": "22222222-2222-2222-2222-222222222222",
  "title": "Kafka в production",
  "author": {
    "id": "11111111-1111-1111-1111-111111111111",
    "authorName": "Backend Kitchen",
    "avatarUrl": "https://cdn.example.local/authors/backend.png",
    "subscribersCount": 1280,
    "isSubscribed": null
  },
  "category": {
    "id": "48b67732-5676-36bd-a97f-44d01de91376",
    "name": "Technology",
    "position": 10
  },
  "coverImageUrl": "https://cdn.example.local/covers/kafka.png",
  "durationSeconds": 2580,
  "num_speakers": 2,
  "status": "PUBLISHED",
  "viewsCount": 4200,
  "likesCount": 340,
  "dislikesCount": 4,
  "publishedAt": "2026-05-20T10:00:00Z",
  "createdAt": "2026-05-19T10:00:00Z",
  "currentUserVote": null,
  "progressSeconds": null,
  "progressPercent": null,
  "description": "Разбор topic, consumer group и DLT",
  "audioUrl": "https://cdn.example.local/audio/kafka.mp3",
  "audio_url_file": "/dev/audio/podcast-1.mp3",
  "audio_size_file": 78000000,
  "hasTranscript": true,
  "hasSummary": true
}
```

Коды: `200`, `403`, `404`.

### `GET /podcasts/{podcastId}/speakers`

Публичный service-to-service endpoint. Возвращает количество спикеров для существующего подкаста в любом статусе; JWT токен не требуется.

```bash
curl http://localhost:8082/podcast/v1/podcasts/22222222-2222-2222-2222-222222222222/speakers
```

```json
{
  "podcastId": "22222222-2222-2222-2222-222222222222",
  "num_speakers": 2
}
```

Коды: `200`, `404`.

### `PUT /podcasts/{podcastId}`

Обновляет метаданные выпуска. Требуется роль `author` и владение выпуском. Коды: `200`, `400`, `401`, `403`, `404`, `422`.

### `DELETE /podcasts/{podcastId}`

Архивирует или удаляет выпуск согласно бизнес-правилам сервиса. Требуется роль `author` и владение. Коды: `204`, `401`, `403`, `404`.

### `POST /podcasts/{podcastId}/publish`

Переводит выпуск в публикационный flow. Требуется роль `author` и владение. Публикация доступна только для подкаста в статусе `PROCESSED`, с непустым `audioUrl` и положительным `durationSeconds`. Коды: `202`, `401`, `403`, `404`, `422`.

### `GET /podcasts/{podcastId}/transcript`

Возвращает транскрипт выпуска. Коды: `200`, `404`.

### `GET /podcasts/{podcastId}/summary`

Возвращает краткое содержание выпуска. Коды: `200`, `404`.

### `POST /podcasts/{podcastId}/progress`

Сохраняет прогресс прослушивания текущего пользователя.

```json
{
  "progressSeconds": 640
}
```

Коды: `204`, `400`, `401`, `404`, `422`.

## Плейлисты

### `GET /playlists`

Возвращает публичные плейлисты. JWT опционален. Коды: `200`, `400`.

### `POST /playlists`

Создаёт плейлист текущего пользователя.

```json
{
  "title": "Backend essentials",
  "description": "Подборка выпусков для backend-разработчика",
  "coverImageUrl": "https://cdn.example.local/playlists/backend.png",
  "isPublic": true
}
```

Коды: `201`, `400`, `401`, `404`.

### `GET /playlists/{playlistId}`

Публичный плейлист доступен всем. Приватный плейлист доступен владельцу. Коды: `200`, `403`, `404`.

### `PUT /playlists/{playlistId}`

Обновляет плейлист владельца. Коды: `200`, `400`, `401`, `403`, `404`.

### `DELETE /playlists/{playlistId}`

Удаляет плейлист владельца. Коды: `204`, `401`, `403`, `404`.

### `POST /playlists/{playlistId}/save`

Сохраняет чужой публичный плейлист в библиотеку текущего пользователя.

```json
{
  "playlistId": "33333333-3333-3333-3333-333333333333",
  "isSaved": true
}
```

Коды: `200`, `401`, `403 CANNOT_SAVE_OWN_PLAYLIST`, `404 PLAYLIST_NOT_FOUND`, `409 ALREADY_SAVED`.

### `DELETE /playlists/{playlistId}/save`

Удаляет плейлист из библиотеки текущего пользователя. Повторный вызов для доступного плейлиста возвращает `isSaved=false`.

```json
{
  "playlistId": "33333333-3333-3333-3333-333333333333",
  "isSaved": false
}
```

Коды: `200`, `401`, `404 PLAYLIST_NOT_FOUND`.

### `POST /playlists/{playlistId}/podcasts`

Добавляет опубликованный подкаст в плейлист владельца.

```json
{
  "podcastId": "22222222-2222-2222-2222-222222222222"
}
```

Коды: `200`, `400`, `401`, `403`, `404`, `409`, `422`.

### `DELETE /playlists/{playlistId}/podcasts/{podcastId}`

Удаляет выпуск из плейлиста владельца. Коды: `204`, `401`, `403`, `404`.

### `PUT /playlists/{playlistId}/podcasts/reorder`

Меняет порядок выпусков.

```json
{
  "items": [
    {
      "podcastId": "22222222-2222-2222-2222-222222222222",
      "position": 1
    }
  ]
}
```

Коды: `200`, `400`, `401`, `403`, `404`, `422`.

### `GET /users/me/playlists`

Возвращает плейлисты текущего пользователя. Требуется JWT. Коды: `200`, `400`, `401`.

### `GET /authors/{authorId}/playlists`

Возвращает публичные плейлисты автора. Коды: `200`, `400`, `404`.

## Голоса

### `POST /podcasts/{podcastId}/vote`

Создаёт или меняет голос за подкаст.

```json
{
  "voteType": "LIKE"
}
```

Коды: `200`, `400`, `401`, `404`, `422`.

### `DELETE /podcasts/{podcastId}/vote`

Удаляет голос за подкаст и возвращает актуальные счётчики. Коды: `200`, `401`, `404`.

### `POST /playlists/{playlistId}/vote`

Создаёт или меняет голос за плейлист. Коды: `200`, `400`, `401`, `404`.

### `DELETE /playlists/{playlistId}/vote`

Удаляет голос за плейлист и возвращает актуальные счётчики. Коды: `200`, `401`, `404`.

## Подписки

### `GET /users/me/subscriptions`

Возвращает подписки текущего пользователя. Коды: `200`, `400`, `401`.

### `GET /users/me/subscriptions/feed`

Возвращает ленту опубликованных подкастов авторов, на которых подписан текущий пользователь. Коды: `200`, `400`, `401`.

### `POST /authors/{authorId}/subscribe`

Подписывает текущего пользователя на автора. Самоподписка блокируется на уровне БД trigger. Коды: `200`, `401`, `404`, `422`.

### `DELETE /authors/{authorId}/subscribe`

Отписывает текущего пользователя от автора. Коды: `200`, `401`, `404`.

## История прослушивания

### `GET /users/me/history`

Возвращает историю прослушивания текущего пользователя. Коды: `200`, `400`, `401`.

## Поиск

### `GET /search/suggest`

Возвращает подсказки для строки `q`.

```bash
curl "http://localhost:8082/podcast/v1/search/suggest?q=Kafka"
```

Коды: `200`, `400`.

### `GET /search`

Возвращает результаты полнотекстового поиска.

```bash
curl "http://localhost:8082/podcast/v1/search?q=Kafka&type=ALL&sort=RELEVANCE&page=1&size=10"
```

Коды: `200`, `400`.

## Известные расхождения контракта и реализации

| Область | Контракт | Реализация |
|---|---|---|
| `DELETE /podcasts/{podcastId}/vote` | допускается `204`, если голос отсутствовал | контроллер возвращает `200 OK` с `VoteResponse` |
