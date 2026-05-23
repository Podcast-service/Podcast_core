# Фильтрация и сортировка

## Подкасты

| Endpoint | Фильтры | Сортировки |
|---|---|---|
| `GET /podcasts` | `q`, `categoryId`, `authorId` | `DATE_DESC`, `DATE_ASC`, `RATING`, `VIEWS` |
| `GET /authors/{authorId}/podcasts` | `q` | `DATE_DESC`, `DATE_ASC`, `RATING`, `VIEWS` |
| `GET /users/me/subscriptions/feed` | подписки текущего пользователя | `DATE_DESC`, `DATE_ASC`, `RATING`, `VIEWS` |

## Плейлисты

| Endpoint | Фильтры | Сортировки |
|---|---|---|
| `GET /playlists` | `q`, только публичные плейлисты | `DATE_DESC`, `DATE_ASC`, `RATING` |
| `GET /authors/{authorId}/playlists` | автор | дата создания |
| `GET /users/me/playlists` | текущий пользователь | дата создания |

## Поиск

`GET /search` выполняет полнотекстовый поиск.

| Параметр | Значение |
|---|---|
| `q` | поисковая строка |
| `type` | `ALL`, `PODCAST`, `AUTHOR`, `PLAYLIST` |
| `categoryId` | ограничение по категории для подкастов |
| `sort` | `RELEVANCE`, `DATE`, `RATING`, `VIEWS` |

`GET /search/suggest` возвращает короткие подсказки для live-поиска.

## Невалидные значения

Невалидные enum values возвращают `400 VALIDATION_ERROR` с перечислением допустимых значений в `details.fields`.
