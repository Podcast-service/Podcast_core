# Пагинация

Пагинация применяется к спискам подкастов, плейлистов, подписок, истории и результатам поиска.

## Query параметры

| Параметр | Тип | Default | Описание |
|---|---|---:|---|
| `page` | integer | `1` | номер страницы |
| `size` | integer | `20` | размер страницы |

## Формат ответа

```json
{
  "items": [],
  "meta": {
    "page": 1,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

## Клиентское поведение

Клиент хранит `page` как единично-индексированное значение. При изменении фильтра или сортировки клиент возвращается на `page=1`. Пустая страница возвращается как `items: []` с корректным `meta`.

## HTTP операции с пагинацией

| Endpoint | Модель элементов |
|---|---|
| `GET /podcasts` | `PodcastCard` |
| `GET /authors/{authorId}/podcasts` | `PodcastCard` |
| `GET /playlists` | `PlaylistCard` |
| `GET /users/me/playlists` | `PlaylistCard` |
| `GET /authors/{authorId}/playlists` | `PlaylistCard` |
| `GET /users/me/subscriptions` | `SubscriptionResponse` |
| `GET /users/me/subscriptions/feed` | `PodcastCard` |
| `GET /users/me/history` | `ListenHistoryItem` |
| `GET /search` | page внутри `podcasts`, `authors`, `playlists` |
