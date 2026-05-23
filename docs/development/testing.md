# Тестирование

## Команды

```bash
./gradlew test
./gradlew clean test
```

На Windows:

```powershell
.\gradlew.bat clean test
```

## Набор тестов

Проект использует Spring Boot test stack, Spring Security Test и Spring Kafka Test. Интеграционные сценарии проверяют Spring context, web layer, security layer и работу с тестовым окружением.

## Важные сценарии

| Область | Сценарии |
|---|---|
| Авторизация | отсутствие токена, истёкший токен, неверная роль |
| Подкасты | создание, обновление владельцем, публикация, публичный каталог |
| Плейлисты | private/public доступ, reorder, добавление опубликованных выпусков |
| Votes | повторный голос, смена типа, удаление |
| Search | пустой результат, фильтры, сортировка |
| Kafka | валидное событие, невалидный payload, DLT |
