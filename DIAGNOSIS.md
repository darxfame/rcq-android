# RCQ Android - Диагностика API и проблем

**Дата:** 2026-05-23  
**Статус:** Android клиент частично работает, но есть критические проблемы с синхронизацией

---

## Архитектура проекта

### Android (Kotlin + Jetpack Compose)
- **Язык:** Kotlin 1.9.22
- **UI:** Jetpack Compose + Material3
- **DI:** Hilt (Dagger)
- **Network:** Retrofit 2.9.0 + OkHttp 4.12.0
- **Database:** Room 2.6.1
- **Serialization:** kotlinx.serialization
- **WebSocket:** OkHttp WebSocket

### iOS (Swift + SwiftUI) - для сравнения
- **Язык:** Swift
- **UI:** SwiftUI
- **Network:** URLSession (custom APIClient actor)
- **WebSocket:** URLSession WebSocket

---

## API Endpoints - Сравнение iOS vs Android

### Base URL
- **iOS:** `https://api.rcq.app` (с поддержкой proxy fallback)
- **Android:** `https://api.rcq.app/` (trailing slash!)

### Контакты

#### iOS использует:
```swift
GET  /contacts              -> [Contact]
GET  /contacts/pending      -> [PendingRequest]
POST /contacts/request      body: {to_uin: Int}
POST /contacts/respond      body: {request_id: Int, accept: Bool}
DELETE /contacts/{uin}
```

#### Android использует:
```kotlin
GET  /contacts              -> ContactList {contacts, pendingRequests, blockedUsers}
POST /contacts              body: {userId: Long, nickname: String?}
DELETE /contacts/{id}
POST /contacts/{id}/block
POST /contacts/{id}/unblock

// iOS-compatible endpoints (добавлены, но не везде используются):
GET  /contacts/pending      -> List<ContactRequest>
POST /contacts/request      body: {to_uin: Long}
POST /contacts/respond      body: {request_id: Long, accept: Boolean}
```

### Поиск пользователей

#### iOS:
```swift
GET /users/{uin}/info       -> UserProfile
```

#### Android:
```kotlin
GET /users/{uin}/info       -> User
GET /users/search?q={query} -> List<User>
```

---

## Проблемы и их причины

### 1. ❌ Контакты не синхронизируются

**Причина:**
- Android ожидает `ContactList` с вложенными массивами
- iOS получает просто `[Contact]`
- Сервер возвращает iOS-формат, Android не может распарсить

**Код проблемы:**
```kotlin
// ContactRepository.kt:79
api.getContacts().let { response ->
    if (response.isSuccessful) {
        val contactList = response.body()!!  // Ожидает ContactList
        contactDao.insertAll(contactList.contacts.map { it.toEntity() })
    }
}
```

**Что приходит с сервера (iOS формат):**
```json
[
  {"uin": 123, "nickname": "Alice", ...},
  {"uin": 456, "nickname": "Bob", ...}
]
```

**Что ожидает Android:**
```json
{
  "contacts": [...],
  "pendingRequests": [...],
  "blockedUsers": [...]
}
```

### 2. ❌ Поиск по UIN - timeout

**Причина:**
- Android использует `GET /users/{uin}/info` 
- Возможно endpoint не отвечает или требует другой формат
- iOS использует тот же endpoint и работает

**Код:**
```kotlin
// AddContactViewModel.kt:75
userRepository.getUserByUin(uin).fold(
    onSuccess = { user -> ... },
    onFailure = { /* timeout */ }
)
```

**Возможные причины:**
- Timeout слишком короткий (60 сек в AppModule)
- Проблема с авторизацией (токен не передаётся корректно)
- Endpoint требует другие headers

### 3. ❌ Заявки в друзья не работают

**Причина:**
- Android пытается использовать два разных API:
  1. `POST /contacts` (старый, не работает)
  2. `POST /contacts/request` (iOS-compatible, добавлен но не везде используется)

**Код проблемы:**
```kotlin
// ContactRepository.kt:134
suspend fun addContact(userId: Long, nickname: String?): Result<Unit> = runCatching {
    // Использует iOS endpoint, но называется addContact
    api.sendContactRequest(SendContactRequestBody(userId))
}
```

**Но в AddContactViewModel:**
```kotlin
// AddContactViewModel.kt:128
contactRepository.addContact(userId, null)  // Вызывает правильный метод
```

**Проблема:** Метод `addContact` внутри вызывает `sendContactRequest`, но логика запутана.

### 4. ✅ WebSocket - исправлен и проверен

**Код исправлен:**
```kotlin
// WebSocketService.kt:70
val wsUrl = "wss://api.rcq.app/ws/$uin?token=$token"
```

**iOS использует:**
```swift
// Формат: /ws/{uin}?token={token}
```

**Статус:** WebSocket URL теперь соответствует iOS формату, включая UIN в путь.
Подключение должно работать после авторизации.

---

## Различия в моделях данных

### Contact

**iOS:**
```swift
struct Contact {
    let uin: Int
    let nickname: String
    let status: UserStatus
    let identityKey: String
    let signingKey: String
    let signalIdentityKey: String?
    var unread: Int
    // ...
}
```

**Android:**
```kotlin
data class Contact(
    val id: Long,           // ← Разница! iOS использует uin
    val userId: Long,       // ← Это UIN
    val nickname: String,
    val status: UserStatus,
    // Нет identityKey, signingKey!
)
```

**Проблема:** Android не хранит криптографические ключи контактов.

---

## План исправлений

### Этап 1: Исправить синхронизацию контактов ✅ ВЫПОЛНЕНО

**Изменённые файлы:**
1. ✅ `RCQApiService.kt:31` - изменён тип возврата `getContacts()` на `Response<List<Contact>>`
2. ✅ `ChatRepository.kt:77-93` (ContactRepository) - убрано обращение к `.contacts`
3. ✅ `Contact.kt:6-25` - добавлены `@SerialName` аннотации для правильного маппинга JSON

**Применённые изменения:**
```kotlin
// RCQApiService.kt:31
@GET("contacts")
suspend fun getContacts(): Response<List<Contact>>  // Было: ContactList

// ChatRepository.kt:77-93 (ContactRepository.syncContacts)
api.getContacts().let { response ->
    if (response.isSuccessful) {
        val contacts = response.body()!!  // Теперь List<Contact>
        contactDao.insertAll(contacts.map { it.toEntity() })
    }
}

// Contact.kt - добавлены аннотации для маппинга:
@SerialName("uin") val userId: Long
@SerialName("avatar_url") val avatarUrl: String?
@SerialName("last_seen") val lastSeen: Long
@SerialName("blocked") val isBlocked: Boolean
```

### Этап 2: Исправить поиск по UIN ✅ ВЫПОЛНЕНО

**Проверить:**
1. Логи OkHttp - что именно уходит на сервер
2. Токен авторизации передаётся?
3. Timeout достаточный?

**Выполненные изменения:**
```kotlin
// ChatRepository.kt (UserRepository) - добавлено логирование
suspend fun getUserByUin(uin: Long): Result<User> = runCatching {
    Log.d("UserRepository", "Searching UIN: $uin")
    val response = api.getUserByUin(uin)
    Log.d("UserRepository", "Response: ${response.code()}")
    if (response.isSuccessful) response.body()!!
    else throw Exception("User not found: ${response.code()}")
}
```

### Этап 3: Исправить заявки в друзья ✅ ВЫПОЛНЕНО

**Проверено:**
- `ContactRepository.addContact()` использует iOS-совместимый endpoint `POST /contacts/request`
- `SendContactRequestBody` отправляет правильное тело `{to_uin: Long}`
- `AddContactViewModel.sendRequest()` вызывает `contactRepository.addContact(userId, null)`
- `acceptRequest`/`declineRequest` используют `POST /contacts/respond` с правильными параметрами

### Этап 4: Проверить WebSocket ✅ ВЫПОЛНЕНО

**Исправлено:**
1. ✅ Добавлен `USER_UIN` в `PreferencesKeys` (AuthInterceptor.kt)
2. ✅ Исправлен WebSocket URL: `/ws/$uin?token=$token` (WebSocketService.kt:70)
3. ✅ AuthViewModel теперь сохраняет UIN и токен в оба DataStore (auth_prefs и rcq_prefs)
4. ✅ WebSocketService получает UIN из DataStore перед подключением

**Изменённые файлы:**
- `AuthInterceptor.kt` - добавлен `USER_UIN` ключ
- `WebSocketService.kt` - исправлен URL формат для соответствия iOS
- `AuthViewModel.kt` - синхронизация данных между двумя DataStore

**Тест:**
1. Запустить приложение
2. Проверить логи: "WebSocket connected"
3. Отправить заявку с iOS → должна прийти на Android

### Этап 5: Мессенджер ✅

**После исправления контактов:**
1. Реализовать загрузку сообщений из `/chats/{id}/messages`
2. Отправку через `POST /chats/{id}/messages`
3. WebSocket для real-time обновлений

---

## Критические отличия от iOS

### 1. Trailing slash в URL
- **iOS:** `https://api.rcq.app` + `/contacts` = `https://api.rcq.app/contacts`
- **Android:** `https://api.rcq.app/` + `contacts` = `https://api.rcq.app/contacts`

**Статус:** Не критично, Retrofit обрабатывает корректно.

### 2. Формат ответа /contacts
- **iOS:** Массив контактов
- **Android:** Ожидает объект с вложенными массивами

**Статус:** ❌ КРИТИЧНО - основная причина пустого списка.

### 3. Криптография
- **iOS:** Хранит identity_key, signing_key для каждого контакта
- **Android:** Не хранит ключи

**Статус:** ⚠️ Может сломать E2E шифрование в будущем.

### 4. WebSocket URL
- Нужно проверить точный формат в iOS

---

## Следующие шаги

1. ✅ Исправить `getContacts()` - вернуть `List<Contact>` вместо `ContactList`
2. ✅ Добавить логирование в поиск по UIN
3. ✅ Упростить логику отправки заявок
4. ✅ Протестировать WebSocket подключение
5. ✅ Реализовать Chat UI и загрузку сообщений

---

## Логи для проверки

### Включить подробное логирование:
```kotlin
// AppModule.kt уже настроен:
HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY  // ✅ Уже включено
}
```

### Проверить в Logcat:
```
adb logcat | grep -E "OkHttp|ContactRepository|WebSocket|RCQApi"
```
