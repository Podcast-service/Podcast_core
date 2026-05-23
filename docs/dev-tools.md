# Dev tools

## JWT scripts

Dev JWT scripts находятся в `.dev-tools/jwt`. Они выпускают локальные access tokens, совместимые с текущим JWT validation layer `podcast-core`.

| Скрипт | Роли | Файл вывода |
|---|---|---|
| `.dev-tools/jwt/generate-user-token.*` | `user` | `.dev/jwt/user-token.txt` |
| `.dev-tools/jwt/generate-author-token.*` | `user`, `author` | `.dev/jwt/author-token.txt` |
| `.dev-tools/jwt/generate-admin-token.*` | `user`, `author`, `admin` | `.dev/jwt/admin-token.txt` |

Dev пользователь:

```json
{
  "id": "00000000-0000-0000-0000-000000000001",
  "email": "dev.user@example.local",
  "username": "dev-user"
}
```

Профиль пользователя создаётся dev seed при активном профиле `dev`.

## Usage

Windows PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\.dev-tools\jwt\generate-user-token.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\.dev-tools\jwt\generate-author-token.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\.dev-tools\jwt\generate-admin-token.ps1
```

Linux/macOS:

```bash
./.dev-tools/jwt/generate-user-token.sh
./.dev-tools/jwt/generate-author-token.sh
./.dev-tools/jwt/generate-admin-token.sh
```

Raw token mode:

```bash
./.dev-tools/jwt/generate-author-token.sh --raw
```

PowerShell raw token mode:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\.dev-tools\jwt\generate-author-token.ps1 -Raw
```

## Authorization header

```http
Authorization: Bearer <token>
```

## curl examples

```bash
TOKEN="$(./.dev-tools/jwt/generate-user-token.sh --raw)"
curl -H "Authorization: Bearer ${TOKEN}" http://localhost:8082/podcast/v1/users/me/profile
```

```bash
TOKEN="$(./.dev-tools/jwt/generate-author-token.sh --raw)"
curl -X POST http://localhost:8082/podcast/v1/podcasts \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"title":"Dev podcast","description":"Local episode","categoryId":null,"coverImageUrl":null}'
```

```bash
TOKEN="$(./.dev-tools/jwt/generate-admin-token.sh --raw)"
curl -X POST http://localhost:8082/podcast/v1/categories \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"name":"Dev Category","position":99}'
```

## Local Kafka tooling

Docker Compose поднимает Kafka UI:

```text
http://localhost:8081
```

Kafka UI используется для просмотра topic `podcasts.users`, consumer group `podcast-service` и DLT topic `podcasts.users.DLT`.

## Local DB tooling

PostgreSQL публикуется на port из `.env` `PODCAST_DB_PORT`, default `5432`.

```bash
docker compose exec postgres psql -U podcast_user -d podcast_db
```

## Dev seed data

Spring profile `dev` активирует `DevDataSeeder`. Локальная БД получает наполненный dataset для frontend-разработки: пользователи, авторы, категории, подкасты в разных статусах, transcript/summary, плейлисты, подписки, голоса и история прослушивания. Default JWT scripts используют пользователя `dev-user`, который входит в этот dataset.

## Debugging

```bash
docker compose logs -f app
docker compose logs -f kafka
docker compose ps
```

Local runtime files:

| Path | Назначение |
|---|---|
| `.dev/dev-token.txt` | Legacy dev token output |
| `.dev/jwt/*.txt` | Role-specific dev token output |
| `.env` | Local compose environment |

`.dev/` и `.env` игнорируются Git.
