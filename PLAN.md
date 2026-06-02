Теперь у меня полная картина. Буду писать документ, опираясь на реальный код проекта.

---
RCQ Android — Production Migration & Architecture Document

Версия: 1.0 | Автор: Staff/Principal Mobile Architect | Дата: 2026-05-29
Аудитория: Engineering Leadership, Android Team, iOS Team, Product, QA

---
ЧАСТЬ 1 — Стратегия миграции

1.1 Decision Matrix: Подход к миграции

┌──────────────────────────┬──────────┬────────────────────┬──────────────────────┬───────────────┐
│          Подход          │ Fit для  │       Риски        │       Скорость       │ Рекомендуется │
│                          │   RCQ    │                    │                      │               │
├──────────────────────────┼──────────┼────────────────────┼──────────────────────┼───────────────┤
│ Big Bang (переписать     │ Низкий   │ Критические —      │ Высокая изначально,  │ ❌            │
│ всё)                     │          │ production пауза   │ медленная в конце    │               │
├──────────────────────────┼──────────────────────────┼───────────────┤
  * migration checklist system
  * parity tracking matrix
  * QA signoff flow
  * release readiness score
  * technical debt scoring
  * risk tracking system

  Добавь:

  * примеры таблиц
  * статусных схем
  * dependency graphs
  * execution tracking

  ---

  # ЧАСТЬ 16 — Формат ответа

  Ответ должен быть:

  * extremely detailed
  * practical
  * production-grade
  * executable
  * engineering-level deep
  * without fluff
  * structured like real architecture documentation

  Разрешается:

  * таблицы
  * matrices
  * dependency graphs
  * module trees
  * phased plans
  * risk matrices
  * implementation examples
  * directory trees
  * ADR examples
  * parity examples

  НЕ давай абстрактные советы.
  Все рекомендации должны быть применимы к production messenger app с realtime communication.


Сначала изучу текущее состояние проекта, чтобы все рекомендации были точными и применимыми.

  Read 4 files, listed 1 directory, ran 1 shell command

Теперь у меня полная картина. Буду писать документ, опираясь на реальный код проекта.

---
RCQ Android — Production Migration &

Версия: 1.0 | Автор: Staff/Principal 6-05-29
Аудитория: Engineering Leadership, Android Team, iOS Team, Product, QA

---
ЧАСТЬ 1 — Стратегия миграции

1.1 Decision Matrix: Подход к миграци

┌──────────────────────────┬──────────┬────────────────────┬──────────────────────┬───────────────┐
│          Подход          │ Fit для  │       Риски        │       Скорость       │ Рекомендуется │
│                          │   RCQ                    │               │
├──────────────────────────┼──────────┼────────────────────┼──────────────────────┼───────────────┤
│ Big Bang (переписать     │ Низкий  кая изначально,  │ ❌            │
│ всё)                     │          │ production пауза   │ медленная в конце    │               │
├──────────────────────────┼──────────┼────────────────────┼──────────────────────┼───────────────┤
│ Incremental              │ Высокий  │ Умеренные —        │ Устойчивая           │ ✅            │
│ (feature-by-feature)     │          │ управляемые        │                      │               │
├──────────────────────────┼──────────┼────────────────────┼──────────────────────┼───────────────┤
│ Vertical Slice           │ Средний няя              │ ⚠️ частично   │
├──────────────────────────┼──────────┼────────────────────┼──────────────────────┼───────────────┤
│ Shared Kotlin            │ Низкий  енная            │ ❌            │
│ Multiplatform            │          │ рано               │                      │               │
└──────────────────────────┴──────────┴────────────────────┴──────────────────────┴───────────────┘

Рекомендуется: Incremental + Vertical

Причина: Android-версия уже частично существует с рабочим крипто-слоем, E2EE, Room. Это не greenfield. Нам нужна стабилизация существующего, а не переписывание — только целенаправленная замена плохих слоёв (UI, state management, WS, sync).

1.2 Anti-Corruption Layer (ACL)

Текущая проблема: GameRepository, PetRepository, Marketplace-сущности живут в коде вместе с core messaging. Это платформенный drift от iOS.

┌─────────────────────────────────────────────────────────┐
│                  Application Boundary                    │
│  ┌─────────────────────┐  ┌───────────────────────────┐ │
│  │   Core Messaging    │  │    Remo
│  │  (iOS-compatible)   │  │  Games/Pets/Marketplace   │ │
│  │                     │  │  → DELETE per contract    │ │
│  └──────────┬──────────┘  └───────────────────────────┘ │
│             │ ACL: API contracts from ios ref             │
│  ┌──────────▼──────────────────────────────────────────┐ │
│  │           RCQ Backend (API source of truth)          │ │
│  └──────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘

ACL правило: любая фича, не существующая в iOS ref-клиенте (reference/ios), не реализуется на Android. Backend API — единственный contract.

1.3 Behavioral Parity Strategy

Три уровня:

Level 1 — Protocol Parity (non-negotiable):
- ECIES v=1 wire format идентичен iOS
- Signal Protocol session setup идентичен
- Message envelope structure идентична
- Auth flow (ключи, UIN, registration bundle) идентичен

Level 2 — Behavioral Parity (high pri
- Порядок сообщений (server_time, не System.currentTimeMillis())
- Offline queue draining (те же retry rules)
- WebSocket reconnect timing
- Unread counter logic
- Delivery state transitions

Level 3 — UI Parity (medium priority):
- Layout приближен к iOS, с Material 3 адаптацией
- Анимации адаптированы (не клонированы — это невозможно)
- Gesture patterns нативны для Android

1.4 Platform Drift Prevention

iOS ref (reference/ios/) ──► Behavioral Contract Tests
                              (что iOS делает → тест на Android)

Backend API spec ──────────► Android

WS event spec ─────────────► Both pla

Правило: любое изменение backend API или wire format → синхронное обновление Android и iOS.

1.5 Sequencing Rationale

Week 1-2: Stabilize (fix what's broken — WS, sync, ordering)
Week 3-4: Foundation (modularize, design system, clean arch)
Week 5-8: Feature parity (complete Tier 1 missing pieces)
Week 9-12: Polish (Tier 2, animations, pixel-perfect)
Week 13+: Advanced features (calls, stories, Tier 3-4)

Почему такой порядок: нельзя строить дизайн-систему на нестабильном фундаменте. Крашащий WS → плохой UX независимо от красоты UI.

---
ЧАСТЬ 2 — Android Architecture

2.1 Target Architecture

┌────────────────────────────────────────────────────────────────┐
│                    Presentation Lay │
│  ┌────────────────┐  ┌─────────────────┐  ┌────────────────┐  │
│  │   Compose UI   │  │  ViewModels     │  │  UI State      │  │
│  │   (screens)    │◄─┤  (MVI/UDF)
│  └────────────────┘  └────────┬────
│                               │ Int │
├───────────────────────────────▼────┤
│                     Domain Layer                                │
│  ┌────────────────┐  ┌─────────────
│  │   Use Cases    │  │  Domain Models  │  │  Repo Ifaces   │  │
│  │   (business    │  │  (pure Kotli
│  │    logic)      │  └─────────────
│  └────────┬───────┘                │
├───────────▼────────────────────────┤
│                      Data Layer     │
│  ┌──────────┐  ┌──────────┐  ┌─────
│  │  Room DB │  │ Retrofit │  │ WebS
│  │  (local) │  │   API    │  │  Eng
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘  │
│  ┌─────────────────────────────────│
│  │              Sync Engine (offlin │
│  └─────────────────────────────────│
├────────────────────────────────────┤
│                    Crypto Layer      │
│  ┌──────────────┐  ┌──────────────┐
│  │ CryptoService│  │  EciesCrypto │  │  SignalProtocol      │ │
│  │ (facade)     │  │  (iOS compat)│
│  └──────────────┘  └──────────────┘  └──────────────────────┘ │
└────────────────────────────────────┘

2.2 Modularization Strategy

Текущее состояние: monolith. Всё в :app.

Target структура (градуальная, не big-bang):

:app                    # thin shell, DI wiring, navigation root
:core:ui                # design system, tokens, primitives
:core:domain            # domain mode
:core:data              # Room, Retrofit, DataStore, Sync engine
:core:crypto            # Signal, ECIES, key management
:core:network           # OkHttp, Webtor
:core:testing           # test utilities, fakes, fixtures

:feature:auth           # welcome, registration, recovery
:feature:chat           # chats list,
:feature:contacts       # contacts, requests, groups
:feature:profile        # profile, se
:feature:calls          # voice/video
:feature:stories        # stories (if

Порядок модуляризации (не делать всё
1. Сначала :core:crypto — уже изолирован логически
2. Затем :core:network — WS engine
3. Затем :feature:auth — наименее свя
4. Остальное — по мере стабилизации

2.3 MVI/UDF Pattern

Сейчас в проекте: частичный MVVM без чёткого UDF. MutableStateFlow напрямую мутируется в VM.

Целевой паттерн:

// Контракт для каждого экрана:
interface UdfViewModel<State, Intent, Effect> {
    val state: StateFlow<State>
    fun send(intent: Intent)
    val effects: Flow<Effect>  // one-shot side effects
}

// Пример для ChatScreen:
sealed class ChatIntent {
    data class SendMessage(val text: String) : ChatIntent()
    data class LoadMessages(val chatI
    object MarkRead : ChatIntent()
    data class ReactToMessage(val msgId: String, val emoji: String) : ChatIntent()
}

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isSendingMessage: Boolean = false,
    val draft: String = "",
    val typingUsers: Set<Long> = emptySet(),
    val error: String? = null
)

sealed class ChatEffect {
    object ScrollToBottom : ChatEffect()
    data class ShowError(val message: String) : ChatEffect()
    object MessageSent : ChatEffect()
}

2.4 WebSocket Architecture

Критическая проблема в текущем коде: тации:
- data/websocket/WebSocketService.kt (новый, typed events WsEvent)
- data/ws/WebSocketManager.kt (старый, WebSocketEvent)

Это deadlock для development — нужно немедленно удалить WebSocketManager.

Целевая архитектура WS:

// Единый WS engine:
@Singleton
class WebSocketEngine @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val dataStore: DataStore<Preferences>
) {
    // Connection state machine
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val sessionId: String) : ConnectionState()
        data class Reconnecting(val a) : ConnectionState()
        data class Error(val cause: Throwable) : ConnectionState()
    }

    val connectionState: StateFlow<ConnectionState>
    val events: SharedFlow<WsEvent>  // hot stream, replay=0

    // Exponential backoff: 1s, 2s, 4s, 8s, 16s, max 60s
    // Network-aware: ConnectivityManager callback resets backoff
    // Keepalive: ping every 25s, disconnect if pong missing for 60s
}

Replay buffer для missed events при reconnect:

// При reconnect: запросить missed ev
suspend fun fetchMissedEvents(since: Long): List<WsEvent>

2.5 Offline-First Architecture

Сейчас: нет. Приложение ломается без сети.

Target: optimistic updates + sync queue:

User action (send msg)
       │
       ▼
Local DB write (status=PENDING)
       │
       ├─── UI updates instantly (optimistic)
       │
       ▼
Outbox Queue (SQLite table: pending_messages)
       │
       ├─── WS connected? → send immediately
       │
       └─── WS disconnected? → wait for reconnect, then drain queue
                               (FIFO, with retry count, max 5 retries)

Конфликт-резолюция: server_time — единственный источник истины для ordering. Локальный timestamp используется только как fallback display.

2.6 Room Schema — Целевое состояние

messages          # core message store
chats             # conversation metadata
contacts          # address book + block list
groups            # group metadata + membership
users             # user cache (TTL-based)
signal_keys       # identity/session keys
pending_outbox    # NEW: offline send
delivery_receipts # NEW: ACK tracking
typing_states     # NEW: ephemeral (in-memory, не persist)

Outbox table:
@Entity(tableName = "pending_outbox")
data class PendingOutboxEntity(
    @PrimaryKey val localId: String,
    val chatId: String,
    val targetUin: Long,
    val plaintextJson: String,
    val retryCount: Int = 0,
    val maxRetries: Int = 5,
    val createdAt: Long,
    val nextRetryAt: Long
)

2.7 Security Architecture

Сейчас: EncryptedSharedPreferences для токена. Достаточно для MVP.

Production требования:
- JWT token: EncryptedSharedPreferences (API 23+) ✅
- Signal keys: Room с шифрованием через SQLCipher или отдельный EncryptedFile
- ECIES private key: Android Keystore System — ключ в hardware-backed keystore, шифрование происходит в TEE
- Biometric: BiometricPrompt + CryptoObject для разблокировки master key
- Certificate pinning: OkHttp CertificatePinner для api.rcq.app
- WebSocket: WSS only, same pinning

// Certificate pinning (добавить в Ok
CertificatePinner.Builder()
    .add("api.rcq.app", "sha256/YOUR_CERT_HASH")
    .add("api.rcq.app", "sha256/BACKUP_CERT_HASH")
    .build()

2.8 Performance Targets

┌────────────────────────────────────────┬────────────────┬─────────────────────┐
│                Метрика                 │     Target     │   Текущий статус    │
├────────────────────────────────────────┼────────────────┼─────────────────────┤
│ Cold start                             │ < 1.5s         │ ~2-3s (не измерено) │
├────────────────────────────────────────┼────────────────┼─────────────────────┤
│ Warm start                             │ < 500ms        │ Unknown             │
├───────────────────────────────────────────────────┤
│ Compose recomposition per scroll frame │ 0 unnecessary  │ Unknown             │
├────────────────────────────────────────┼────────────────┼─────────────────────┤
│ WS reconnect (network change)          │ < 2s           │ ~5-10s              │
├────────────────────────────────────────┼────────────────┼─────────────────────┤
│ Message list scroll                    │ 60fps constant │ Unknown             │
├────────────────────────────────────────┼────────────────┼─────────────────────┤
│ Message send → local display           │ < 100ms        │ ~200ms est          │
└────────────────────────────────────────┴────────────────┴─────────────────────┘

---
ЧАСТЬ 3 — Анализ существующего проекта

3.1 Что СОХРАНИТЬ (хорошее)

┌────────────────────────────────┬──────────────────────────┬───────────────────────────────────┐
│           Компонент            │             Причина              │
├────────────────────────────────┼──────────────────────────┼───────────────────────────────────┤
│ crypto/EciesCrypto.kt          │ ✅-compatible, тщательно отлажен │
├────────────────────────────────┼──────────────────────────┼───────────────────────────────────┤
│ crypto/EciesKeyStore.kt        │ ✅вильная генерация ключей       │
├────────────────────────────────┼──────────────────────────┼───────────────────────────────────┤
│ crypto/CryptoService.kt        │ ✅ Сохранить             │ Facade pattern, правильный        │
├────────────────────────────────┼──────────────────────────┼───────────────────────────────────┤
│ crypto/SessionManager.kt       │ ✅ Сохранить             │ Signal protocol sessions          │
├────────────────────────────────┼──────────────────────────┼───────────────────────────────────┤
│ data/api/RCQApiService.kt      │ ✅rofit interface                │
├────────────────────────────────┼──────────────────────────┼───────────────────────────────────┤
│ data/db/ (все DAO)             │ ✅ Сохранить + migr      │ Room схема достаточная            │
├────────────────────────────────┼──────────────────────────┼───────────────────────────────────┤
│ di/AppModule.kt                │ ✅ Сохранить + модуляриз │ Hilt wiring правильный            │
├────────────────────────────────┼──────────────────────────┼───────────────────────────────────┤
│ service/NotificationHelper.kt  │ ✅ Сохранить             │ Уже исправлен                     │
├────────────────────────────────┼──────────────────────────┼───────────────────────────────────┤
│ domain/model/                  │ ✅ Сохранить             │ Domain models правильные          │
├────────────────────────────────┼──────────────────────────────────┤
│ Build system (Gradle KTS, KSP) │ ✅ Сохранить             │ Современный stack                 │
└────────────────────────────────┴──────────────────────────┴───────────────────────────────────┘

3.2 Что ПЕРЕПИСАТЬ (проблемное)

┌───────────────────────────────────┬───┬───────────────┐
│             Компонент             │           Проблема            │   Приоритет   │
├───────────────────────────────────┼───────────────────────────────┼───────────────┤
│ data/ws/WebSocketManager.kt       │ ДУБЛИРУЕТ WebSocketService.kt │ 🔴 НЕМЕДЛЕННО │
├───────────────────────────────────┼───┼───────────────┤
│ ui/chat/ChatsViewModel.kt         │ Нет UDF, нет error handling   │ 🔴 Высокий    │
├───────────────────────────────────┼───────────────────────────────┼───────────────┤
│ ui/chat/ChatScreen.kt             │ Нет state contracts           │ 🔴 Высокий    │
├───────────────────────────────────┼───┼───────────────┤
│ data/repository/ChatRepository.kt │ Нет outbox, нет offline       │ 🟡 Средний    │
├───────────────────────────────────┼───────────────────────────────┼───────────────┤
│ domain/model/WebSocketEvent.kt    │   │ 🔴 НЕМЕДЛЕННО │
├───────────────────────────────────┼───────────────────────────────┼───────────────┤
│ ui/theme/                         │ Нет design tokens             │ 🟡 Средний    │
├───────────────────────────────────┼───┼───────────────┤
│ Navigation в RCQApp.kt            │ Нет deep link handling        │ 🟡 Средний    │
├───────────────────────────────────┼───────────────────────────────┼───────────────┤
│ ui/games/GamesScreen.kt           │ УДАЛИТЬ (per contract)        │ 🔴 НЕМЕДЛЕННО │
├───────────────────────────────────┼───────────────────────────────┼───────────────┤
│ ui/market/MarketplaceScreen.kt    │ УДАЛИТЬ (per contract)        │ 🔴 НЕМЕДЛЕННО │
├───────────────────────────────────┼───────────────────────────────┼───────────────┤
│ data/repository/GameRepository.kt │ УДАЛИТЬ (per contract)        │ 🔴 НЕМЕДЛЕННО │
├───────────────────────────────────┼───┼───────────────┤
│ domain/model/Game.kt + Pet.kt     │ УДАЛИТЬ (per contract)        │ 🔴 НЕМЕДЛЕННО │
└───────────────────────────────────┴───────────────────────────────┴───────────────┘

3.3 Architecture Smell Checklist

✅ = OK   ⚠️ = Warning   ❌ = Problem

Data Layer:
  ❌ Два WS implementation (WebSocketService + WebSocketManager)
  ❌ Нет offline outbox queue
  ❌ Нет sync engine (pull + push интеграция)
  ⚠️ ChatRepository содержит UI-level логику (navigation, state)
  ❌ Message ordering — System.currentTimeMillis() вместо server_time
  ⚠️ Нет TTL для user cache (UserDao)

Presentation Layer:
  ❌ ViewModels напрямую мутируют MutableStateFlow без UDF
  ❌ Нет sealed Intent/Effect contrac
  ⚠️ ChatViewModel — fat (media, voice, messages, UI state в одном)
  ❌ Нет screen-level error recovery

Crypto:
  ✅ ECIES v=1 iOS-compatible
  ✅ Signal Protocol sessions
  ⚠️ Key rotation не реализован

Infrastructure:
  ❌ Нет crash reporting (Crashlytics
  ❌ Нет performance monitoring
  ❌ Нет feature flags
  ❌ Нет analytics events
  ❌ Нет logging framework (только android.util.Log)
  ❌ Нет network inspector в debug build

Build:
  ✅ KSP вместо kapt
  ✅ R8/ProGuard для release
  ⚠️ Нет build variants для staging/p
  ❌ Нет CI/CD конфигурации

Testing:
  ❌ Нет unit тестов для ViewModels
  ❌ Нет unit тестов для repositories
  ✅ CryptoServiceTest упоминается в CLAUDE.md
  ❌ Нет screenshot тестов
  ❌ Нет WS integration тестов

3.4 Migration Readiness Checklist

Criteria                           St
─────────────────────────────────────────────────────
Stable build                       ✅ YES    -
iOS API DTO parity                 ⚠️ ~70%   No
E2EE iOS compatibility             ✅
WebSocket stable connection        ❌ NO     YES — WS 500 known bug
Message ordering correct           ❌ NO     YES
Offline send queue                 ❌)
Push notifications working         ✅ YES    -
Group membership filtering         ❌ NO     Yes (shows wrong groups)
No duplicate WS implementations    ❌ NO     YES — causes state bugs
Dead code removed (games/pets)     ❌ NO     Minor blocker
Design system tokens               ❌ NO     No (future)
CI/CD pipeline                     ❌ NO     No (future)

---
ЧАСТЬ 4 — Roadmap разработки

Phase 0: Emergency Stabilization (Неделя 1-2)

Goals: убрать критические баги, удалить мёртвый код, стабилизировать WS.

Features: нет новых. Только стабилизация.

Technical Tasks:

1. DELETE: WebSocketManager.kt, WebSocketEvent.kt — оставить только WebSocketService.kt + WsEvent
2. DELETE: GameRepository.kt, GamesScreen.kt, MarketplaceScreen.kt, PetDao.kt, PetEntity.kt, Pet.kt,
Marketplace.kt, Game.kt, StoryDao.kt,сли Stories нет в iOS ref)
3. FIX: WebSocket 500 error — диагностировать handshake, проверить auth header format
4. FIX: Message ordering — заменить System.currentTimeMillis() на server_time из envelope
5. FIX: Group membership filtering — добавить JOIN с membership таблицей или фильтр по ownUin
6. ADD: pending_outbox table + OutboxProcessor для offline send
7. ADD: Build variant: staging с api.staging.rcq.app

Dependencies: нет

Risks: удаление кода может сломать DI граф — тщательно проверить Hilt граф после удаления

Exit Criteria:
- WS подключается без 500
- Сообщения в правильном порядке
- Группы отображают только членство
- Offline send работает

QA: ручное тестирование на реальном устройстве, logcat monitoring

Metrics:
- WS connection success rate: > 95%
- Message ordering: 100% правильный
- Build: 0 errors

---
Phase 1: Architecture Foundation (Неделя 3-4)

Goals: заложить правильную архитектур будущем.

Technical Tasks:

1. MVI/UDF: рефакторинг ChatsViewModetern (Intent/State/Effect)
2. Design Tokens: создать core/ui/tokens/ — SpacingTokens, ColorTokens, TypographyTokens, ShapeTokens
3. Navigation: вынести nav graph в отдельный файл, добавить deep link support
4. Error Handling: единый ErrorHandlenline error states
5. Logging: заменить android.util.Log на структурированный logger с tag management:

// Пример: Timber или кастомный:
object RCQLogger {
    fun d(tag: String, msg: String, vararg args: Any?)
    fun e(tag: String, throwable: Throwable, msg: String)
    // В debug: logcat. В release: буфер для crash report attachment
}

6. Build Variants:
// build.gradle.kts:
flavorDimensions += "environment"
productFlavors {
    create("staging") {
        buildConfigField("String", "API_BASE_URL", "\"https://api.staging.rcq.app/\"")
        applicationIdSuffix = ".stagi
        versionNameSuffix = "-staging"
    }
    create("production") {
        buildConfigField("String", "API_BASE_URL", "\"https://api.rcq.app/\"")
    }
}

Exit Criteria:
- ChatsViewModel/ChatViewModel следуют UDF
- Design tokens файлы созданы и используются
- Deep links работают
- Staging build существует и собирает

---
Phase 2: Tier 1 Feature Completion (Неделя 5-8)

Goals: полный Tier 1 feature set из Части 5.

Features:

┌───────────────────────────────────────┬──────────┬────────────┐
│                Feature                │ Priority │   Status   │
├────────────────────────────────────
│ Auth + session management             │ P0       │ ✅ Done    │
├───────────────────────────────────────┼──────────┼────────────┤
│ WebSocket core                        │ P0       │ ⚠️ Buggy   │
├───────────────────────────────────────┼──────────┼────────────┤
│ Direct messaging                      │ P0       │ ⚠️ Buggy   │
├───────────────────────────────────────┼──────────┼────────────┤
│ Group messaging                       │ P0       │ ⚠️ Buggy   │
├───────────────────────────────────────┼──────────┼────────────┤
│ Realtime updates
├───────────────────────────────────────┼──────────┼────────────┤
│ Delivery states (sent/delivered/read) │ P0       │ ⚠️ Partial │
├───────────────────────────────────────┼──────────┼────────────┤
│ Local DB + offline send               │ P0       │ ❌ Missing │
├───────────────────────────────────────┼──────────┼────────────┤
│ Unread counters
├───────────────────────────────────────┼──────────┼────────────┤
│ Push notifications                    │ P1       │ ✅ Done    │
├───────────────────────────────────────┼──────────┼────────────┤
│ WS reconnection
├───────────────────────────────────────┼──────────┼────────────┤
│ Message ordering
├───────────────────────────────────────┼──────────┼────────────┤
│ Retry queue
└───────────────────────────────────────┴──────────┴────────────┘

Technical Tasks:

1. Sync Engine — SyncEngine.kt:
@Singleton
class SyncEngine @Inject constructor(
    private val api: RCQApiService,
    private val chatRepository: ChatR
    private val wsEngine: WebSocketEngine
) {
    // Pull on app start: fetch missed messages since last_sync_timestamp
    suspend fun syncOnStart()
    // Push: drain outbox when WS rec
    suspend fun drainOutbox()
    // Subscribe to WS events and rou
    fun observeAndRoute()
}

2. Delivery State Machine:
PENDING → SENT → DELIVERED → READ
           ↓
         FAILED (max retries exceeded

3. Unread Counter:
// MessageDao:
@Query("SELECT COUNT(*) FROM messages is_read = 0 AND sender_uin !=:ownUin")
fun getUnreadCount(chatId: String, ow

4. Typing Indicator:
// In-memory only, не persist:
@Singleton
class TypingStateManager @Inject constructor() {
    private val _typingStates = MutableStateFlow<Map<String, Set<Long>>>(emptyMap())

    fun onTypingStarted(chatId: Strin
    fun onTypingStopped(chatId: String, userId: Long)
    // Auto-clear after 5 seconds (как iOS)
}

Exit Criteria:
- Все Tier 1 features работают offline и online
- Message delivery: PENDING → READ state machine полный
- Unread counters точные
- Retry queue drain при reconnect

QA: Manual test matrix с iOS клиентом на одном аккаунте

Metrics:
- Message delivery rate: > 99%
- Offline → online sync: < 3s для dra

---
Phase 3: Communication Features (Неделя 9-12)

Goals: Tier 2 features.

Features:
- Reactions, editing, deleting
- Typing states (UI)
- Presence/status
- Search (local full-text)
- Drafts
- Replies + forwards
- Pinned messages
- Archived chats
- Mute settings
- Media: photos, videos, voice

Technical Tasks:

1. Media Pipeline:
Image select → Compress (Coroutine, IO dispatcher) → Upload API → Send URL in message
                                                    ↓ (failure)
                                               Retry queue (same as outbox)

2. Voice Messages:
// VoiceRecorder уже есть, нужно интегрировать с сообщениями
// Формат: OGG Opus (как iOS использует m4a — проверить API spec)

3. Full-text search:
// Room FTS4:
@Fts4(contentEntity = MessageEntity::class)
@Entity(tableName = "messages_fts")
class MessageFts

@Query("SELECT * FROM messages WHERE messages_fts WHERE messages_fts MATCH:query)")
fun searchMessages(query: String): Flow<List<MessageEntity>>

4. Presence система:
// Lifecycle-aware:
// App foreground → POST /presence { status: "online" }
// App background > 5 min → POST /pre
// explicit set → POST /presence { status: "dnd" }

Exit Criteria:
- Все Tier 2 features работают
- Media upload/download стабильный
- Search возвращает результаты < 200ms

---
Phase 4: Advanced Features + Polish (Неделя 13+)

Goals: Tier 3-4, visual polish, parity.

Features:
- Voice calls (WebRTC, уже есть инфраструктура)
- Video calls
- Animations + transitions
- Visual parity с iOS
- Performance tuning
- Accessibility

Exit Criteria:
- Calls работают stable
- Screenshot parity тесты проходят
- Cold start < 1.5s
- 60fps везде

---
ЧАСТЬ 5 — Приоритизация функционально

Tier 1 — Critical Core (блокирует release)

Priority | Feature
─────────────────────────────────────────────────────────────────
P0       | Registration + JWT auth        | ✅ Working
P0       | ECIES key generation           | ✅ Working
P0       | WebSocket connection
P0       | Send direct message            | ⚠️ Works with bugs
P0       | Receive direct message
P0       | Message ordering (server_time) | ❌ Broken
P0       | Local DB persistence           | ✅ Working
P0       | Offline outbox queue           | ❌ Missing
P1       | Group messaging send/recei
P1       | Unread counters               | ⚠️ Partial
P1       | Push notifications             | ✅ Working
P1       | WS reconnect + backoff
P1       | Delivery states (✓ ✓✓)        | ⚠️ Partial
P1       | Contacts sync                  | ✅ Working
P1       | Message retry on failure

Tier 2 — Communication Features (после Tier 1 done)

P2 | Photo/video messages         |  ts)
P2 | Voice messages               | ⚠️ Partial (VoiceRecorder exists)
P2 | Message reactions            | ❌ Missing
P2 | Message editing              | ❌ Missing
P2 | Message deletion             |
P2 | Typing indicator (send/recv) | ❌ Missing (WsEvent exists)
P2 | Presence/online status       |
P2 | Search messages              | ❌ Missing
P2 | Draft messages               |
P2 | Reply to message             | ⚠️ ReplyPreview exists, not wired
P2 | Forward message              | ❌ Missing
P2 | Deep links                   | ⚠️ Partial
P2 | Pinned messages              | ❌ Missing
P2 | Archive chats                | ❌ Missing
P2 | Mute notifications per chat  |

Tier 3 — Advanced

P3 | Voice calls (WebRTC)        |
P3 | Video calls                  | ❌ Missing
P3 | Audio rooms                  | ⚠️ Service exists
P3 | Background media upload      |
P3 | Smart notification bundling  | ❌ Missing

Tier 4 — Polish

P4 | Compose animations           | ❌ Basic only
P4 | Haptic feedback              |
P4 | Screen transitions           |
P4 | Visual parity pixel-perfect  | 🟡 In progress conceptually
P4 | Accessibility (a11y)         | ❌ Not started

---
ЧАСТЬ 6 — Deep Dive по major features

6.1 WebSocket Engine

Complexity: High
Implementation Risks: Connection drops mid-send, missed events during reconnect, auth token expiry
Hidden Edge Cases:
- Network change (WiFi → LTE) без disconnect event
- Server-side timeout без close frame
- Concurrent WS messages race condition
- Auth token expiry во время active session

Performance Concerns: Один WS для всех событий — правильно. Не использовать polling.

Battery Concerns: Ping interval 25s — оптимален. Меньше — battery drain. Больше — timeout risk.

Offline Concerns: Outbox queue нужен. Missed event recovery через /messages?since= endpoint.

Observability:
// Логировать:
wsEngine.connectionState.collect { state ->
    analytics.track("ws_state_change", mapOf("state" to state::class.simpleName))
}

Rollback: WS implementation не требует rollback — connection-level failures изолированы.

---
6.2 ECIES/Signal E2EE

Complexity: Very High
Implementation Risks: Key mismatch между iOS и Android, session corruption, pre-key exhaustion

Hidden Edge Cases:
- Первое сообщение без pre-key bundle (TOFU)
- Pre-key exhaustion (сервер отдаёт null)
- Session reset после reinstall
- Group message с частично недоступными ключами участников

Data Consistency Concerns:
- Signal session state — stateful. При DB corruption → все сессии сброшены
- Pre-key IDs должны быть уникальны и монотонны

Backend Dependencies:
- /keys/prekey/{uin} — должен быть доступен всегда
- Сервер должен репленить pre-keys когда < 10 осталось

Observability:
// Логировать успех/провал дешифрова
logger.d("ECIES", "decrypt success for uin=$senderUin, msgId=$msgId")
logger.e("ECIES", error, "decrypt failed for uin=$senderUin")

QA Complexity: Очень высокая — нужен iOS-Android cross-device test для каждого типа сообщений.

---
6.3 Offline Outbox Queue

Complexity: Medium
Implementation Risks: Duplicate sends на reconnect, ordering violation при batch drain

Hidden Edge Cases:
- App killed во время drain — можно ли повторно отправить? (нужны idempotency keys)
- Message expires (сервер reject) — нужен DLQ (dead letter queue)
- Conflicting local IDs

Idempotency Design:
data class PendingOutboxEntity(
    @PrimaryKey val localId: String,к client_msg_id
    // server deduplicates by client_msg_id
)

Performance Concerns: Drain должен бrdering, parallel across chats.

Offline Concerns: Core feature, должен работать полностью offline.

---
6.4 Group Messaging

Complexity: High
Implementation Risks: Fan-out encrypership sync lag

Hidden Edge Cases:
- Новый участник группы — не может читать старые сообщения (правильно, не исправлять)
- Участник исключён — дальнейшие попытки дешифрования падают
- Группа из 100+ участников — ECIES fan-out медленный

Data Consistency Concerns:
- Membership state между клиентом и сервером может расходиться
- Group metadata кешируется — TTL нужен

Backend Dependencies:
- /groups/{id}/members должен быть б
- Membership webhook через WS

QA Strategy:
- Test: создать группу на iOS, написать с Android, проверить на iOS
- Test: исключить участника, проверить что сообщения не доставляются

---
6.5 Push Notifications

Complexity: Medium
Implementation Risks: FCM token rotation, notification при active chat (нужно подавить), deep link
navigation state

Hidden Edge Cases:
- Нотификация при открытом чате того же диалога → не показывать
- Нотификация → тап → приложение already open → navigate to correct chat
- Multiple notifications → group into conversation group

Battery Concerns: FCM высокоприоритетные push → wakelock для 10s. Используем только для сообщений.

Offline Concerns: FCM работает когда приложение убито. Нужен FirebaseMessagingService.onMessageReceived.

---
6.6 Media (Photos/Video)

Complexity: High
Implementation Risks: Upload progress tracking, partial uploads, large file OOM

Implementation:
PhotoPicker API (API 33+) / SAF fallback
→ Compress (Dispatchers.IO, Coroutin
→ Upload to /media/upload (multipart)
→ Get URL → send in message body
→ Download: Coil + disk cache + memory cache

Performance Concerns:
- Decode large images на UI thread → Coil handles this ✅
- Video thumbnails — lazy generation

Offline Concerns: Upload queue (часть Outbox, но с file path ссылкой)

---
ЧАСТЬ 7 — Pixel-perfect parity с iOS

7.1 Принцип Platform-Native Parity

Неправильный подход: "скопировать UIKit layout в Compose"
Правильный подход: "достичь того же тивными Android инструментами"

Примеры:
- iOS: NavigationView back swipe → Android: Predictive Back (Android 13+) + BackHandler
- iOS: UIContextMenu → Android: DropdownMenu + long press
- iOS: Haptic feedback (UIImpactFeedbackGenerator) → Android: VibrationEffect
- iOS: UITableView scroll → Android:

7.2 Design Token Architecture

// core/ui/tokens/

object RCQSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    // Chat-specific
    val messageBubbleHorizontalPadding = 12.dp
    val messageBubbleVerticalPadding
    val messageListItemHeight = 72.d
    val avatarSizeMd = 40.dp
    val avatarSizeSm = 32.dp
}

object RCQColors {
    // Semantic tokens (не hex напря
    val sentMessageBackground = Color(0xFF0066CC)    // match iOS
    val receivedMessageBackground =
    val onlineDot = Color(0xFF34C759
    val awayDot = Color(0xFFFF9500)    // iOS yellow
    val offlineDot = Color(0xFF8E8E9

    // Dynamic (light/dark)
    val chatBackground: Color @Composable get() =
        if (isSystemInDarkTheme()) CxFFF2F2F7)
}

object RCQTypography {
    val messageBody = TextStyle(font.sp)
    val chatListTitle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    val chatListSubtitle = TextStyle(fontSize = 15.sp, color = RCQColors.secondaryText)
    val timestamp = TextStyle(fontSize = 13.sp)
}

7.3 Parity Matrix

┌───────────────────────┬──────────────────────────────┬────────────────────────────┬───────────────┐
│        Элемент        │      iOS Implementation      │   Android Implementation   │    Status     │
├───────────────────────┼──────────────────────────────┼───────────────┤
│ Message bubble colors │ Blue(own) / Gray(other)      │ Compose Box + tokens       │ 🟡 Partial    │
├───────────────────────┼──────────────────────────────┼────────────────────────────┼───────────────┤
│ Message timestamp     │ Right-bottom в bubble        │ Same                       │ 🟡 Partial    │
├───────────────────────┼──────────────────────────────┼───────────────┤
│ Delivery checkmarks   │ ✓ / ✓✓ / blue ✓✓             │ Icon composable            │ ⚠️ Needs work │
├───────────────────────┼──────────────────────────────┼───────────────┤
│ Avatar                │ Circular с initials fallback │ Coil + placeholder         │ ✅            │
├───────────────────────┼──────────────────────────────┼────────────────────────────┼───────────────┤
│ Online indicator      │ Green dot overlay            │ StatusIndicator.kt         │ ✅            │
├───────────────────────┼──────────────────────────────┼────────────────────────────┼───────────────┤
│ Typing indicator      │ ... animated                 │ Missing                    │ ❌            │
├───────────────────────┼──────────────────────────────┼───────────────┤
│ Chat list swipe       │ Swipe → delete/mute          │ Missing                    │ ❌            │
├───────────────────────┼──────────────────────────────┼────────────────────────────┼───────────────┤
│ Navigation back       │ Swipe from left edge         │ Android back gesture       │ ✅ native     │
├───────────────────────┼──────────────────────────────┼────────────────────────────┼───────────────┤
│ Context menu (msg)    │ UIContextMMenu on long press │ ❌            │
├───────────────────────┼──────────────────────────────┼────────────────────────────┼───────────────┤
│ Search bar            │ UISearchController           │ SearchBar compose          │ ❌            │
└───────────────────────┴──────────────────────────────┴────────────────────────────┴───────────────┘

7.4 Screenshot Parity Testing

// Paparazzi для screenshot тестов:
class ChatScreenParityTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material3.DayNight"
    )

    @Test
    fun chatList_matchesDesign() {
        paparazzi.snapshot {
            ChatsScreen(
                chats = fakeChatList,
                onChatClick = {}
            )
        }
    }
}

Процесс parity review:
1. iOS designer делает screenshot референс
2. Android screenshot тест генерирует baseline
3. Diff tool сравнивает
4. PR не мержится если visual diff >

7.5 Team Process

Design ──► Figma tokens (shared) ──►d adaptation
                                                         ──► QA cross-platform sign-off

Android dev ──► Implement feature ──► Screenshot test ──► iOS team review
                                   ──► Behavioral test ──► Product sign-off
                                   ─

Approval workflow:
- Visual parity: QA + Design sign-of
- Behavioral parity: iOS dev + Android dev cross-review
- Protocol parity (crypto/WS): senior review mandatory

---
ЧАСТЬ 8 — Что делать ПРЯМО СЕЙЧАС

Sprint 0 (Дни 1-5): Emergency Fixes

День 1-2: Удалить мёртвый код

# Файлы для немедленного удаления:
rm app/src/main/java/com/rcq/messengt
rm app/src/main/java/com/rcq/messenger/domain/model/WebSocketEvent.kt
rm app/src/main/java/com/rcq/messengtory.kt
rm app/src/main/java/com/rcq/messenger/ui/games/GamesScreen.kt
rm app/src/main/java/com/rcq/messengn.kt
rm app/src/main/java/com/rcq/messenger/domain/model/Game.kt
rm app/src/main/java/com/rcq/messenger/domain/model/Pet.kt
rm app/src/main/java/com/rcq/messenger/domain/model/Marketplace.kt
rm app/src/main/java/com/rcq/messeng
rm app/src/main/java/com/rcq/messenger/data/db/StoryDao.kt

После удаления: исправить Hilt граф,.

День 2-3: Fix WS 500 error

Диагностика:
# На устройстве:
adb logcat | grep "WebSocket\|OkHttp\|WS"

Проверить: auth header format в ws://api.rcq.app/ws/{uin}?token={jwt} — точно ли совпадает с тем что ожидает сервер.

День 3-4: Fix message ordering

// В MessageEntity:
val serverTime: Long  // из envelope, это canonical timestamp

// В MessageDao:
@Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY server_time ASC")
fun getMessages(chatId: String): Flo

День 4-5: Fix group membership filtering

// В GroupDao добавить:
@Query("""
    SELECT g.* FROM groups g
    INNER JOIN group_members gm ON g.id = gm.group_id
    WHERE gm.user_uin = :ownUin
""")
fun getMyGroups(ownUin: Long): Flow<List<GroupEntity>>

---
Sprint 1 (Дни 6-10): Offline Outbox

Кто: Android dev
Dependencies: Sprint 0 complete

Task 1: Room migration добавить pend
Task 2: OutboxProcessor — drain logic
Task 3: Интеграция с WebSocketEngine reconnect event
Task 4: Тест: kill app → reconnect → messages delivered

---
Sprint 2 (Дни 11-15): Build Infrastructure

Кто: Android dev + DevOps (если есть)
Dependencies: Sprint 0

Task 1: Добавить build variants (staging/production)
Task 2: Настроить GitHub Actions:
# .github/workflows/android.yml:
# trigger: PR to main
# steps: setup-java → gradle build → gradle test → upload APK artifact
Task 3: Добавить Timber logging framework
Task 4: Добавить debugImplementation("com.squareup.leakcanary:leakcanary-android:2.x") для memory leak detection

---
Quick Wins (параллельно)

┌───────────────────────────────────────────────┬────────┬──────────────────────┐
│                     Task                      │ Время  │        Impact        │
├──────────────────────────────────────────────────┤
│ Удалить дублирующий WS                        │ 1 час  │ Убирает класс багов  │
├───────────────────────────────────────────────┼────────┼──────────────────────┤
│ Убрать System.currentTimeMillis() из ordering │ 2 часа │ Fix ordering bug     │
├───────────────────────────────────────────────┼────────┼──────────────────────┤
│ Добавить LeakCanary в debug                   │ 30 мин │ Найдёт утечки памяти │
├──────────────────────────────────────────────────┤
│ Добавить @VisibleForTesting аннотации         │ 1 час  │ Unlocks unit testing │
├───────────────────────────────────────────────┼────────┼──────────────────────┤
│ Добавить Timber                               │ 1 час  │ Structured logging   │
└───────────────────────────────────────────────┴────────┴──────────────────────┘

---
High-Risk Areas (требуют особого вни

1. WS auth handshake — если сервер обновился, Android может отставать
2. ECIES key rotation — если iOS нач упадёт
3. Room migrations — каждая migration должна тестироваться на реальных данных
4. Hilt DI граф — после удаления модулей граф легко ломается

---
ЧАСТЬ 9 — CI/CD и Release Engineering

9.1 Branch Strategy: Trunk-Based Development

main (trunk) — всегда deployable, pr
├── feature/ws-reconnect (short-lived, max 2 days)
├── feature/offline-outbox (short-lived)
├── fix/message-ordering (short-lived)
└── release/1.0.0 (release branch, cut from main)

Правило: feature branches живут максимум 2 дня. Нет → merge конфликты, divergence.

Feature flags для незаконченных features (не hotfix):
// RemoteConfig или local flags:
object FeatureFlags {
    val REACTIONS_ENABLED = BuildConfig.DEBUG  // только в debug пока не готово
    val VOICE_CALLS_ENABLED = false  // ждём стабилизации
}

9.2 CI Pipeline

# Каждый PR:
jobs:
  validate:
    - ktlint (style)
    - detekt (static analysis)
    - ./gradlew kspDebugKotlin (KSP compilation check)

  test:
    - ./gradlew testDebugUnitTest
    - ./gradlew connectedDebugAndroidTest (если emulator доступен)

  build:
    - ./gradlew assembleDebug
    - ./gradlew assembleRelease (без signing для PR)
    - Upload APK artifact

  screenshot_tests:
    - ./gradlew verifyPaparazziDebug (parity regression check)

9.3 Release Process

1. Cut release branch: git checkout -b release/1.0.0
2. Version bump: versionCode++, vers
3. Release build: ./gradlew assembleRelease (signed)
4. Internal testing track (Google Play) — team
5. Alpha track — 5% users
6. Beta track — 20% users (watch crash rate 48h)
7. Production — staged 1% → 5% → 20% → 50% → 100%
8. Каждый этап: минимум 24h wait + мониторинг crash rate

9.4 Production SLAs

┌───────────────────────────────────┬──────────────┬────────────────────────┐
│              Метрика              eshold     │
├───────────────────────────────────┼──────────────┼────────────────────────┤
│ Crash-free rate                   │ ≥ 99.5%      │ < 99.0% → stop rollout │
├───────────────────────────────────┼──────────────┼────────────────────────┤
│ ANR rate                          p rollout  │
├───────────────────────────────────┼──────────────┼────────────────────────┤
│ Cold start                        │ ≤ 1.5s       │ > 2.5s → P2 bug        │
├───────────────────────────────────┼──────────────┼────────────────────────┤
│ WS reconnect after network change g          │
├───────────────────────────────────┼──────────────┼────────────────────────┤
│ Message send → server ACK         │ ≤ 500ms      │ > 2s → alert           │
├───────────────────────────────────┼──────────────┼────────────────────────┤
│ Message receive latency (WS)      │ ≤ 200ms      │ > 1s → alert           │
├───────────────────────────────────┼──────────────┼────────────────────────┤
│ Offline → online sync             │ ≤ 3s         │ > 10s → P2 bug         │
├───────────────────────────────────┼──────────────┼────────────────────────┤
│ Push delivery                     t          │
└───────────────────────────────────┴──────────────┴────────────────────────┘

9.5 Hotfix Process

Issue detected (crash spike)
       │
       ▼
Create hotfix/crash-{description} fr
       │
       ▼
Fix, test on staging
       │
       ▼
PR: hotfix → release AND hotfix → main
       │
       ▼
Emergency release build + expedited Play review
       │
       ▼
Staged rollout: 1% → monitor 1h → если OK → 100%

9.6 Rollback Strategy

- Google Play: staged rollout позволяет halt + rollback
- Feature flags: disable flag → effective immediate rollback для flagged features
- WS protocol changes: versioned envelopes, backward compatible

---
ЧАСТЬ 10-13 — Repository Documentation Architecture

10.1 Структура /docs

/docs
├── MASTER_INDEX.md           # Entrks
├── AI_CONTEXT.md             # Claude reads first — project state, conventions
├── CURRENT_STATE.md          # What works, what's broken RIGHT NOW
├── NEXT_STEPS.md             # Immediate action items
│
├── /architecture
│   ├── ARCHITECTURE_OVERVIEW.md    # This document (condensed)
│   ├── DATA_FLOW.md                # Message lifecycle diagram
│   ├── WEBSOCKET_PROTOCOL.md       # WS event spec
│   ├── CRYPTO_SPEC.md
│   ├── SYNC_ENGINE.md              # Offline-first sync design
│   └── MODULARIZATION.md           # Module structure + rules
│
├── /adr
│   ├── ADR-001-single-module.md    # Why monolith now (incremental modularization)
│   ├── ADR-002-ecies-v1.md         t
│   ├── ADR-003-ws-engine.md        # Why unified WS vs dual
│   ├── ADR-004-room-vs-realm.md    # DB choice rationale
│   └── ADR-005-mvi-udf.md
│
├── /roadmap
│   ├── ROADMAP.md                  # This section (condensed)
│   ├── PHASE_0_STATUS.md           # Emergency stabilization progress
│   ├── PHASE_1_STATUS.md           # Foundation progress
│   └── PHASE_2_STATUS.md           # Feature completion progress
│
├── /features
│   ├── FEATURE_MATRIX.md           # All features + iOS/Android status
│   ├── messaging/
│   │   ├── OFFLINE_OUTBOX.md       # Design + implementation notes
│   │   ├── DELIVERY_STATES.md      # State machine
│   │   └── GROUP_MESSAGING.md      # ECIES fan-out design
│   ├── auth/
│   │   └── ACCOUNT_RECOVERY.md     )
│   └── crypto/
│       └── SIGNAL_ECIES.md         # Key management lifecycle
│
├── /parity
│   ├── PARITY_STATUS.md            ty matrix
│   ├── VISUAL_PARITY.md            # Screenshot comparison status
│   └── BEHAVIORAL_PARITY.md
│
├── /migration
│   ├── MIGRATION_PROGRESS.md       cking
│   └── IOS_REFERENCE_MAPPING.md    # iOS file → Android equivalent
│
├── /tech-debt
│   └── TECH_DEBT_REGISTER.md       # Scored, prioritized debt items
│
├── /qa
│   ├── TEST_MATRIX.md              # Manual test cases
│   ├── CROSS_PLATFORM_TESTS.md     # iOS-Android interaction tests
│   └── SCREENSHOT_BASELINES.md     # Paparazzi baseline info
│
├── /release
│   ├── RELEASE_READINESS.md        # Current release gate status
│   └── RELEASE_HISTORY.md          # Past releases + metrics
│
├── /ai-context
│   ├── SESSION_HANDOFF.md          # What Claude needs to know at session start
│   ├── KNOWN_ISSUES.md             # Current known bugs
│   ├── DECISIONS_LOG.md            # Why certain decisions were made
│   └── CONVENTIONS.md              # Code conventions for this project
│
├── /observability
│   ├── LOGGING_GUIDE.md            # Log tags + levels
│   └── METRICS_DASHBOARD.md        # What to monitor
│
└── /runbooks
    ├── WS_RECONNECT_DEBUG.md        # How to debug WS issues
    ├── CRYPTO_SESSION_RESET.md      # How to handle session corruption
    └── RELEASE_PROCESS.md           # Step-by-step release guide

10.2 Обязательные документы

MASTER_INDEX.md

- Purpose: единая точка входа для людей и AI
- Ownership: tech lead
- Update frequency: при каждом изменении структуры
- AI relevance: CRITICAL — Claude читает первым
- Human relevance: CRITICAL — onboarding new engineers

Содержит: ссылки на все docs, текущий phase, quick status.

AI_CONTEXT.md

- Purpose: то что Claude должен знать в начале каждой сессии
- Ownership: вся команда (каждый пишет что открыл)
- Update frequency: после каждой значимой сессии
- AI relevance: CRITICAL — первое что Claude читает
- Human relevance: Medium

# AI Context — RCQ Android

## Project State (updated: 2026-05-2
- Branch: phase-1-core-messaging
- Current bugs: WS 500, message ordering broken, group filter missing
- DO NOT implement: games, pets, marketplace (per contract)
- iOS ref: reference/ios/ (gitignored, available locally)
- API source of truth: docs/RCQ_API_SPEC.md

## Conventions
- Commit messages: Russian
- Push after every commit: git push origin phase-1-core-messaging
- WS implementation: WebSocketServict DELETED)
State/Effect

## Critical Files
- Crypto: crypto/EciesCrypto.kt, crypto/EciesKeyStore.kt (DO NOT BREAK)
- WS: data/websocket/WebSocketService.kt (single source of truth)
- DB: data/db/RCQDatabase.kt (check migration version before changing)

CURRENT_STATE.md

- Purpose: честный snapshot что работает и что нет
- Ownership: dev lead
- Update frequency: ежедневно или после каждого merge
- AI relevance: HIGH — Claude использует для приоритизации

# Current State — 2026-05-29

## Working ✅
- Registration + auth
- ECIES crypto (iOS compatible)
- Push notifications
- Contact sync

## Broken ❌
- WS connection (500 error on /ws/{uin})
- Message ordering (uses local time instead of server_time)
- Group filtering (shows all server groups, not just member groups)
- Incoming direct messages sometimes

## Missing ❌
- Offline outbox queue
- Message retry on failure
- Typing indicators
- Presence system

TECH_DEBT_REGISTER.md

# Tech Debt Register

| ID | Item | Severity | Effort | Owner | Added |
|---|---|---|---|---|---|
| TD-001 | Duplicate WS implementations | Critical | 2h | - | 2026-05-29 |
| TD-002 | No offline outbox | High | 3d | - | 2026-05-29 |
| TD-003 | system.currentTimeMillis for ordering | High | 2h | - | 2026-05-29 |
| TD-004 | Fat ChatViewModel | Medium | 1d | - | 2026-05-29 |
| TD-005 | No UDF pattern in ViewModels | Medium | 2d | - | 2026-05-29 |
| TD-006 | No crash reporting | Medium | 4h | - | 2026-05-29 |
| TD-007 | No unit tests | High | ongoing | - | 2026-05-29 |

FEATURE_MATRIX.md

# Feature Matrix

| Feature | iOS | Android | Notes |
|---|---|---|---|
| Registration | ✅ | ✅ | |
| ECIES E2EE | ✅ | ✅ | iOS-compatible verified |
| Direct messaging | ✅ | ⚠️ | Receive sometimes missing |
| Group messaging | ✅ | ⚠️ | Filter bug |
| Message reactions | ✅ | ❌ | |
| Message editing | ✅ | ❌ | |
| Voice messages | ✅ | ⚠️ | VoiceRe
| Voice calls | ✅ | ⚠️ | WebRTC infra exists |
| Typing indicator | ✅ | ❌ | |
| Presence/status | ✅ | ❌ | |
| Stories | ✅/❌ | ❌ | Check iOS ref |
| Games | ❌ | ❌ | NOT in scope |

10.3 AI Session Handoff Process

Что Claude читает первым (идеальный порядок):

1. CLAUDE.md — project overview, build commands, conventions
2. docs/ai-context/AI_CONTEXT.md — current bugs, what NOT to implement, key files
3. docs/CURRENT_STATE.md — что работ
4. docs/NEXT_STEPS.md — конкретные immediate tasks
5. docs/ai-context/DECISIONS_LOG.md — почему так, а не иначе
6. docs/tech-debt/TECH_DEBT_REGISTER.md — текущий технический долг

Что обновляется автоматически:
- CURRENT_STATE.md — разработчик обновляет после каждого significant change
- FEATURE_MATRIX.md — обновляется при реализации/поломке feature

Что обновляется вручную:
- ADR/*.md — только при архитектурных решениях
- DECISIONS_LOG.md — при нетривиальных выборах
- MIGRATION_PROGRESS.md — еженедельно

Как минимизировать потерю контекста:
1. В конце каждой Claude-сессии: обновить CURRENT_STATE.md
2. Любой нетривиальный выбор → запис
3. Новый баг → запись в KNOWN_ISSUES.md
4. Merge → обновить FEATURE_MATRIX.md

Handoff template:
## Session Handoff — 2026-05-29 22:0

### Completed this session
- Fixed WS 500 error: был неправильн
- Deleted WebSocketManager.kt

### Left for next session
- Implement OutboxProcessor
- Fix group membership filter

### State of codebase
- Branch: phase-1-core-messaging (ahushed)
- Build: passing
- Known issues: message ordering still uses local time (TD-003)

---
ЧАСТЬ 14 — ADR / RFC / Governance

14.1 ADR Format

# ADR-003: Единый WebSocket Engine

**Date:** 2026-05-29
**Status:** Accepted
**Supersedes:** -
**Superseded by:** -

## Context
Проект содержал два WS implementation: WebSocketManager (старый)
и WebSocketService (новый). Это создавало state inconsistency и
дублирование event routing.

## Decision
Удалить WebSocketManager.kt. WebSocketService.kt — единственный WS engine.
WsEvent (sealed class) — единственный event model.

## Consequences
Positive: единственный event stream, нет state race conditions.
Negative: необходимо обновить все потребители WebSocketManager.

## Alternatives Considered
- Оставить оба, задепрекейтить старый — rejected (drag на годы)
- Объединить в один файл — rejected (нет value над simple deletion)

ADR Lifecycle:
Proposed → Under Review → Accepted → Implemented → Superseded

Naming: ADR-{number}-{kebab-slug}.md

Approval: ADR требует sign-off от tech lead + 1 senior dev

14.2 RFC Process (для крупных изменений)

RFC нужен для:
- Изменения DB schema с migration
- Новый крипто-механизм
- Новый сетевой protocol
- Breaking API change
- Новый feature module

Не нужен:
- Bugfix
- UI изменения
- Рефакторинг без breaking changes

Процесс:
1. Draft RFC в /docs/rfc/RFC-{N}-{slug}.md
2. Post ссылку в team chat для review
3. Review период: 48h минимум
4. Если нет возражений → Accepted
5. Если есть → Discussion → Revised → Re-review

14.3 Release Gates

Gate 1 — Code Complete:
  ✅ All features in scope merged
  ✅ No P0/P1 open bugs
  ✅ Unit test coverage > 60%

Gate 2 — QA Sign-off:
  ✅ Manual test matrix executed
  ✅ Cross-platform iOS-Android test passed
  ✅ No regression vs previous version

Gate 3 — Release Readiness:
  ✅ Crash-free rate > 99.5% on beta
  ✅ ANR rate < 0.5%
  ✅ Staging environment validated
  ✅ Rollback plan documented

Gate 4 — Staged Rollout:
  1% → 24h wait → metrics OK → 5% → 0%

---
ЧАСТЬ 15 — Tracking & Execution Syst

15.1 Feature Completion Matrix

Feature                    | Impl | Unit Test | Integration | Screenshot | iOS Parity | GA Ready
───────────────────────────────────────────────────────────────────────────────────────────────
Registration               |  ✅  |    ❌     |     ❌      |     ❌     |    ✅      |    ⚠️
Direct messaging           |  ⚠️  |    ❌     |     ❌      |     ❌     |    ⚠️      |    ❌
Group messaging            |  ⚠️  |    ❌     |     ❌      |     ❌     |    ⚠️      |    ❌
WebSocket                  |  ⚠️  | N/A     |    ⚠️      |    ❌
Offline outbox             |  ❌  |    ❌     |     ❌      |    N/A     |    ❌      |    ❌
Push notifications         |  ✅  |    ❌     |     ❌      |     ❌     |    ✅      |    ⚠️
Delivery states            |  ⚠️  |    ❌     |     ❌      |     ❌     |    ⚠️      |    ❌

15.2 Risk Tracking Matrix

┌────────────────────────────────┬────────────────────────┬───────────┐
│              Risk              │ Probability │  Impact  │         Mitigation         │   Owner   │
├────────────────────────────────┼─────────────┼──────────┼────────────────────────────┼───────────┤
│ WS server-side protocol change │ Medium      │ Critical │ API versioning, test with  │ Android   │
│                                │             │          │ staging                    │ dev       │
├────────────────────────────────┼─────────────┼──────────┼────────────────────────────┼───────────┤
│ ECIES iOS incompatibility      │ Low         │ Critical │ Cross-platform tests       │ Crypto    │
│ after iOS update               │                        │ lead      │
├────────────────────────────────┼─────────────┼──────────┼────────────────────────────┼───────────┤
│ Room migration corruption      │ Ltion tests with       │ Android   │
│                                │             │          │ snapshot                   │ dev       │
├────────────────────────────────┼─────────────┼──────────┼────────────────────────────┼───────────┤
│ FCM token churn (reinstalls)   │ Medium      │ Medium   │ Server-side token refresh  │ Backend   │
├────────────────────────────────┼────────────────────────┼───────────┤
│ Play Store review delay        │ Low         │ Medium   │ Submit early, keep backup  │ PM        │
├────────────────────────────────┼─────────────┼──────────┼────────────────────────────┼───────────┤
│ Kotlin/Compose version upgrade │ Low         │ Medium   │ Freeze versions during     │ Android   │
│  breaking                      │             │          │ stabilization              │ dev       │
└────────────────────────────────┴─────────────┴──────────┴────────────────────────────┴───────────┘

15.3 Definition of Done

# Definition of Done — Feature

A feature is DONE when:
1. [ ] Implementation complete and code reviewed
2. [ ] Unit tests written and passing (ViewModels, use cases, DAOs)
3. [ ] Integration test: works with real server (staging)
4. [ ] Cross-platform test: iOS send versa)
5. [ ] Screenshot test baseline added (if UI feature)
6. [ ] FEATURE_MATRIX.md updated
7. [ ] CURRENT_STATE.md updated
8. [ ] No P0/P1 bugs introduced (ver
9. [ ] PR merged + pushed
10. [ ] Commit message in Russian, conventional format

15.4 Technical Debt Scoring

Score = Severity (1-5) × Frequency (1-5) × Fix Difficulty (inverse: 5=easy, 1=hard)

TD-001 (Duplicate WS): 5 × 5 × 5 = 125  ← FIX IMMEDIATELY
TD-002 (No outbox):    5 × 5 × 3 = 75   ← Fix this sprint
TD-003 (ordering):     5 × 5 × 5 = 125  ← FIX IMMEDIATELY
TD-004 (fat VM):       3 × 3 × 3 = 2
TD-005 (no UDF):       3 × 4 × 2 = 24   ← Schedule
TD-006 (no crash rpt): 4 × 2 × 5 = 4
TD-007 (no tests):     4 × 5 × 1 = 20   ← Long-term

---
Итог и немедленные действия

★ Insight ──────────────────────────
Критические находки из реального кода:
1. data/ws/WebSocketManager.kt и data/websocket/WebSocketService.kt — два параллельных WS engine. Это единственная наиболее опасная точка нестабильности. Удаление WebSocketManager должно быть первым коммитом.
2. domain/model/WebSocketEvent.kt соs 40+ в WsEvent. Весь routing идётмимо sealed class — это объясняет почему входящие сообщения "теряются".
3. System.currentTimeMillis() в messроблема clock skew между клиентами.Всегда server_time.
────────────────────────────────────

Топ-5 действий сегодня (в порядке пр

1. Удалить WebSocketManager.kt + WebSocketEvent.kt → единый WsEvent
2. Удалить GameRepository, GamesScreGame*
3. Диагностировать WS 500: adb logcat | grep WebSocket + сравнить auth header с iOS ref
4. Исправить message ordering: server_time из envelope вместо System.currentTimeMillis()
5. Создать docs/ai-context/AI_CONTEXдля session continuity

После этих 5 шагов: appликация станет стабильнее, кодовая база — меньше и понятнее, следующая Claude-сессия получит полный контекс
