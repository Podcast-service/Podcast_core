# Локальная разработка

## Требования

| Инструмент | Версия |
|---|---|
| Java | 21 |
| Docker | актуальная стабильная |
| Docker Compose | plugin v2 |

## Запуск

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

Проверка:

```bash
curl http://localhost:8082/podcast/v1/actuator/health
curl http://localhost:8082/podcast/v1/categories
```

Swagger:

```text
http://localhost:8082/podcast/v1/swagger
```

## Основные URL

| Сервис | URL |
|---|---|
| API | `http://localhost:8082/podcast/v1` |
| Swagger | `http://localhost:8082/podcast/v1/swagger` |
| Kafka UI | `http://localhost:8081` |
| PostgreSQL | `localhost:5432` |
