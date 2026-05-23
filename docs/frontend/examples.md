# Примеры для frontend

## Каталог с токеном

```bash
curl "http://localhost:8082/podcast/v1/podcasts?page=1&size=20" \
  -H "Authorization: Bearer ${TOKEN}"
```

## Голосование

```bash
curl -X POST http://localhost:8082/podcast/v1/podcasts/22222222-2222-2222-2222-222222222222/vote \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"voteType":"LIKE"}'
```

## Обработка ошибки

```json
{
  "code": "FORBIDDEN",
  "message": "You don't have permission to access this resource",
  "timestamp": "2026-05-23T10:00:00Z"
}
```

Клиент ориентируется на `code`, а текст `message` использует как техническое сообщение или маппит на локализованный UI-текст.
