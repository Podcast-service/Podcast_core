# Переменные окружения

Все проектные переменные используют префикс `PODCAST_*`.

| Переменная | Default | Обязательность | Назначение |
|---|---|---|---|
| `PODCAST_SPRING_PROFILES_ACTIVE` | `docker,dev` в compose | да | активные Spring profiles |
| `PODCAST_SERVER_PORT` | `8082` | нет | порт приложения внутри контейнера |
| `PODCAST_SERVER_SERVLET_CONTEXT_PATH` | `/podcast/v1` | нет | префикс API |
| `PODCAST_APP_PORT` | `8082` | нет | порт приложения на host |
| `PODCAST_DB` | `podcast_db` | да для compose | имя БД |
| `PODCAST_DB_URL` | зависит от profile | да | JDBC URL |
| `PODCAST_DB_USER` | `podcast_user` | да | пользователь БД |
| `PODCAST_DB_PASSWORD` | `podcast_pass` | да | пароль БД |
| `PODCAST_DB_PORT` | `5432` | нет | порт PostgreSQL на host |
| `PODCAST_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | да | Kafka bootstrap servers |
| `PODCAST_KAFKA_CONSUMER_GROUP` | `podcast-service` | да | consumer group |
| `PODCAST_KAFKA_TOPIC_USER_REGISTER` | `podcast.user.register` | да | topic регистрации пользователей |
| `PODCAST_KAFKA_TOPIC_MEDIA_UPLOAD` | `media.upload` | да | topic загрузки медиа |
| `PODCAST_KAFKA_TOPIC_MEDIA_WORKER` | `media.worker` | да | topic обработки медиа |
| `PODCAST_KAFKA_TOPIC_MEDIA_SUBTITLE` | `media.subtitle` | да | topic результатов субтитров |
| `PODCAST_KAFKA_TOPIC_TTS_START` | `tts.start` | да | topic старта TTS-flow |
| `PODCAST_KAFKA_TOPIC_TTS_FAILED` | `tts.failed` | да | topic ошибок TTS-flow |
| `PODCAST_KAFKA_RETRY_BACKOFF_MS` | `1000` | нет | задержка между retry Kafka |
| `PODCAST_KAFKA_RETRY_MAX_ATTEMPTS` | `3` | нет | количество retry перед DLT для retryable ошибок |
| `PODCAST_KAFKA_DLT_SUFFIX` | `.DLT` | нет | суффикс DLT topic |
| `PODCAST_KAFKA_INTERNAL_PORT` | `9092` | нет | local port Kafka internal listener |
| `PODCAST_KAFKA_EXTERNAL_PORT` | `9094` | нет | local port Kafka external listener |
| `PODCAST_KAFKA_EXTERNAL_HOST` | `host.docker.internal` | нет | advertised host external listener |
| `PODCAST_KAFKA_AUTO_CREATE_TOPICS_ENABLE` | `true` | нет | auto-create topics в local Kafka |
| `PODCAST_RECOMMENDATION_EVENTS_ENABLED` | `false` | нет | запись recommendation MVP events в `outbox_events`; Kafka publisher не включает |
| `PODCAST_KAFKA_UI_PORT` | `8081` | нет | порт Kafka UI |
| `PODCAST_AUTH_SERVICE_BASE_URL` | `http://localhost:8080`, `http://auth-service:8080` в docker | да | base URL auth-service для выдачи роли автора через `/auth/me/update-roles` |
| `PODCAST_AUTH_SERVICE_CONNECT_TIMEOUT` | `2s` | нет | timeout установки соединения с auth-service |
| `PODCAST_AUTH_SERVICE_READ_TIMEOUT` | `5s` | нет | timeout ожидания ответа auth-service |
| `PODCAST_ACCESS_TOKEN_SECRET` | пусто в base config | да | secret подписи JWT |
| `PODCAST_ACCESS_TOKEN_ISSUER` | `auth-service` | да | ожидаемый issuer |
| `PODCAST_AUTH_ENABLED` | `true` | да | включение JWT validation |
| `PODCAST_CORS_ALLOWED_ORIGINS` | local origins | да | разрешённые browser origins |
| `PODCAST_DEV_SEED_ENABLED` | `true` в `dev` | нет | включение dev seed |
| `PODCAST_SWAGGER_ENABLED` | `false` base, `true` dev | нет | включение Swagger/OpenAPI endpoints |
| `PODCAST_SWAGGER_OPENAPI_URL` | `/openapi/podcast-service-dev.yaml` | нет | servlet-relative URL OpenAPI для Swagger UI |

Production secrets передаются из secret storage и не хранятся в репозитории.
