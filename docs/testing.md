# Тестирование

## Запуск тестов

Windows:

```powershell
.\gradlew.bat test
```

Linux/macOS:

```bash
./gradlew test
```

## Используемые инструменты

| Инструмент | Назначение |
|---|---|
| JUnit 5 | Unit/integration tests |
| Spring Boot Test | Поднятие Spring context |
| Spring Security Test | Проверка security-сценариев |
| Spring Kafka Test | Тестовая инфраструктура Kafka |
| Mockito/AssertJ | Моки и assertions через Spring Boot starter test |

## Текущие тесты

| Тест | Что покрывает |
|---|---|
| `ApplicationTests` | Загрузка Spring context с отключением внешних зависимостей |
| `JwtAuthenticationServiceTest` | Валидация JWT, issuer, роли, ошибки токена |
| `AuthorProfileServiceTest` | Авторские профили и проверки бизнес-правил |
| `PodcastVoteServiceTest` | Голосование за подкасты |
| `PodcastMediaServiceTest` | Transcript/summary |
| `SubscriptionServiceTest` | Подписки и лента |
| `ListenHistoryServiceTest` | Прогресс и история |
| `SearchServiceTest` | Поиск |

## Что важно покрывать

### Security

- Запрос без token на protected endpoint возвращает `401`.
- Валидный token без роли `author` не проходит author endpoint и возвращает `403`.
- Валидный token без роли `admin` не проходит category write endpoint и возвращает `403`.
- Истёкший token, неверный issuer и неверная подпись возвращают `401`.
- Публичные GET-ручки работают без token.
- Публичные GET-ручки с token заполняют пользовательский контекст.

### Users/auth integration

- `user.created` создаёт `user_profiles`.
- Повторное событие не ломает состояние или корректно обрабатывает конфликт.
- JWT `user_id` совпадает с `user_profiles.user_id`.

### Podcasts

- Создание подкаста доступно только автору.
- Редактировать/архивировать/публиковать может только владелец.
- Неопубликованный подкаст не виден чужому пользователю.
- Публикация без обязательных условий возвращает `422`.

### Playlists

- Приватный плейлист видит только владелец.
- Только владелец может менять состав.
- Повторное добавление одного подкаста возвращает конфликт или бизнес-ошибку.
- Reorder требует полный и корректный набор элементов.

### Votes

- Повторный vote меняет существующий голос.
- Удаление vote корректно пересчитывает counters.
- Голосование за отсутствующий ресурс возвращает ошибку ресурса.

### Subscriptions

- Подписка меняет `subscribersCount`.
- Повторная подписка не создаёт дубли.
- Self-subscribe запрещён.

### Listen history

- `progressSeconds >= 0`.
- `progressPercent` корректно считается от длительности.
- История сортируется по `lastListenedAt`.

### Search

- Поиск возвращает только опубликованные подкасты и публичные плейлисты.
- `type` фильтрует выдачу.
- `categoryId` фильтрует подкасты.
- `q` валидируется.

## Мок внешних зависимостей

| Зависимость | Как мокать |
|---|---|
| PostgreSQL | Unit tests через mock repositories; integration tests через Testcontainers PostgreSQL |
| Kafka consumer | `spring.kafka.listener.auto-startup=false` для context tests или EmbeddedKafka/Testcontainers Kafka |
| Kafka producer | Mock `KafkaTemplate`/`KafkaEventPublisher` |
| JWT | Генерировать тестовые JWT тем же secret или мокать `JwtAuthenticationService` |
| `auth-service` | Для REST тестов достаточно JWT; для user sync отправлять Kafka message `user.created` |

## Покрытие, представленное в проектной документации

- Controller-level сценарии описывают роли и публичные ручки.
- Integration-сценарии используют PostgreSQL/Testcontainers для миграций, constraints и native search queries.
- Kafka-сценарии включают валидный `user.created`, невалидный envelope и DLT.
- Contract-сценарии сопоставляют `openapi-2.yaml` и контроллеры.
