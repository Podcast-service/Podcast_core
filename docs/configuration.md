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
| `PODCAST_SPRING_PROFILES_ACTIVE` | `docker,dev` в compose | Активные Spring profiles |
| `PODCAST_SERVER_PORT` | `8082` | HTTP port приложения |
| `PODCAST_SERVER_SERVLET_CONTEXT_PATH` | `/podcast/v1` | Base path REST API и infrastructure endpoints |
| `PODCAST_APP_PORT` | `8082` | Порт на host-машине для публикации приложения |
| `PODCAST_DB_URL` | `jdbc:postgresql://localhost:5432/podcast_db` | JDBC URL |
| `PODCAST_DB_USER` | `podcast_user` | Пользователь БД |
| `PODCAST_DB_PASSWORD` | `podcast_pass` | Пароль БД |
| `PODCAST_DB` | `podcast_db` | Имя БД для docker compose |
| `PODCAST_DB_PORT` | `5432` | Порт PostgreSQL на host-машине |
| `PODCAST_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `PODCAST_KAFKA_CONSUMER_GROUP` | `podcast-service` | Consumer group |
| `PODCAST_KAFKA_TOPIC_USERS` | `podcasts.users` | Топик пользовательских событий |
| `PODCAST_KAFKA_TOPIC_PODCASTS` | `podcasts.podcasts` | Топик подкаст-событий, сейчас не используется доменным кодом |
| `PODCAST_KAFKA_INTERNAL_PORT` | `9092` | Порт internal listener Kafka на host-машине |
| `PODCAST_KAFKA_EXTERNAL_PORT` | `9094` | Порт external listener Kafka |
| `PODCAST_KAFKA_EXTERNAL_HOST` | `host.docker.internal` | Host/IP, который Kafka отдаёт внешним клиентам в advertised listeners |
| `PODCAST_KAFKA_AUTO_CREATE_TOPICS_ENABLE` | `true` | Auto-create topics в локальном Kafka |
| `PODCAST_KAFKA_UI_PORT` | `8081` | Порт Kafka UI на host-машине |
| `PODCAST_AUTH_ENABLED` | `true` | Включает JWT validation |
| `PODCAST_ACCESS_TOKEN_SECRET` | пусто | Секрет подписи JWT; в production обязателен |
| `PODCAST_ACCESS_TOKEN_ISSUER` | `auth-service` | Ожидаемый issuer токена |
| `PODCAST_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173,http://localhost:8082` | Разрешённые browser origins |
| `PODCAST_DEV_SEED_ENABLED` | `true` в профиле `dev` | Включает dev seed |
| `PODCAST_SWAGGER_ENABLED` | `true` в профиле `dev`, `false` по умолчанию | Включает SpringDoc OpenAPI/Swagger endpoints |
| `PODCAST_SWAGGER_OPENAPI_URL` | `/openapi/podcast-service-dev.yaml` | Servlet-relative URL dev OpenAPI document для Swagger UI |

## Ключевые переменные для серверного окружения

| Переменная | Production-oriented значение |
|---|---|
| `PODCAST_SPRING_PROFILES_ACTIVE` | `docker` |
| `PODCAST_ACCESS_TOKEN_SECRET` | Production secret из secret storage |
| `PODCAST_DB_PASSWORD` | Production пароль PostgreSQL |
| `PODCAST_CORS_ALLOWED_ORIGINS` | Реальные frontend домены, без wildcard |
| `PODCAST_KAFKA_EXTERNAL_HOST` | Домен/IP сервера для внешнего доступа к Kafka |
| `PODCAST_DEV_SEED_ENABLED` | `false` или профиль без `dev` |
| `PODCAST_SWAGGER_ENABLED` | `false`, если Swagger не публикуется наружу |

## Spring profiles

| Profile | Где используется | Что меняет |
|---|---|---|
| default | Локальный запуск без Docker | PostgreSQL/Kafka на `localhost` |
| `docker` | Docker Compose | PostgreSQL host `postgres`, Kafka host `kafka` |
| `dev` | Docker Compose и локальная разработка | Включает `DevDataSeeder` |

По умолчанию в `docker-compose.yml` приложение запускается с:

```yaml
--spring.profiles.active=${PODCAST_SPRING_PROFILES_ACTIVE:-docker,dev}
```

Для сервера типичное значение:

```env
PODCAST_SPRING_PROFILES_ACTIVE=docker
PODCAST_DEV_SEED_ENABLED=false
```

## Database config

```yaml
spring:
  datasource:
    url: ${PODCAST_DB_URL:jdbc:postgresql://localhost:5432/podcast_db}
    username: ${PODCAST_DB_USER:podcast_user}
    password: ${PODCAST_DB_PASSWORD:podcast_pass}
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
  port: ${PODCAST_SERVER_PORT:8082}
  servlet:
    context-path: ${PODCAST_SERVER_SERVLET_CONTEXT_PATH:/podcast/v1}
```

При default configuration локальный API доступен по base URL:

```text
http://localhost:8082/podcast/v1
```

## Kafka config

```yaml
spring:
  kafka:
    bootstrap-servers: ${PODCAST_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: ${PODCAST_KAFKA_CONSUMER_GROUP:podcast-service}
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
      enabled: ${PODCAST_AUTH_ENABLED:true}
      secret: ${PODCAST_ACCESS_TOKEN_SECRET:}
      issuer: ${PODCAST_ACCESS_TOKEN_ISSUER:auth-service}
```

Production-требование: `PODCAST_ACCESS_TOKEN_SECRET` поступает из secret storage. Production secret не хранится в Git.

## CORS

```yaml
app:
  cors:
    allowed-origins: ${PODCAST_CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173,http://localhost:8082}
```

Разрешены методы `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`. Credentials выключены. Wildcard `*` не используется.

## Springdoc/Swagger

```yaml
springdoc:
  api-docs:
    enabled: ${PODCAST_SWAGGER_ENABLED:false}
  swagger-ui:
    enabled: ${PODCAST_SWAGGER_ENABLED:false}
    path: /swagger
    url: ${PODCAST_SWAGGER_OPENAPI_URL:/openapi/podcast-service-dev.yaml}
```

Swagger использует dev-спецификацию из static resources, а не `openapi-2.yaml`. Значение `PODCAST_SWAGGER_OPENAPI_URL` хранится без `/podcast/v1`, потому что Springdoc добавляет servlet context path при формировании Swagger UI config.

## Feature flags

| Flag | Где | Default | Описание |
|---|---|---|---|
| `app.dev-seed.enabled` / `PODCAST_DEV_SEED_ENABLED` | `application-dev.yaml` | `true` | Включает сидирование dev-данных |
| `app.auth.jwt.enabled` / `PODCAST_AUTH_ENABLED` | `application.yaml` | `true` | Включает JWT-аутентификацию |
| `springdoc.*.enabled` / `PODCAST_SWAGGER_ENABLED` | `application.yaml`, `application-dev.yaml` | `false` base, `true` dev | Управляет Swagger/OpenAPI endpoints |

> Требует уточнения: политика использования `PODCAST_AUTH_ENABLED=false` за пределами локальной разработки.
