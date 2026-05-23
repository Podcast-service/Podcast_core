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
| `PODCAST_KAFKA_TOPIC_USERS` | `podcasts.users` | да | topic событий пользователей |
| `PODCAST_KAFKA_TOPIC_PODCASTS` | `podcasts.podcasts` | нет | зарезервированный topic подкастов |
| `PODCAST_KAFKA_INTERNAL_PORT` | `9092` | нет | local port Kafka internal listener |
| `PODCAST_KAFKA_EXTERNAL_PORT` | `9094` | нет | local port Kafka external listener |
| `PODCAST_KAFKA_EXTERNAL_HOST` | `host.docker.internal` | нет | advertised host external listener |
| `PODCAST_KAFKA_AUTO_CREATE_TOPICS_ENABLE` | `true` | нет | auto-create topics в local Kafka |
| `PODCAST_KAFKA_UI_PORT` | `8081` | нет | порт Kafka UI |
| `PODCAST_ACCESS_TOKEN_SECRET` | пусто в base config | да | secret подписи JWT |
| `PODCAST_ACCESS_TOKEN_ISSUER` | `auth-service` | да | ожидаемый issuer |
| `PODCAST_AUTH_ENABLED` | `true` | да | включение JWT validation |
| `PODCAST_CORS_ALLOWED_ORIGINS` | local origins | да | разрешённые browser origins |
| `PODCAST_DEV_SEED_ENABLED` | `true` в `dev` | нет | включение dev seed |
| `PODCAST_SWAGGER_ENABLED` | `false` base, `true` dev | нет | включение Swagger/OpenAPI endpoints |
| `PODCAST_SWAGGER_OPENAPI_URL` | `/openapi/podcast-service-dev.yaml` | нет | servlet-relative URL OpenAPI для Swagger UI |

Production secrets передаются из secret storage и не хранятся в репозитории.
