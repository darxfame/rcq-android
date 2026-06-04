# RCQ Android — Codex Fix Plan
**Платформа:** Codex GPT-5.5  
**Проект:** `/root/llm/rcq_android/android/`  
**Сборка:** `./gradlew assembleDebug` (проверять после каждой фазы)  
**Правило для Codex:** каждая задача атомарна и самодостаточна. Не переходи к следующей задаче не убедившись что текущая компилируется без ошибок (`./gradlew kspDebugKotlin`).

---

## Контекст архитектуры (читай перед началом)

```
app/src/main/java/com/rcq/messenger/
├── ui/
│   ├── MainActivity.kt          — точка входа, lifecycle ПУСТОЙ
│   ├── RCQApp.kt                — NavHost + AuthNavigation
│   ├── chat/
│   │   ├── ChatScreen.kt        — экран чата
│   │   ├── ChatsScreen.kt       — список чатов
│   │   ├── ChatsViewModel.kt    — содержит ТАКЖЕ ChatViewModel (оба в одном файле)
│   │   └── inbox/InboxMapper.kt — маппинг Chat/Contact/Group → InboxRow
│   ├── contacts/
│   │   ├── ContactsScreen.kt    — содержит ТАКЖЕ ContactsViewModel (оба в одном файле)
│   │   └── ContactInfoViewModel.kt
│   ├── common/StatusIndicator.kt — ICQ статус-иконки
│   └── settings/SettingsScreen.kt
├── data/
│   ├── api/RCQApiService.kt     — Retrofit интерфейс
│   ├── repository/ChatRepository.kt — содержит ВСЕ репозитории (один огромный файл)
│   └── websocket/WebSocketService.kt
└── domain/model/
    ├── ChatEntity.kt — Room entity (lastMessageContent, lastMessageKind, lastMessageTimestamp есть)
    ├── ContactEntity.kt
    ├── GroupEntity.kt
    └── Message.kt   — содержит ТАКЖЕ data class Chat и другие модели
```

**Зависимости уже в build.gradle:**
- `io.coil-kt:coil-compose:2.5.0` — загрузка изображений (УЖЕ ДОБАВЛЕН)
- `io.coil-kt:coil-gif:2.5.0` — GIF поддержка (УЖЕ ДОБАВЛЕН)
- `com.google.accompanist:accompanist-permissions` — runtime permissions
- `com.google.android.gms:play-services-location` — геолокация
- Drawable ресурсы `status_00.png..status_08.png` — ICQ статус иконки (УЖЕ В /res/drawable/)

---

## ФАЗА 1 — Критические баги (сломан базовый функционал)

### Задача 1.1 — Загрузка изображений в чате (Coil)
**Файл:** `app/src/main/java/com/rcq/messenger/ui/chat/components/MediaMessageBubble.kt`  
**Проблема:** строка ~157 содержит `// TODO: Load image from base64 thumbnail or media URL`. Фото-сообщения рендерятся как текст "photo".

**Что сделать:**
1. Найди в `MediaMessageBubble.kt` блок для `MessageKind.PHOTO` и `MessageKind.VIDEO`.
2. Замени заглушку на `AsyncImage` из Coil:
```kotlin
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

// Для PHOTO:
val imageUrl = message.mediaUrl ?: "https://api.rcq.app/media/${message.mediaId}"
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .crossfade(true)
        .build(),
    contentDescription = "Photo",
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 260.dp)
        .clip(RoundedCornerShape(12.dp)),
    contentScale = ContentScale.Crop
)
```
3. Для случая когда `mediaId == null && mediaUrl == null` показывай `Box` с иконкой `Icons.Default.BrokenImage`.
4. Аналогично для VIDEO — показывай превью с иконкой Play поверх.

**Проверка:** `./gradlew kspDebugKotlin` без ошибок.

---

### Задача 1.2 — Аватары контактов и групп
**Файлы:**
- `ui/common/` — создать новый файл `AvatarImage.kt`
- `ui/chat/ChatScreen.kt` — строки с `Box(Modifier.size(36.dp))` где буква на фоне
- `ui/contacts/ContactsScreen.kt` — строки аватара контакта
- `ui/chat/ChatsScreen.kt` — строки аватара в InboxRow

**Проблема:** везде рендерится первая буква на цветном фоне. Coil в зависимостях есть, но не используется.

**Что сделать:**
1. Создай `ui/common/AvatarImage.kt`:
```kotlin
package com.rcq.messenger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.rcq.messenger.ui.theme.LocalRCQColors

@Composable
fun AvatarImage(
    avatarUrl: String?,
    displayName: String,
    size: Dp = 40.dp,
    hasStory: Boolean = false,
    modifier: Modifier = Modifier
) {
    val rcq = LocalRCQColors.current
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(modifier = modifier.size(size)) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = displayName,
                modifier = Modifier.size(size).clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier.size(size).clip(CircleShape).background(rcq.accent),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, color = Color.White, fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.4f).sp)
            }
        }
    }
}
```
2. Замени все места с ручным рендером буквы-аватара на вызов `AvatarImage(avatarUrl = ..., displayName = ...)`.
3. В `ChatScreen.kt` в TopAppBar: `AvatarImage(avatarUrl = null, displayName = chatTitle, size = 36.dp)`.
4. В InboxRow найди рендер аватара и замени на `AvatarImage(avatarUrl = row.avatarUrl, displayName = row.title)`.

**Проверка:** `./gradlew kspDebugKotlin`.

---

### Задача 1.3 — Копирование recovery phrase
**Файл:** `ui/RCQApp.kt` строка ~367

**Проблема:** `onCopy = { /* TODO: Copy to clipboard */ }` — кнопка не работает.

**Что сделать:** найди место где вызывается `RecoveryPhraseScreen` и добавь перед ним `val context = LocalContext.current`, затем:
```kotlin
onCopy = {
    val clip = android.content.ClipData.newPlainText(
        "Recovery Phrase",
        recoveryPhrase.joinToString(" ")
    )
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
        as android.content.ClipboardManager
    clipboard.setPrimaryClip(clip)
}
```

---

### Задача 1.4 — Профиль контакта "User not found"
**Файл:** `ui/contacts/ContactInfoViewModel.kt` строка с `userRepository.getUser(userId)`

**Проблема:** `getUser(userId)` вызывает `GET /users/{id}` — устаревший/неправильный endpoint. `getUserByUin()` вызывает `GET /users/{uin}/info` — правильный endpoint.

**Что сделать:**
```kotlin
// Найди в ContactInfoViewModel.load():
userRepository.getUser(userId)
// Замени на:
userRepository.getUserByUin(userId)
```
Также в `data/repository/ChatRepository.kt` в классе `UserRepository` сделай `getUser()` delegate:
```kotlin
suspend fun getUser(userId: Long): Result<User> = getUserByUin(userId)
```

---

### Задача 1.5 — UIN вместо никнейма в заголовке и пузырьках чата
**Файл:** `ui/chat/ChatsViewModel.kt` → класс `ChatViewModel` метод `loadChat()`

**Проблема:** если контакт не в локальной БД — `targetNickname = userId.toString()` (число).

**Что сделать:**
1. В `ChatViewModel.loadChat()` после установки `_chatTitle`:
```kotlin
// Если title — чистое число (UIN без никнейма), подгружаем с сервера:
if (_chatTitle.value.all { it.isDigit() } && _chatTitle.value.isNotEmpty()) {
    userRepository.getUserByUin(_chatTitle.value.toLong())
        .onSuccess { user ->
            if (user.nickname.isNotBlank()) {
                _chatTitle.value = user.nickname
                chatRepository.updateChatNickname(chatId, user.nickname)
            }
        }
}
```
2. В `ChatRepository` добавь метод:
```kotlin
suspend fun updateChatNickname(chatId: String, nickname: String) {
    chatDao.getChat(chatId)?.let { chatDao.insertChat(it.copy(targetNickname = nickname)) }
}
```
3. Для групповых сообщений: добавь в `ChatViewModel` кэш `val nicknameCache = mutableMapOf<Long, String>()`. При рендере `MessageBubble` когда `message.senderName == null` — проверь кэш, если нет — запроси `getUserByUin(senderId)` и положи в кэш.

---

### Задача 1.6 — Нет последнего сообщения в списке чатов
**Файл:** `data/repository/ChatRepository.kt`

**Проблема:** `ChatEntity` имеет поля `lastMessageContent/Timestamp/Kind` но они не обновляются после получения нового сообщения через WS или offline queue.

**Что сделать:** найди в `ChatRepository` все места где `messageDao.insertMessage(...)` вызывается при получении входящего сообщения. После каждой такой вставки добавь:
```kotlin
val existingChat = chatDao.getChat(chatId)
if (existingChat != null) {
    val preview = when (msg.kind) {
        "PHOTO", "PREMIUM_PHOTO" -> "📷 Photo"
        "VIDEO", "PREMIUM_VIDEO" -> "🎥 Video"
        "VOICE" -> "🎤 Voice message"
        "FILE"  -> "📎 ${msg.fileName ?: "File"}"
        "LOCATION" -> "📍 Location"
        else -> msg.content.take(100)
    }
    chatDao.insertChat(existingChat.copy(
        lastMessageContent = preview,
        lastMessageTimestamp = msg.timestamp,
        lastMessageKind = msg.kind,
        updatedAt = msg.timestamp
    ))
}
```
Это нужно сделать в ТРЁХ местах: при обработке `MessageNew` WS-события, при дрейне offline queue (`syncOfflineQueue`), и при успешной отправке исходящего сообщения.

---

### Задача 1.7 — Количество участников группы в списке
**Файл:** `data/repository/ChatRepository.kt` → класс `GroupRepository` метод `syncGroups()`

**Проблема:** `Group.memberCount` = 0 потому что при маппинге `GroupApiResponse → GroupEntity` `members.size` не сохраняется.

**Что сделать:**
1. Найди в `GroupRepository.syncGroups()` маппинг `GroupApiResponse` → `GroupEntity`.
2. Добавь: `memberCount = response.members.size`
3. Если в `GroupEntity` нет поля `memberCount` — добавь `val memberCount: Int = 0` и создай Room migration:
```kotlin
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE groups ADD COLUMN memberCount INTEGER NOT NULL DEFAULT 0")
    }
}
```
4. Зарегистрируй миграцию в `RCQDatabase.kt` в `addMigrations(...)`.

---

## ФАЗА 2 — Lifecycle и фоновая работа

### Задача 2.1 — Обновление при разблокировке экрана / возврате в приложение
**Файл:** `ui/MainActivity.kt`

**Проблема:** нет lifecycle overrides. WS может быть "мёртв" после sleep-а экрана, но код не проверяет.

**Что сделать:**
```kotlin
// Добавить inject в MainActivity:
@Inject lateinit var webSocketService: com.rcq.messenger.data.websocket.WebSocketService

private var stoppedAt: Long = 0L

override fun onStart() {
    super.onStart()
    val bgMs = System.currentTimeMillis() - stoppedAt
    if (stoppedAt > 0L && bgMs > 4_000L) {
        lifecycleScope.launch {
            webSocketService.reconnectIfNeeded()
        }
    }
}

override fun onStop() {
    super.onStop()
    stoppedAt = System.currentTimeMillis()
}
```
В `WebSocketService.kt` добавь:
```kotlin
fun reconnectIfNeeded() {
    val connected = _connectionState.value == ConnectionState.Connected
    if (!connected) {
        reconnectJob?.cancel()
        reconnectStrategy.reset()
        connect()
    }
}
```

---

### Задача 2.2 — Away-статус при сворачивании приложения
**Файл:** `ui/MainActivity.kt`

**Проблема:** пользователь числится Online сутками после закрытия приложения.

**Что сделать:**
```kotlin
// Добавить inject:
@Inject lateinit var userRepository: com.rcq.messenger.data.repository.UserRepository

override fun onStop() {
    super.onStop()
    stoppedAt = System.currentTimeMillis()
    lifecycleScope.launch {
        runCatching { userRepository.updatePresence("away") }
    }
}

override fun onStart() {
    super.onStart()
    // ... reconnect логика из 2.1
    lifecycleScope.launch {
        runCatching { userRepository.updatePresence("online") }
    }
}
```

---

### Задача 2.3 — Фоновый прием сообщений (WorkManager)
**Файл:** создать `service/MessageSyncWorker.kt`

**Проблема:** WS умирает в фоне через ~10 минут. Новые сообщения не приходят.

**Что сделать:**
1. Добавь в `build.gradle.kts`:
```kotlin
implementation("androidx.work:work-runtime-ktx:2.9.0")
implementation("androidx.hilt:hilt-work:1.1.0")
kapt("androidx.hilt:hilt-compiler:1.1.0")
```
2. Создай `service/MessageSyncWorker.kt`:
```kotlin
package com.rcq.messenger.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.rcq.messenger.data.repository.ChatRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class MessageSyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val chatRepository: ChatRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = runCatching {
        chatRepository.syncOfflineQueue()
    }.fold({ Result.success() }, { Result.retry() })

    companion object {
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<MessageSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("rcq_msg_sync",
                    ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
```
3. В `RCQApplication.kt`:
```kotlin
override fun onCreate() {
    super.onCreate()
    // ... существующий код
    MessageSyncWorker.schedule(this)
}
```
4. В `RCQApplication` добавь `@HiltAndroidApp` если ещё нет. Зарегистрируй `HiltWorkerFactory` через `Configuration.Provider`.

---

## ФАЗА 3 — UI/UX: навигация и экраны

### Задача 3.1 — Call screen на весь экран (убрать BottomNavBar)
**Файл:** `ui/RCQApp.kt`

**Проблема:** CallScreen через NavHost — виден BottomNavBar.

**Что сделать:** в `MainScaffold` в логике `showBottomBar`:
```kotlin
val showBottomBar = bottomNavItems.any { screen ->
    currentDestination?.hierarchy?.any { it.route == screen.route } == true
} && currentDestination?.route?.startsWith("call/") != true
```

---

### Задача 3.2 — Минимизированный бар активного звонка
**Файл:** создать `ui/calls/CallMiniBar.kt`, встроить в `MainScaffold`

**Что сделать:**
1. Создай `ui/calls/CallMiniBar.kt`:
```kotlin
package com.rcq.messenger.ui.calls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CallMiniBar(
    calleeName: String,
    isVisible: Boolean,
    onTap: () -> Unit,
    onHangup: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B5E20))
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("📞 $calleeName", color = Color.White,
                style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onHangup) {
                Icon(Icons.Default.CallEnd, "End call", tint = Color.Red)
            }
        }
    }
}
```
2. В `MainScaffold` добавь в `topBar` слот `Scaffold`: `CallMiniBar(isVisible = callViewModel.isCallActive, ...)`. Получи `CallViewModel` через `hiltViewModel()`.

---

### Задача 3.3 — AudioRoomsScreen — рабочая навигация
**Файл:** `ui/RCQApp.kt` строка 186

**Что сделать:**
```kotlin
// Добавить маршрут:
composable("room/{roomId}") { backStackEntry ->
    val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
    // Используй существующий AudioRoomsScreen или создай AudioRoomDetailScreen
    AudioRoomsScreen(onRoomClick = { navController.popBackStack() })
}

// Заменить:
onRoomClick = { }
// На:
onRoomClick = { roomId -> navController.navigate("room/$roomId") }
```

---

### Задача 3.4 — Кнопка Nearby на главном экране
**Файл:** `ui/contacts/ContactsScreen.kt`

**Что сделать:** в TopAppBar в `ContactsScreen` добавь иконку:
```kotlin
IconButton(onClick = onNearby) {
    Icon(Icons.Default.LocationOn, contentDescription = "Nearby users")
}
```
Убедись что `onNearby: () -> Unit` есть в параметрах `ContactsScreen`. Если нет — добавь.

---

### Задача 3.5 — Архивированные чаты: секция в ChatsScreen
**Файл:** `ui/chat/ChatsViewModel.kt` + `ui/chat/ChatsScreen.kt`

**Что сделать:**
1. В `ChatsViewModel` добавь:
```kotlin
val archivedCount: StateFlow<Int> = chatRepository.getArchivedChats()
    .map { it.size }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
```
2. В `ChatsScreen.kt` в `LazyColumn` после основного контента добавь:
```kotlin
if (archivedCount > 0) {
    item {
        TextButton(
            onClick = { /* navigate to archived */ },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.Archive, null)
            Spacer(Modifier.width(8.dp))
            Text("Archive ($archivedCount)", color = rcq.textSecondary)
        }
    }
}
```

---

## ФАЗА 4 — Разрешения и нативные функции

### Задача 4.1 — Разрешение RECORD_AUDIO перед звонком
**Файл:** `ui/calls/CallScreen.kt`

**Что сделать:** в начале `CallScreen` composable:
```kotlin
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CallScreen(...) {
    val audioPermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        if (!audioPermission.status.isGranted) audioPermission.launchPermissionRequest()
    }

    if (!audioPermission.status.isGranted) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.MicOff, null, Modifier.size(48.dp))
                Text("Microphone permission required for calls")
                Button(onClick = { audioPermission.launchPermissionRequest() }) { Text("Grant Permission") }
            }
        }
        return
    }
    // ... остальной код CallScreen
}
```

---

### Задача 4.2 — Разрешение ACCESS_FINE_LOCATION в NearbyScreen
**Файл:** `ui/contacts/NearbyScreen.kt`

**Что сделать:** аналогично задаче 4.1, добавь permission guard с `android.Manifest.permission.ACCESS_FINE_LOCATION`.

---

### Задача 4.3 — Deep links обработка
**Файл:** `ui/MainActivity.kt`, `AndroidManifest.xml`

**Что сделать:**
1. В `AndroidManifest.xml` внутри `<activity android:name=".ui.MainActivity">` добавь:
```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <category android:name="android.intent.category.BROWSABLE"/>
    <data android:scheme="rcq"/>
</intent-filter>
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <category android:name="android.intent.category.BROWSABLE"/>
    <data android:scheme="https" android:host="rcq.app"/>
</intent-filter>
```
2. В `MainActivity.handleIntent()`:
```kotlin
intent.data?.let { uri ->
    when {
        // rcq://add/123456789 или https://rcq.app/u/123456789
        (uri.scheme == "rcq" && uri.host == "add") ||
        (uri.host == "rcq.app" && uri.pathSegments.getOrNull(0) == "u") -> {
            uri.lastPathSegment?.toLongOrNull()?.let { uin ->
                pendingScreen.value = "add_contact_$uin"
            }
        }
        // rcq://group/42 или https://rcq.app/g/42
        (uri.scheme == "rcq" && uri.host == "group") ||
        (uri.host == "rcq.app" && uri.pathSegments.getOrNull(0) == "g") -> {
            uri.lastPathSegment?.let { pendingScreen.value = "join_group_$it" }
        }
    }
}
```
3. В `RCQApp.kt` в `LaunchedEffect` обработай новые значения `initialScreen`.

---

## ФАЗА 5 — Иконки, статусы и UI полировка

### Задача 5.1 — Исправить ICQ статус-иконки (убрать reflection)
**Файл:** `ui/common/StatusIndicator.kt`

**Проблема:** reflection `R.drawable::class.java.getField(name)` падает в release builds (R8 минификация).

**Что сделать:** замени `statusIconIds` map через reflection на прямые ссылки:
```kotlin
import com.rcq.messenger.R

private fun statusDrawableId(status: UserStatus): Int? = when (status) {
    UserStatus.ONLINE    -> R.drawable.status_00
    UserStatus.AWAY      -> R.drawable.status_01
    UserStatus.BUSY      -> R.drawable.status_02
    UserStatus.DND       -> R.drawable.status_03
    UserStatus.INVISIBLE -> R.drawable.status_07
    UserStatus.OFFLINE   -> R.drawable.status_08
    else -> null
}
```
Удали `private val statusIconIds: Map<UserStatus, Int> by lazy { ... }`.

---

### Задача 5.2 — Story ring на аватарах контактов
**Файл:** `ui/common/AvatarImage.kt` (создан в задаче 1.2)

**Что сделать:** параметр `hasStory: Boolean` уже предусмотрен. Реализуй градиентное кольцо:
```kotlin
if (hasStory) {
    Box(
        modifier = Modifier.size(size + 4.dp).clip(CircleShape)
            .background(
                Brush.sweepGradient(listOf(
                    Color(0xFFE91E63), Color(0xFFFF9800), Color(0xFFE91E63)
                ))
            )
    )
    // Аватар с отступом 2dp внутри кольца
}
```

---

### Задача 5.3 — Группы без иконки статуса участников (убрать ложный кружок)
**Файл:** `ui/chat/ChatsScreen.kt` или `ui/chat/inbox/` рендер InboxRow

**Проблема:** для групповых чатов `row.status == null` но StatusIndicator может вызываться без null-проверки и показывать OFFLINE кружок.

**Что сделать:** найди все места рендера `StatusIndicator` в контексте InboxRow и оберни:
```kotlin
row.status?.let { status ->
    StatusIndicator(status = status, size = 10, ...)
}
```

---

## ФАЗА 6 — Самостоятельно найденные дополнительные баги

### Задача 6.1 — `syncChats()` — только drain очереди, нет восстановления чатов
**Файл:** `data/repository/ChatRepository.kt` строки ~629-631

**Проблема:**
```kotlin
suspend fun syncChats(): Result<Unit> = runCatching {
    syncOfflineQueue()  // ← это ВСЁ
}
```
При первом запуске список чатов пуст до прихода первого WS-сообщения.

**Что сделать:** после `syncOfflineQueue()` добавь восстановление чатов из сообщений:
```kotlin
// Материализуй чаты из MessageEntity для контактов где чата ещё нет
val existingTargetIds = chatDao.getAllChats().map { it.targetId }.toSet()
contactDao.getAllContacts()
    .filter { it.userId !in existingTargetIds }
    .forEach { contact ->
        val latestMsg = messageDao.getLatestMessageForChat("direct_${contact.userId}")
        if (latestMsg != null) {
            chatDao.insertChat(ChatEntity(
                id = "direct_${contact.userId}",
                targetId = contact.userId,
                targetNickname = contact.customNickname ?: contact.nickname,
                targetAvatar = contact.avatarUrl,
                lastMessageContent = latestMsg.content,
                lastMessageTimestamp = latestMsg.timestamp,
                lastMessageKind = latestMsg.kind,
                updatedAt = latestMsg.timestamp
            ))
        }
    }
```
Если метода `getLatestMessageForChat` нет в `MessageDao` — добавь:
```kotlin
@Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 1")
suspend fun getLatestMessageForChat(chatId: String): MessageEntity?
```

---

### Задача 6.2 — `ChatEntity.targetNickname` не обновляется при смене ника контакта
**Файл:** `data/repository/ChatRepository.kt` → обработчик WS события presence/contact_updated

**Проблема:** контакт меняет ник — в списке чатов остаётся старое имя.

**Что сделать:** при обработке WS-события обновления контакта:
```kotlin
// В ContactRepository или ChatRepository WS handler:
val chatId = "direct_${peerUin}"
chatDao.getChat(chatId)?.let { chat ->
    if (chat.targetNickname != newNickname) {
        chatDao.insertChat(chat.copy(targetNickname = newNickname))
    }
}
```

---

### Задача 6.3 — `forwardMessage()` в ChatViewModel показывает ошибку пользователю
**Файл:** `ui/chat/ChatsViewModel.kt`

**Проблема:**
```kotlin
fun forwardMessage(message: Message) {
    _sendError.value = "Forward: coming soon"
}
```

**Что сделать:** проверь что в `ChatScreen.kt` строка ~322 использует `onForward = { showForwardPicker = message }` (не вызывает `viewModel.forwardMessage()`). Если `forwardMessage()` больше нигде не нужен — удали его. `ForwardPickerDialog` и `forwardMessageTo()` уже реализованы.

---

### Задача 6.4 — Reflection в `statusIconIds` падает в production (R8/ProGuard)
**Уже описана в Задаче 5.1.** Это одна и та же проблема.

---

### Задача 6.5 — DataStore extension объявлен в ViewModel (multiple instance risk)
**Файл:** `ui/auth/AuthViewModel.kt` первые строки файла

**Проблема:**
```kotlin
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")
```
Должен быть объявлен один раз на уровне приложения.

**Что сделать:**
1. Удали эту строку из `AuthViewModel.kt`.
2. Убедись что в `di/AppModule.kt` есть singleton DataStore. Если нет — добавь:
```kotlin
@Provides @Singleton
fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("rcq_prefs") }
    )
```
3. `AuthViewModel` уже получает `DataStore<Preferences>` через конструктор — убедись что это тот же singleton.

---

### Задача 6.6 — `getUser()` всегда вызывает неправильный endpoint
**Файл:** `data/repository/ChatRepository.kt` → `UserRepository.getUser()`

**Проблема:** `GET /users/{id}` не работает с UIN — Android всегда передаёт UIN, не internal DB id.

**Что сделать:**
```kotlin
suspend fun getUser(userId: Long): Result<User> = getUserByUin(userId)
```

---

### Задача 6.7 — `PUT /users/me` передаёт полный User объект
**Файл:** `data/api/RCQApiService.kt`

**Проблема:** сервер ожидает PATCH с отдельными полями.

**Что сделать:**
1. Добавь в `RCQApiService.kt`:
```kotlin
@Serializable
data class UpdateProfileRequest(
    val nickname: String? = null,
    val bio: String? = null,
    @SerialName("avatar_media_id") val avatarMediaId: String? = null,
    @SerialName("status_message") val statusMessage: String? = null
)

@PATCH("users/me")
suspend fun patchProfile(@Body request: UpdateProfileRequest): Response<User>
```
2. В `UserRepository.updateProfile()` используй новый `patchProfile()`.

---

### Задача 6.8 — Мёртвые REST endpoints для сообщений (compile-time guard)
**Файл:** `data/api/RCQApiService.kt`

**Проблема:** `getMessages()`, `sendMessage()`, `editMessage()`, `deleteMessage()` — эти endpoints не существуют на сервере.

**Что сделать:** пометь `@Deprecated(level = DeprecationLevel.ERROR)`:
```kotlin
@Deprecated(
    "Server has no REST message endpoints. Use POST /messages/sealed",
    level = DeprecationLevel.ERROR
)
@GET("chats/{id}/messages")
suspend fun getMessages(...): Response<List<Message>>
```
Это вызовет compile error при случайном использовании.

---

### Задача 6.9 — `originalSender = "User"` при пересылке сообщений
**Файл:** `ui/chat/ChatScreen.kt` строка ~362

**Что сделать:**
```kotlin
// Заменить:
originalSender = "User"
// На:
originalSender = if (msg.isFromMe) "Me"
                 else msg.senderName ?: msg.senderId.toString()
```

---

### Задача 6.11 — Статус не передаётся на сервер (ИСПРАВЛЕНО вручную)
**Статус:** ✅ Исправлено в трёх файлах.

**Root cause (три независимых бага):**

**Баг А** — `PresenceUpdateRequest` не содержал `status_message`.  
iOS всегда шлёт `{ "status": "online", "status_message": null }`.  
Android слал `{ "status": "online" }` — сервер мог отклонять запрос с 422.  
**Файл:** `data/api/RCQApiService.kt` → добавлено поле `status_message: String? = null`.

**Баг Б** — `WebSocketService.onOpen()` не уведомлял о переподключении.  
После каждого reconnect (sleep, сетевой сбой, kill WS) сервер сбрасывает статус в offline.  
Никакого `POST /presence/status` после reconnect не происходило.  
**Файл:** `data/websocket/WebSocketService.kt` → добавлен `_connectedSignal: MutableSharedFlow<Unit>`, emit в `onOpen()`.

**Баг В** — `AuthViewModel` не подписывался на переподключение WS.  
`_currentStatus` хранил выбранный статус локально, но не пересылал его после reconnect.  
**Файл:** `ui/auth/AuthViewModel.kt` → `init {}` теперь подписывается на `connectedSignal` и вызывает `updatePresence(_currentStatus.value)` при каждом успешном подключении.

**Бонус (Codex уже применил):** `MainActivity.onStop()` → `updatePresence("away")`, `onStart()` → `updatePresence("online")`.

---

### Задача 6.10 — Badge counter на иконке приложения не обновляется
**Файл:** `data/repository/ChatRepository.kt` + `service/NotificationHelper.kt`

**Проблема:** iOS обновляет badge при каждом сообщении. Android не обновляет.

**Что сделать:** в `NotificationHelper.kt` при создании notification:
```kotlin
// Уже создаёшь notification — добавь number:
val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    // ... существующие поля
    .setNumber(unreadCount)  // обновляет badge на Samsung/MIUI/etc.
    .build()
```
При открытии чата вызывай `NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)` чтобы сбросить badge.

---

## ПРОВЕРКА ПОСЛЕ КАЖДОЙ ФАЗЫ

```bash
# После любой фазы — базовая проверка:
./gradlew kspDebugKotlin

# После добавления WorkManager (Фаза 2.3):
./gradlew assembleDebug

# Полная сборка:
./gradlew assembleDebug

# Тесты:
./gradlew test
```

---

## ИТОГОВАЯ ПРИОРИТИЗАЦИЯ ДЛЯ CODEX (сортировка по Impact/Effort)

| # | Задача | Файл | Сложность | Пользователь видит |
|---|--------|------|-----------|-------------------|
| 1 | 5.1 Direct status drawable refs | StatusIndicator.kt | Низкая | Иконки статусов работают в release |
| 2 | 6.6 getUser→getUserByUin | ChatRepository.kt | Низкая | Профиль открывается |
| 3 | 1.4 ContactInfo→getUserByUin | ContactInfoViewModel.kt | Низкая | Профиль открывается |
| 4 | 1.3 Recovery phrase copy | RCQApp.kt | Низкая | Кнопка копирует |
| 5 | 6.9 originalSender fix | ChatScreen.kt | Низкая | Правильный отправитель |
| 6 | 6.5 DataStore singleton | AuthViewModel.kt | Низкая | Стабильность |
| 7 | 6.3 Forward message fix | ChatsViewModel.kt | Низкая | Пересылка работает |
| 8 | 6.8 Deprecated dead endpoints | RCQApiService.kt | Низкая | Защита от ошибок |
| 9 | 1.2 AvatarImage + Coil | AvatarImage.kt + экраны | Средняя | Видны аватары |
| 10 | 1.1 MediaMessageBubble Coil | MediaMessageBubble.kt | Средняя | Видны фото |
| 11 | 1.6 lastMessage при WS events | ChatRepository.kt | Средняя | Превью в списке чатов |
| 12 | 1.5 UIN→nickname | ChatViewModel.kt | Средняя | Имена вместо цифр |
| 13 | 2.1 onResume WS reconnect | MainActivity.kt | Средняя | Сообщения приходят |
| 14 | 2.2 Away на onStop | MainActivity.kt | Низкая | Правильный статус |
| 15 | 1.7 memberCount в Group | GroupRepository.kt | Средняя | Кол-во участников |
| 16 | 4.1 RECORD_AUDIO permission | CallScreen.kt | Низкая | Звонки работают |
| 17 | 4.2 LOCATION permission | NearbyScreen.kt | Низкая | Nearby работает |
| 18 | 3.4 Nearby button | ContactsScreen.kt | Низкая | Видна кнопка |
| 19 | 3.1 Call fullscreen | RCQApp.kt | Низкая | Нет bottom bar в звонке |
| 20 | 6.7 PUT→PATCH updateProfile | RCQApiService.kt | Средняя | Обновление профиля |
| 21 | 6.1 syncChats восстановление | ChatRepository.kt | Средняя | Список не пустой |
| 22 | 6.2 targetNickname обновление | ChatRepository.kt | Средняя | Актуальные имена |
| 23 | 5.3 Null status guard | ChatsScreen.kt | Низкая | Нет ложного offline |
| 24 | 3.5 Archive UI | ChatsScreen.kt | Средняя | Секция архива |
| 25 | 3.2 CallMiniBar | CallMiniBar.kt | Высокая | Мини-бар звонка |
| 26 | 2.3 WorkManager sync | MessageSyncWorker.kt | Высокая | Фоновые сообщения |
| 27 | 4.3 Deep links | MainActivity + Manifest | Высокая | rcq:// ссылки |
| 28 | 5.2 Story ring | AvatarImage.kt | Средняя | Story индикатор |
| 29 | 6.10 Badge counter | NotificationHelper.kt | Средняя | Badge на иконке |
| 30 | 3.3 AudioRoom nav fix | RCQApp.kt | Низкая | Комнаты открываются |
