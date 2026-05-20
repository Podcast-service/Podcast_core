# Архитектура

## Назначение сервиса

`podcast-core` отвечает за основной домен подкаст-платформы:

- пользовательские профили и настройки;
- авторские профили;
- категории;
- подкасты, публикацию, transcript и summary;
- плейлисты;
- голоса за подкасты и плейлисты;
- подписки на авторов и персональную ленту;
- историю прослушивания;
- полнотекстовый поиск и подсказки.

Сервис не логинит пользователей и не выпускает токены. Он доверяет JWT access token от `auth-service`, валидирует подпись, issuer и срок действия, а затем использует `user_id` и роли из токена.

## Технологии

| Область | Используется |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 4.0.5 |
| REST | Spring WebMVC |
| Security | Spring Security, JWT Bearer token |
| Persistence | Spring Data JPA, Hibernate |
| DB migrations | Flyway |
| Database | PostgreSQL |
| Messaging | Spring Kafka |
| DTO mapping | MapStruct |
| Validation | Jakarta Validation |
| Observability | Spring Boot Actuator, SLF4J logs |
| Tests | JUnit 5, Spring Boot Test, Spring Security Test, Spring Kafka Test |

## Основные пакеты

| Пакет | Назначение |
|---|---|
| `podcastService.user` | Профили пользователей, настройки, создание профиля по Kafka-событию |
| `podcastService.author` | Авторские профили и публичные карточки авторов |
| `podcastService.category` | Справочник категорий |
| `podcastService.podcast` | Подкасты, публикация, выдача карточек и деталей, голоса за подкасты |
| `podcastService.transcript` | Transcript и summary подкаста |
| `podcastService.playlist` | Плейлисты, состав плейлистов, голоса за плейлисты |
| `podcastService.subscription` | Подписки на авторов и лента подписок |
| `podcastService.history` | История прослушивания и прогресс |
| `podcastService.search` | Поиск и autocomplete |
| `podcastService.infrastructure.security` | JWT filter, SecurityFilterChain, CORS, security error writer |
| `podcastService.infrastructure.messaging` | Kafka envelope, routing, producer, error handler, DLT |
| `podcastService.common` | Общие DTO, исключения и обработка ошибок |

## Слои

| Слой | Примеры | Ответственность |
|---|---|---|
| Controller | `PodcastController`, `PlaylistController` | HTTP mapping, validation annotations, извлечение `AuthenticatedUser`, логирование входа |
| Service | `PodcastService`, `PlaylistService`, `SubscriptionService` | Бизнес-правила, проверки владельца, статусы, транзакции |
| Repository | `PodcastRepository`, `SearchRepository` | Запросы к PostgreSQL через JPA/native SQL |
| Entity | `PodcastEntity`, `PlaylistEntity` | Модель БД |
| DTO | `PodcastCard`, `CreatePlaylistRequest` | Контракт REST API |
| Mapper | MapStruct mappers | Преобразование entity в DTO |
| Infrastructure | security, messaging, config | Сквозные механизмы |

## Поток HTTP-запроса

1. Запрос проходит через CORS и Spring Security filter chain.
2. `JwtAuthenticationFilter` извлекает `Authorization: Bearer ...`.
3. `JwtAuthenticationService` валидирует JWT и создаёт `AuthenticatedUser`.
4. Spring Security применяет правила URL-level security.
5. Controller принимает запрос, применяет Jakarta Validation и логирует ключевые параметры.
6. Method security применяет `@PreAuthorize`.
7. Service выполняет бизнес-логику и проверки доступа на уровне владельца ресурса.
8. Repository работает с PostgreSQL.
9. DTO возвращается клиенту.
10. Исключения централизованно обрабатываются `GlobalExceptionHandler` или security handlers.

## Авторизация

| Доступ | Где используется |
|---|---|
| Public | Категории, публичные подкасты, публичные плейлисты, публичные авторы, поиск |
| Optional token | Публичные списки/детали, где полезны `currentUserVote`, `progressSeconds`, `isSubscribed` |
| Authenticated | Профиль, настройки, плейлисты пользователя, подписки, история, прогресс, голосование |
| `ROLE_AUTHOR` | Авторский профиль `/authors/me`, создание/редактирование/публикация подкастов |
| `ROLE_ADMIN` | Создание, изменение и удаление категорий |

Роли приходят из JWT claim `roles` и нормализуются в Spring Security authority вида `ROLE_AUTHOR`, `ROLE_ADMIN`, `ROLE_USER`.

## Зависимости от других сервисов

| Сервис | Тип связи | Назначение |
|---|---|---|
| `auth-service` | JWT + Kafka | Выпускает access token и публикует событие создания пользователя |
| `tts-stt-service` | TODO: уточнить | В OpenAPI указано, что он генерирует transcript/summary, но в текущем коде REST/Kafka-интеграция с ним не реализована |
| Frontend | HTTP | Использует REST API и Swagger/OpenAPI contract |

## Хранилища и очереди

| Компонент | Использование |
|---|---|
| PostgreSQL | Основное хранилище доменных данных |
| Flyway | Версионирование схемы БД |
| Kafka | Приём события `user.created` от `auth-service`; инфраструктура producer есть, но текущий доменный publish producer в коде не используется |

## База данных

Основная миграция `V1__init_schema.sql` создаёт таблицы:

- `user_profiles`
- `author_profiles`
- `categories`
- `podcasts`
- `podcast_transcripts`
- `podcast_summaries`
- `playlists`
- `playlist_podcasts`
- `subscriptions`
- `podcast_votes`
- `playlist_votes`
- `listen_history`

Также создаются индексы для списков, feed, поиска, trigram-поиска и триггеры для `updated_at`, search vector и части доменных ограничений. Миграция `V2__harden_author_profiles.sql` усиливает ограничения длины `author_name` и `description`.

## Несоответствия

| Место | Что указано в `openapi-2.yaml` | Что видно в коде |
|---|---|---|
| Base URL | `http://localhost:8082/v1` и production `/podcast/v1` | Контроллеры смонтированы без `/v1`; фактический локальный путь `http://localhost:8082` |
| Удаление vote подкаста | Описание говорит, что при отсутствии голоса возможен `204` | Код возвращает `200 OK` с `VoteResponse` |
| Название sort enum | `SortPodcast` | В коде DTO называется `SortPodcasts`; значения совпадают |
| `CreateUserRequest` | Не описан как REST body | Используется только как Kafka payload для `user.created` |
| Transcript/summary generation | Указано, что генерирует `tts-stt-service` | В коде есть только чтение из БД; механика записи внешним сервисом требует уточнения |
