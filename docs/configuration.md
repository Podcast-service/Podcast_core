# Конфигурация

## Переменные окружения

Шаблон окружения для Docker Compose лежит в [`../.env.example`](../.env.example). Для локального или серверного запуска его обычно копируют в `.env`:

```bash
cp .env.example .env
```

Docker Compose автоматически читает `.env` из корня проекта и использует его для подстановки `${...}` в `docker-compose.yml`.

Реальный `.env` игнорируется Git. В репозитории хранится только `.env.example` без production secrets.

| Переменная | Default | Назначение |
|---|---|---|
| `COMPOSE_PROJECT_NAME` | `podcast-core` в `.env.example` | Имя compose-проекта и prefix ресурсов Docker |
| `SPRING_PROFILES_ACTIVE` | `docker,dev` в compose | Активные Spring profiles |
| `SERVER_PORT` | `8082` | HTTP port приложения |
| `SERVER_SERVLET_CONTEXT_PATH` | `/podcast/v1` | Base path REST API и infrastructure endpoints |
| `APP_PORT` | `8082` | Порт на host-машине для публикации приложения |
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
| `POSTGRES_PORT` | `5432` | Порт PostgreSQL на host-машине |
| `KAFKA_INTERNAL_PORT` | `9092` | Порт internal listener Kafka на host-машине |
| `KAFKA_EXTERNAL_PORT` | `9094` | Порт external listener Kafka |
| `KAFKA_EXTERNAL_HOST` | `host.docker.internal` | Host/IP, который Kafka отдаёт внешним клиентам в advertised listeners |
| `KAFKA_AUTO_CREATE_TOPICS_ENABLE` | `true` | Auto-create topics в локальном Kafka |
| `KAFKA_UI_PORT` | `8081` | Порт Kafka UI на host-машине |
| `SWAGGER_OPENAPI_URL` | `/podcast/v1/openapi/podcast-service-dev.yaml` | URL dev OpenAPI document для Swagger UI |

## Ключевые переменные для серверного окружения

| Переменная | Production-oriented значение |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Убрать `dev`, если не нужны тестовые данные |
| `ACCESS_TOKEN_SECRET` | Production secret из secret storage |
| `POSTGRES_PASSWORD` | Production пароль PostgreSQL |
| `CORS_ALLOWED_ORIGINS` | Указать реальные frontend домены, без wildcard |
| `KAFKA_EXTERNAL_HOST` | Домен/IP сервера для внешнего доступа к Kafka |
| `DEV_SEED_ENABLED` | Поставить `false` или не включать `dev` profile |

## Spring profiles

| Profile | Где используется | Что меняет |
|---|---|---|
| default | Локальный запуск без Docker | PostgreSQL/Kafka на `localhost` |
| `docker` | Docker Compose | PostgreSQL host `postgres`, Kafka host `kafka` |
| `dev` | Docker Compose и локальная разработка | Включает `DevDataSeeder` |

По умолчанию в `docker-compose.yml` приложение запускается с:

```yaml
SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-docker,dev}
```

Для сервера типичное значение:

```env
SPRING_PROFILES_ACTIVE=docker
DEV_SEED_ENABLED=false
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

Hibernate не создаёт схему. Изменения схемы БД проходят через Flyway migrations.

## HTTP base path

```yaml
server:
  port: ${SERVER_PORT:8082}
  servlet:
    context-path: ${SERVER_SERVLET_CONTEXT_PATH:/podcast/v1}
```

При default configuration локальный API доступен по base URL:

```text
http://localhost:8082/podcast/v1
```

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

Production-требование: `ACCESS_TOKEN_SECRET` поступает из secret storage. Production secret не хранится в Git.

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
    url: ${SWAGGER_OPENAPI_URL:/podcast/v1/openapi/podcast-service-dev.yaml}
```

Swagger использует dev-спецификацию из static resources, а не `openapi-2.yaml`.

## Feature flags

| Flag | Где | Default | Описание |
|---|---|---|---|
| `app.dev-seed.enabled` / `DEV_SEED_ENABLED` | `application-dev.yaml` | `true` | Включает сидирование dev-данных |
| `app.auth.jwt.enabled` / `AUTH_ENABLED` | `application.yaml` | `true` | Включает JWT-аутентификацию |

> Требует уточнения: политика использования `AUTH_ENABLED=false` за пределами локальной разработки.
