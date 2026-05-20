# Конфигурация

## Переменные окружения

| Переменная | Default | Назначение |
|---|---|---|
| `SERVER_PORT` | `8082` | HTTP port приложения |
| `DB_URL` | `jdbc:postgresql://localhost:5432/podcast_db` | JDBC URL |
| `DB_USERNAME` | `podcast_user` | Пользователь БД |
| `DB_PASSWORD` | `podcast_pass` | Пароль БД |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `KAFKA_CONSUMER_GROUP` | `podcast-service` | Consumer group |
| `KAFKA_TOPIC_USERS` | `podcasts.users` | Топик пользовательских событий |
| `KAFKA_TOPIC_PODCASTS` | `podcasts.podcasts` | Топик подкаст-событий, сейчас не используется доменным кодом |
| `AUTH_ENABLED` | `true` | Включает JWT validation |
| `ACCESS_TOKEN_SECRET` | пусто | Секрет подписи JWT; в production обязателен |
| `ACCESS_TOKEN_ISSUER` | `auth-service` | Ожидаемый issuer токена |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173,http://localhost:8082` | Разрешённые browser origins |
| `DEV_SEED_ENABLED` | `true` в профиле `dev` | Включает dev seed |
| `POSTGRES_DB` | `podcast_db` в compose | Имя БД для docker compose |
| `POSTGRES_USER` | `podcast_user` в compose | Пользователь БД для docker compose |
| `POSTGRES_PASSWORD` | `podcast_pass` в compose | Пароль БД для docker compose |

## Spring profiles

| Profile | Где используется | Что меняет |
|---|---|---|
| default | Локальный запуск без Docker | PostgreSQL/Kafka на `localhost` |
| `docker` | Docker Compose | PostgreSQL host `postgres`, Kafka host `kafka` |
| `dev` | Docker Compose и локальная разработка | Включает `DevDataSeeder` |

В `docker-compose.yml` приложение запускается с:

```yaml
SPRING_PROFILES_ACTIVE: docker,dev
```

## Database config

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/podcast_db}
    username: ${DB_USERNAME:podcast_user}
    password: ${DB_PASSWORD:podcast_pass}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: none
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

Hibernate не создаёт схему. Все изменения БД должны идти через Flyway migrations.

## Kafka config

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: ${KAFKA_CONSUMER_GROUP:podcast-service}
      auto-offset-reset: earliest
```

Доменные topic names лежат в `app.kafka.topics`. Routing сейчас содержит:

```yaml
app:
  kafka:
    routing:
      user.created: users
```

## JWT config

```yaml
app:
  auth:
    jwt:
      enabled: ${AUTH_ENABLED:true}
      secret: ${ACCESS_TOKEN_SECRET:}
      issuer: ${ACCESS_TOKEN_ISSUER:auth-service}
```

Production-требование: `ACCESS_TOKEN_SECRET` должен приходить из secret storage. Не хранить production secret в Git.

## CORS

```yaml
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173,http://localhost:8082}
```

Разрешены методы `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`. Credentials выключены. Wildcard `*` не используется.

## Springdoc/Swagger

```yaml
springdoc:
  swagger-ui:
    path: /swagger
    url: /openapi/podcast-service-dev.yaml
```

Swagger использует dev-спецификацию из static resources, а не `openapi-2.yaml`.

## Feature flags

| Flag | Где | Default | Описание |
|---|---|---|---|
| `app.dev-seed.enabled` / `DEV_SEED_ENABLED` | `application-dev.yaml` | `true` | Включает сидирование dev-данных |
| `app.auth.jwt.enabled` / `AUTH_ENABLED` | `application.yaml` | `true` | Включает JWT-аутентификацию |

TODO: уточнить, допустимо ли выключать `AUTH_ENABLED=false` вне локальной разработки.
