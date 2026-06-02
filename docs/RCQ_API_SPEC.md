# RCQ API Specification

Спецификация API для RCQ мессенджера на основе [rcq-spec](https://github.com/rcq-messenger/rcq-spec/blob/main/SPEC.md).

## Аутентификация

### Регистрация
```
POST /auth/register
```
```json
{
  "nickname": "string (1-64 символа)",
  "identity_key": "base64 X25519 public key (32 байта)",
  "signing_key": "base64 Ed25519 public key (32 байта)", 
  "inviter_uin": "int|null"
}
```

### Управление сессией
```
POST /auth/session
```
Создает новый JWT для существующей аутентифицированной сессии.

**JWT формат**: HS256 с полями `sub` (UIN как строка), `iat`, `exp`. TTL по умолчанию 30 дней.

## Основные Endpoints

### Профиль пользователя
- `GET /users/me/info` - Информация о текущем пользователе
- `GET /users/{uin}/info` - Информация о пользователе с контролем видимости
- `PUT /users/me` - Частичное обновление профиля
- `POST /presence/status` - Обновление статуса (online/away/dnd/invisible/offline)

### Управление контактами
- `GET /contacts` - Список контактов со статусами и ключами
- `POST /contacts/request` - Отправка запроса в друзья с авто-принятием
- `GET /contacts/pending` - Ожидающие запросы
- `POST /contacts/respond` - Принять/отклонить запрос
- `DELETE /contacts/{contact_uin}` - Удалить контакт
- `POST /contacts/{contact_uin}/block` - Заблокировать пользователя

### Криптографические ключи (libsignal)
- `POST /keys/bundle` - Загрузка libsignal key bundle
- `POST /keys/prekeys` - Пополнение одноразовых prekeys
- `GET /keys/{uin}/bundle` - Получение ключей получателя
- `GET /keys/me/status` - Проверка количества prekeys

### Сообщения
- `POST /messages/sealed` - Отправка 1:1 зашифрованного сообщения
- `POST /messages/group-sealed` - Отправка группового сообщения
- `GET /messages/queue` - Получение оффлайн сообщений

### Медиа и файлы
- `POST /media/upload` - Загрузка зашифрованных медиа
- `GET /media/{media_id}` - Скачивание медиа
- `GET /media/usage` - Проверка квоты хранилища

### Push уведомления
- `POST /users/me/push-token` - Регистрация APNs/FCM токена
- `DELETE /users/me/push-token` - Отмена регистрации токена
- `GET /users/me/push-preferences` - Настройки уведомлений
- `PUT /users/me/push-preferences` - Обновление настроек уведомлений

## WebSocket канал

**Подключение**: `wss://api.rcq.app/ws/{uin}?token={jwt}`

**Типы событий**:
- `message-family` - Новые сообщения
- `presence` - Обновления статуса
- `typing` - Индикаторы печати
- События контактов (запросы, принятия)
- События групп (изменения участников)
- Сигналинг для голосовых/видео звонков

## Ключевые структуры данных

**UIN**: 9-значный идентификатор в диапазоне `[100_000, 999_999_999]`

**Области видимости**: `everyone`, `contacts`, `nobody`

**Конверт сообщения**: Использует libsignal v2 с sealed-sender для приватности метаданных

**Ограничения**: Запросы в друзья ограничены "30/час на идентичность"

## Модель безопасности

Система использует **libsignal (X3DH + Double Ratchet + Kyber/PQXDH)** для сквозного шифрования.

- Сервер **никогда не видит текст сообщений**
- Использует sealed-sender конверты - **сервер не может идентифицировать отправителя 1:1 сообщения**
- Все приватные ключи принадлежат клиенту без серверного бэкапа
- Восстановление аккаунта невозможно - **клиент владеет единственной копией приватных ключей**
- Встроенный обход цензуры с протоколами VLESS+Reality и Hysteria2
- Поддержка нескольких устройств через управление сессиями libsignal

## Примечания по реализации

- Открытый исходный код (AGPL-3.0)
- Активно поддерживается на `github.com/rcq-messenger/rcq-spec`
- Совместимость с iOS реализацией через libsignal
- Полная E2EE совместимость между платформами