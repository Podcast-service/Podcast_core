# Отладка

## Логи приложения

```bash
docker compose logs -f app
```

## Проверка БД

```bash
docker exec -it podcast_core-postgres-1 psql -U podcast_user -d podcast_db
```

Пример счётчика:

```sql
select count(*) from podcasts;
```

## Проверка Kafka

Kafka UI:

```text
http://localhost:8081
```

## Частые локальные симптомы

| Симптом | Причина | Проверка |
|---|---|---|
| `401` в Swagger | не передан JWT или secret не совпадает | новый token через `.dev-tools/jwt` |
| Swagger запрашивает двойной путь | неверный `PODCAST_SWAGGER_OPENAPI_URL` | значение `/openapi/podcast-service-dev.yaml` |
| пустые списки | profile `dev` не активен | `PODCAST_SPRING_PROFILES_ACTIVE=docker,dev` |
| Kafka consumer не стартует | Kafka недоступна | `docker compose ps`, Kafka UI |
