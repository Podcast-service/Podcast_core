# Аутентификация и авторизация

## JWT

Сервис использует JWT access token от сервиса аутентификации. Токен передаётся в header:

```http
Authorization: Bearer <access_token>
```

Параметры проверки:

| Параметр | Значение |
|---|---|
| Алгоритм | `HS256` |
| Secret | `PODCAST_ACCESS_TOKEN_SECRET` |
| Issuer | `PODCAST_ACCESS_TOKEN_ISSUER`, значение по умолчанию `auth-service` |
| Expiration | обязательный claim `exp` |
| User id | обязательный claim `user_id` |
| Email | обязательный claim `email` |
| Roles | массив строк в claim `roles` |

Пример полезной нагрузки:

```json
{
  "iss": "auth-service",
  "sub": "00000000-0000-0000-0000-000000000001",
  "user_id": "00000000-0000-0000-0000-000000000001",
  "email": "dev.user@example.local",
  "roles": ["user", "author"],
  "iat": 1779540000,
  "nbf": 1779540000,
  "exp": 1779626400
}
```

## Роли

| Роль в JWT | Spring authority | Назначение |
|---|---|---|
| `user` | `ROLE_USER` | пользовательские операции |
| `author` | `ROLE_AUTHOR` | авторский профиль и подкасты |
| `admin` | `ROLE_ADMIN` | управление категориями |

## Публичные HTTP операции

Публичные GET-операции доступны без токена. Если токен передан, ответ может включать персональные поля: `currentUserVote`, `progressSeconds`, `progressPercent`, `isSubscribed`.

## Swagger authorization

В Swagger UI используется кнопка Authorize. Значение вводится без префикса `Bearer`, Swagger добавляет схему сам.

Пример ручного запроса:

```bash
curl -H "Authorization: Bearer ${TOKEN}" \
  http://localhost:8082/podcast/v1/users/me/profile
```

## Dev JWT tooling

Скрипты находятся в `.dev-tools/jwt/` и создают токены для одного dev-пользователя:

```json
{
  "id": "00000000-0000-0000-0000-000000000001",
  "email": "dev.user@example.local",
  "username": "dev-user"
}
```

Роли:

| Скрипт | Роли |
|---|---|
| `generate-user-token.*` | `user` |
| `generate-author-token.*` | `user`, `author` |
| `generate-admin-token.*` | `user`, `author`, `admin` |
