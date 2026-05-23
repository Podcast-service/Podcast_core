# Пагинация для frontend

`page` начинается с `1`. Клиент отображает следующую страницу, если `meta.page < meta.totalPages`.

```json
{
  "items": [],
  "meta": {
    "page": 1,
    "size": 20,
    "totalElements": 120,
    "totalPages": 6
  }
}
```

При смене `q`, фильтра, категории или сортировки клиент возвращается на `page=1`.
