# Dev JWT scripts

Скрипты выпускают локальные JWT для ручной проверки `podcast-core` в dev-окружении. Токены подписываются HS256 секретом из `PODCAST_ACCESS_TOKEN_SECRET`; при отсутствии переменной используется локальный dev secret `dev-access-token-secret-change-me`.

## Dev пользователь

```json
{
  "id": "00000000-0000-0000-0000-000000000001",
  "email": "dev.user@example.local",
  "username": "dev-user"
}
```

Профиль этого пользователя создаётся dev seed при активном Spring profile `dev`.

## Скрипты

| Скрипт | Роли | Файл вывода |
|---|---|---|
| `generate-user-token.*` | `user` | `.dev/jwt/user-token.txt` |
| `generate-author-token.*` | `user`, `author` | `.dev/jwt/author-token.txt` |
| `generate-admin-token.*` | `user`, `author`, `admin` | `.dev/jwt/admin-token.txt` |

## Windows PowerShell

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\.dev-tools\jwt\generate-user-token.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\.dev-tools\jwt\generate-author-token.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\.dev-tools\jwt\generate-admin-token.ps1
```

## Linux/macOS

```bash
./.dev-tools/jwt/generate-user-token.sh
./.dev-tools/jwt/generate-author-token.sh
./.dev-tools/jwt/generate-admin-token.sh
```

## Authorization header

```http
Authorization: Bearer <token>
```

## Payload

```json
{
  "user_id": "00000000-0000-0000-0000-000000000001",
  "email": "dev.user@example.local",
  "roles": ["user", "author"],
  "iss": "auth-service",
  "exp": 1790000000
}
```

`aud` не используется текущей реализацией `podcast-core`.
