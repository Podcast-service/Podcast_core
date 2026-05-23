# Окружения

## Profiles

| Profile | Назначение |
|---|---|
| default | базовая конфигурация для локального запуска без Docker |
| `docker` | подключение к PostgreSQL и Kafka по именам Docker-сервисов |
| `dev` | dev seed data и Swagger UI |

Для локального Docker Compose используется:

```env
PODCAST_SPRING_PROFILES_ACTIVE=docker,dev
```

Для production окружения используется профиль без `dev`:

```env
PODCAST_SPRING_PROFILES_ACTIVE=docker
```

## Production-значения по умолчанию

Базовая конфигурация отключает Swagger endpoints через `PODCAST_SWAGGER_ENABLED=false`. Dev seed не активируется без Spring profile `dev`.
