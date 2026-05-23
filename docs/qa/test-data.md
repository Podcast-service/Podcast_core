# Тестовые данные

Dev seed data создаёт реалистичный набор для ручной проверки и frontend automation.

## Основной пользователь

```json
{
  "id": "00000000-0000-0000-0000-000000000001",
  "email": "dev.user@example.local",
  "username": "dev-user"
}
```

## Роли

| Token | Роли |
|---|---|
| user token | `user` |
| author token | `user`, `author` |
| admin token | `user`, `author`, `admin` |

## Набор данных

Seed содержит опубликованные, черновые, архивные, processing и failed подкасты, public/private плейлисты, разные языки, пустые optional fields и длинные описания.
