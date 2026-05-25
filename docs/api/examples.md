# Примеры запросов

## Получить категории

```bash
curl http://localhost:8082/podcast/v1/categories
```

## Создать подкаст

```bash
curl -X POST http://localhost:8082/podcast/v1/podcasts \
  -H "Authorization: Bearer ${AUTHOR_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Spring Boot в production",
    "description": "Практический выпуск о сервисах, логах и миграциях",
    "categoryId": "48b67732-5676-36bd-a97f-44d01de91376",
    "coverImageUrl": "https://cdn.example.local/covers/spring.png",
    "num_speakers": 2
  }'
```

## Получить количество спикеров

```bash
curl http://localhost:8082/podcast/v1/podcasts/22222222-2222-2222-2222-222222222222/speakers
```

```json
{
  "podcastId": "22222222-2222-2222-2222-222222222222",
  "num_speakers": 2
}
```

## Найти подкасты

```bash
curl "http://localhost:8082/podcast/v1/search?q=Kafka&type=PODCAST&sort=RELEVANCE&page=1&size=10"
```

## Добавить выпуск в плейлист

```bash
curl -X POST http://localhost:8082/podcast/v1/playlists/11111111-1111-1111-1111-111111111111/podcasts \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "podcastId": "22222222-2222-2222-2222-222222222222"
  }'
```

## Сохранить прогресс

```bash
curl -X POST http://localhost:8082/podcast/v1/podcasts/22222222-2222-2222-2222-222222222222/progress \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"progressSeconds": 640}'
```

## Ошибка валидации

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "timestamp": "2026-05-23T10:00:00Z",
  "details": {
    "fields": {
      "page": "must be greater than or equal to 1"
    }
  }
}
```
