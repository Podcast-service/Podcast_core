# Edge cases

| Область | Сценарий | Ожидаемое поведение |
|---|---|---|
| JWT | истёкший token | `401 UNAUTHORIZED` |
| JWT | роль отсутствует | `403 FORBIDDEN` |
| Категории | дублирование имени | `409 CONFLICT` |
| Подкасты | публикация без необходимых медиа | `422 BUSINESS_RULE_VIOLATION` |
| Плейлисты | доступ к private playlist не владельцем | `403 FORBIDDEN` |
| Плейлисты | повторное добавление выпуска | `409 CONFLICT` |
| Votes | повторный vote меняет тип | `200` и обновлённые счётчики |
| Search | пустой `q` | `400 VALIDATION_ERROR` |
| Kafka | неизвестный `eventType` | DLT |
