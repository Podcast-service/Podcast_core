# Интеграция frontend и mobile

## Base URL

```text
http://localhost:8082/podcast/v1
```

Production URL задаётся инфраструктурой reverse proxy.

## Общие правила клиента

| Правило | Описание |
|---|---|
| JSON | все request/response body используют JSON |
| JWT | защищённые запросы отправляют `Authorization: Bearer <token>` |
| Публичные списки | могут работать без токена |
| Персонализация | публичные списки с токеном возвращают пользовательские поля |
| Ошибки | клиент читает `code` и `message` из `ApiErrorResponse` |

## Основные экраны

| Экран | Endpoints |
|---|---|
| Главная лента | `GET /podcasts`, `GET /playlists` |
| Поиск | `GET /search`, `GET /search/suggest` |
| Выпуск | `GET /podcasts/{podcastId}`, `GET /podcasts/{podcastId}/speakers`, transcript, summary, progress |
| Плейлист | `GET /playlists/{playlistId}` |
| Профиль автора | `GET /authors/{authorId}`, podcasts, playlists |
| Личный кабинет | `/users/me/profile`, settings, playlists, subscriptions, history |
