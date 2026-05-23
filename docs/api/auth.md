# Auth

## Overview

`podcast-core` использует stateless JWT authentication. Access token выпускается внешним `auth-service`, а `podcast-core` валидирует подпись, issuer, срок действия и роли.

Защищённые запросы передают токен в HTTP header:

```http
Authorization: Bearer <access_token>
```

## JWT validation

| Параметр | Значение |
|---|---|
| Algorithm | `HS256` |
| Signature | HMAC SHA-256 |
| Secret | `PODCAST_ACCESS_TOKEN_SECRET` |
| Issuer | `PODCAST_ACCESS_TOKEN_ISSUER`, default `auth-service` |
| Audience | Не валидируется текущим кодом |
| Expiration | Claim `exp`, Unix timestamp seconds |
| Not before | Claim `nbf`, опционально |
| Clock skew | 30 секунд |

JWT header:

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

JWT payload:

```json
{
  "user_id": "00000000-0000-0000-0000-000000000001",
  "email": "dev.user@example.local",
  "roles": ["user", "author"],
  "iss": "auth-service",
  "exp": 1790000000,
  "nbf": 1789990000
}
```

`nbf` опционален. Дополнительные claims не используются security layer.

## Claims

| Claim | Тип | Использование |
|---|---|---|
| `user_id` | string UUID | Principal id, маппится в `AuthenticatedUser.userId()` |
| `email` | string | Маппится в `AuthenticatedUser.email()` |
| `roles` | array of string | Преобразуется в Spring authorities |
| `iss` | string | Сравнивается с `PODCAST_ACCESS_TOKEN_ISSUER` |
| `exp` | integer | Срок действия токена |
| `nbf` | integer | Начало действия токена, если claim присутствует |
| `aud` | any | Не используется текущей реализацией |

> Требует уточнения: audience policy для production access token.

## Roles and authorities

Роли из JWT нормализуются в uppercase и получают prefix `ROLE_`, если prefix отсутствует.

| JWT role | Spring authority |
|---|---|
| `user` | `ROLE_USER` |
| `author` | `ROLE_AUTHOR` |
| `admin` | `ROLE_ADMIN` |
| `ROLE_ADMIN` | `ROLE_ADMIN` |

## Permissions

| Permission level | Правила |
|---|---|
| Public | Категории, публичные подкасты, публичные плейлисты, публичные авторы, поиск |
| Optional token | Публичные GET-ручки с пользовательским контекстом: `currentUserVote`, `progressSeconds`, `isSubscribed` |
| Authenticated | Профиль, настройки, плейлисты пользователя, подписки, история, progress, votes |
| `ROLE_AUTHOR` | `/authors/me`, создание/обновление/архивация/публикация подкастов |
| `ROLE_ADMIN` | Управление категориями |

## Error behavior

| Сценарий | HTTP status | Error code |
|---|---:|---|
| Нет token на protected endpoint | `401` | `UNAUTHORIZED` |
| Неверная Bearer scheme | `401` | `UNAUTHORIZED` |
| Неверная подпись | `401` | `UNAUTHORIZED` |
| Неверный issuer | `401` | `UNAUTHORIZED` |
| Истёкший token | `401` | `UNAUTHORIZED` |
| Token с неподдерживаемым algorithm | `401` | `UNAUTHORIZED` |
| Недостаточная роль | `403` | `FORBIDDEN` |

## Dev JWT tooling

Dev scripts находятся в `.dev-tools/jwt`.

| Скрипт | Роли | Назначение |
|---|---|---|
| `generate-user-token.*` | `user` | Проверка пользовательских ручек |
| `generate-author-token.*` | `user`, `author` | Проверка пользовательских и авторских ручек |
| `generate-admin-token.*` | `user`, `author`, `admin` | Проверка пользовательских, авторских и admin-ручек |

Dev пользователь:

```json
{
  "id": "00000000-0000-0000-0000-000000000001",
  "email": "dev.user@example.local",
  "username": "dev-user"
}
```

Windows PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\.dev-tools\jwt\generate-author-token.ps1
```

Linux/macOS:

```bash
./.dev-tools/jwt/generate-author-token.sh
```

Пример curl:

```bash
TOKEN="$(./.dev-tools/jwt/generate-author-token.sh --raw)"
curl -H "Authorization: Bearer ${TOKEN}" http://localhost:8082/podcast/v1/users/me/profile
```

Generated token files сохраняются в `.dev/jwt/`, которая игнорируется Git. Секрет для подписи берётся из `PODCAST_ACCESS_TOKEN_SECRET`; default dev value совпадает с `.env.example`.
