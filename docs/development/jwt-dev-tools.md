# Dev JWT-инструменты

Скрипты находятся в `.dev-tools/jwt/` и создают локальные JWT для Swagger и curl.

## Скрипты

| Скрипт | Роли |
|---|---|
| `generate-user-token.ps1` / `.sh` | `user` |
| `generate-author-token.ps1` / `.sh` | `user`, `author` |
| `generate-admin-token.ps1` / `.sh` | `user`, `author`, `admin` |

## PowerShell

```powershell
$env:PODCAST_ACCESS_TOKEN_SECRET = "dev-access-token-secret-change-me"
.\.dev-tools\jwt\generate-author-token.ps1
```

## Bash

```bash
export PODCAST_ACCESS_TOKEN_SECRET="dev-access-token-secret-change-me"
./.dev-tools/jwt/generate-author-token.sh
```

Токены сохраняются в `.dev/jwt/`, каталог игнорируется Git.
