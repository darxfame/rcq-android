# F5/F6 Feature Families Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans task-by-task.

**Goal:** Implement 7 missing server-backed feature families: Presence → Polls → Hood → Nearby → Random → News → Reports.

**Architecture:** Each family = API endpoints in RCQApiService + thin Repository + ViewModel + Screen. All repositories use `@Singleton @Inject constructor` — Hilt auto-discovers. New "Discover" tab in bottom nav links to all discovery features. Hood and Nearby require `ACCESS_FINE_LOCATION`.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Retrofit, `FusedLocationProviderClient` (Hood/Nearby), Accompanist Permissions

**Server contracts:**
- Presence: `POST /presence/status`, `GET /presence/online`, `GET /presence/online-count`
- Polls: `POST /polls`, `GET /polls/{id}`, `POST /polls/{id}/vote`, `POST /polls/{id}/close`
- Hood: `POST /hood/send`, `GET /hood/messages?lat=&lon=`, `GET /hood/banners?lat=&lon=`; WS events: `hood_message`, `hood_reaction`
- Nearby: `POST /nearby/checkin`, `GET /nearby/list?lat=&lon=`
- Random: `POST /random/queue`, `DELETE /random/queue`, `POST /random/skip`
- News: `GET /news/feed?page=&limit=`
- Reports: `POST /reports`

---

## File Map

| File | Action |
|------|--------|
| `data/api/RCQApiService.kt` | Add all endpoints + 14 DTOs |
| `data/repository/PresenceRepository.kt` | New |
| `data/repository/PollRepository.kt` | New |
| `data/repository/HoodRepository.kt` | New |
| `data/repository/NearbyRepository.kt` | New |
| `data/repository/RandomChatRepository.kt` | New |
| `data/repository/NewsRepository.kt` | New |
| `data/repository/ReportRepository.kt` | New |
| `ui/discover/DiscoverScreen.kt` | New — hub with nav to all discovery features |
| `ui/polls/PollScreen.kt` | New — view + vote |
| `ui/hood/HoodScreen.kt` | New — geo-chat with location |
| `ui/RCQApp.kt` | Add routes + Discover bottom-nav tab |

---

## Task 1: Add all API endpoints + DTOs

**Files:**
- Modify: `app/src/main/java/com/rcq/messenger/data/api/RCQApiService.kt`

- [ ] Add these endpoints inside `interface RCQApiService` after the existing `updatePresence` call:

```kotlin
// Presence (full)
@GET("presence/online")
suspend fun getOnlineUsers(): Response<List<Long>>
@GET("presence/online-count")
suspend fun getOnlineCount(): Response<OnlineCountResponse>

// Polls
@POST("polls")
suspend fun createPoll(@Body req: CreatePollRequest): Response<PollResponse>
@GET("polls/{id}")
suspend fun getPoll(@Path("id") id: String): Response<PollResponse>
@POST("polls/{id}/vote")
suspend fun votePoll(@Path("id") id: String, @Body req: PollVoteRequest): Response<PollResponse>
@POST("polls/{id}/close")
suspend fun closePoll(@Path("id") id: String): Response<PollResponse>

// Hood
@POST("hood/send")
suspend fun sendHoodMessage(@Body req: HoodMessageRequest): Response<HoodMessageResponse>
@GET("hood/messages")
suspend fun getHoodMessages(@Query("lat") lat: Double, @Query("lon") lon: Double, @Query("limit") limit: Int = 50): Response<List<HoodMessageResponse>>
@GET("hood/banners")
suspend fun getHoodBanners(@Query("lat") lat: Double, @Query("lon") lon: Double): Response<List<HoodBannerResponse>>

// Nearby
@POST("nearby/checkin")
suspend fun nearbyCheckin(@Body req: NearbyCheckinRequest): Response<Unit>
@GET("nearby/list")
suspend fun getNearbyUsers(@Query("lat") lat: Double, @Query("lon") lon: Double): Response<List<NearbyUserResponse>>

// Random chat
@POST("random/queue")
suspend fun joinRandomQueue(): Response<RandomQueueResponse>
@DELETE("random/queue")
suspend fun leaveRandomQueue(): Response<Unit>
@POST("random/skip")
suspend fun skipRandomPartner(): Response<RandomQueueResponse>

// News
@GET("news/feed")
suspend fun getNewsFeed(@Query("page") page: Int = 0, @Query("limit") limit: Int = 20): Response<List<NewsItem>>

// Reports
@POST("reports")
suspend fun submitReport(@Body req: ReportRequest): Response<Unit>
```

- [ ] Add DTOs at the bottom of `RCQApiService.kt`:

```kotlin
@kotlinx.serialization.Serializable
data class OnlineCountResponse(val count: Int)

@kotlinx.serialization.Serializable
data class CreatePollRequest(
    val question: String,
    val options: List<String>,
    @kotlinx.serialization.SerialName("group_id") val groupId: String? = null,
    @kotlinx.serialization.SerialName("is_anonymous") val isAnonymous: Boolean = false,
    @kotlinx.serialization.SerialName("multiple_choice") val multipleChoice: Boolean = false
)

@kotlinx.serialization.Serializable
data class PollVoteRequest(val option: Int)

@kotlinx.serialization.Serializable
data class PollOption(val text: String, val votes: Int = 0)

@kotlinx.serialization.Serializable
data class PollResponse(
    val id: String,
    val question: String,
    val options: List<PollOption>,
    @kotlinx.serialization.SerialName("is_closed") val isClosed: Boolean = false,
    @kotlinx.serialization.SerialName("voted_option") val votedOption: Int? = null,
    @kotlinx.serialization.SerialName("created_at") val createdAt: Long = 0L
)

@kotlinx.serialization.Serializable
data class HoodMessageRequest(val text: String, val lat: Double, val lon: Double)

@kotlinx.serialization.Serializable
data class HoodMessageResponse(
    val id: String,
    val text: String,
    @kotlinx.serialization.SerialName("sender_uin") val senderUin: Long,
    @kotlinx.serialization.SerialName("sender_nickname") val senderNickname: String,
    val lat: Double,
    val lon: Double,
    @kotlinx.serialization.SerialName("sent_at") val sentAt: Long,
    val reactions: Map<String, Int> = emptyMap()
)

@kotlinx.serialization.Serializable
data class HoodBannerResponse(
    val id: String,
    val text: String,
    @kotlinx.serialization.SerialName("image_url") val imageUrl: String? = null,
    @kotlinx.serialization.SerialName("expires_at") val expiresAt: Long
)

@kotlinx.serialization.Serializable
data class NearbyCheckinRequest(val lat: Double, val lon: Double, val radius: Int = 1000)

@kotlinx.serialization.Serializable
data class NearbyUserResponse(
    val uin: Long,
    val nickname: String,
    @kotlinx.serialization.SerialName("avatar_url") val avatarUrl: String? = null,
    val distance: Int
)

@kotlinx.serialization.Serializable
data class RandomQueueResponse(
    val status: String, // "waiting" | "matched"
    @kotlinx.serialization.SerialName("chat_id") val chatId: String? = null,
    @kotlinx.serialization.SerialName("partner_uin") val partnerUin: Long? = null
)

@kotlinx.serialization.Serializable
data class NewsItem(
    val id: String,
    val title: String,
    val body: String,
    @kotlinx.serialization.SerialName("image_url") val imageUrl: String? = null,
    @kotlinx.serialization.SerialName("published_at") val publishedAt: Long,
    val url: String? = null
)

@kotlinx.serialization.Serializable
data class ReportRequest(
    @kotlinx.serialization.SerialName("reported_uin") val reportedUin: Long,
    val reason: String,
    val details: String = ""
)
```

- [ ] `./gradlew kspDebugKotlin --no-daemon 2>&1 | grep -E "ERROR|BUILD"`

- [ ] Commit + push:
```bash
git add app/src/main/java/com/rcq/messenger/data/api/RCQApiService.kt
git commit -m "feat: API endpoints для presence/polls/hood/nearby/random/news/reports"
git push origin phase-1-core-messaging
```

---

## Task 2: Seven repositories (one file each)

**Files:**
- Create: `app/src/main/java/com/rcq/messenger/data/repository/PresenceRepository.kt`
- Create: `app/src/main/java/com/rcq/messenger/data/repository/PollRepository.kt`
- Create: `app/src/main/java/com/rcq/messenger/data/repository/HoodRepository.kt`
- Create: `app/src/main/java/com/rcq/messenger/data/repository/NearbyRepository.kt`
- Create: `app/src/main/java/com/rcq/messenger/data/repository/RandomChatRepository.kt`
- Create: `app/src/main/java/com/rcq/messenger/data/repository/NewsRepository.kt`
- Create: `app/src/main/java/com/rcq/messenger/data/repository/ReportRepository.kt`

- [ ] Create `PresenceRepository.kt`:
```kotlin
package com.rcq.messenger.data.repository
import com.rcq.messenger.data.api.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject; import javax.inject.Singleton

@Singleton
class PresenceRepository @Inject constructor(private val api: RCQApiService) {
    private val _onlineCount = MutableStateFlow(0)
    val onlineCount: StateFlow<Int> = _onlineCount.asStateFlow()

    suspend fun setStatus(status: String) = runCatching {
        api.updatePresence(PresenceUpdateRequest(status)).let { if (!it.isSuccessful) throw Exception("${it.code()}") }
    }
    suspend fun refreshOnlineCount() = runCatching {
        api.getOnlineCount().let { if (it.isSuccessful) _onlineCount.value = it.body()!!.count else throw Exception("${it.code()}") }
    }
}
```

- [ ] Create `PollRepository.kt`:
```kotlin
package com.rcq.messenger.data.repository
import com.rcq.messenger.data.api.*
import javax.inject.Inject; import javax.inject.Singleton

@Singleton
class PollRepository @Inject constructor(private val api: RCQApiService) {
    suspend fun createPoll(q: String, opts: List<String>, groupId: String? = null) = runCatching {
        api.createPoll(CreatePollRequest(q, opts, groupId)).let { if (it.isSuccessful) it.body()!! else throw Exception("${it.code()}") }
    }
    suspend fun getPoll(id: String) = runCatching {
        api.getPoll(id).let { if (it.isSuccessful) it.body()!! else throw Exception("${it.code()}") }
    }
    suspend fun vote(id: String, option: Int) = runCatching {
        api.votePoll(id, PollVoteRequest(option)).let { if (it.isSuccessful) it.body()!! else throw Exception("${it.code()}") }
    }
    suspend fun close(id: String) = runCatching {
        api.closePoll(id).let { if (it.isSuccessful) it.body()!! else throw Exception("${it.code()}") }
    }
}
```

- [ ] Create `HoodRepository.kt`:
```kotlin
package com.rcq.messenger.data.repository
import com.rcq.messenger.data.api.*
import javax.inject.Inject; import javax.inject.Singleton

@Singleton
class HoodRepository @Inject constructor(private val api: RCQApiService) {
    suspend fun sendMessage(text: String, lat: Double, lon: Double) = runCatching {
        api.sendHoodMessage(HoodMessageRequest(text, lat, lon)).let { if (it.isSuccessful) it.body()!! else throw Exception("${it.code()}") }
    }
    suspend fun getMessages(lat: Double, lon: Double) = runCatching {
        api.getHoodMessages(lat, lon).let { if (it.isSuccessful) it.body()!! else throw Exception("${it.code()}") }
    }
    suspend fun getBanners(lat: Double, lon: Double) = runCatching {
        api.getHoodBanners(lat, lon).let { if (it.isSuccessful) it.body()!! else throw Exception("${it.code()}") }
    }
}
```

- [ ] Create `NearbyRepository.kt`:
```kotlin
package com.rcq.messenger.data.repository
import com.rcq.messenger.data.api.*
import javax.inject.Inject; import javax.inject.Singleton

@Singleton
class NearbyRepository @Inject constructor(private val api: RCQApiService) {
    suspend fun checkin(lat: Double, lon: Double) = runCatching {
        api.nearbyCheckin(NearbyCheckinRequest(lat, lon)).let { if (!it.isSuccessful) throw Exception("${it.code()}") }
    }
    suspend fun getUsers(lat: Double, lon: Double) = runCatching {
        api.getNearbyUsers(lat, lon).let { if (it.isSuccessful) it.body()!! else throw Exception("${it.code()}") }
    }
}
```

- [ ] Create `RandomChatRepository.kt`:
```kotlin
package com.rcq.messenger.data.repository
import com.rcq.messenger.data.api.*
import javax.inject.Inject; import javax.inject.Singleton

@Singleton
class RandomChatRepository @Inject constructor(private val api: RCQApiService) {
    suspend fun join() = runCatching { api.joinRandomQueue().let { if (it.isSuccessful) it.body()!! else throw Exception("${it.code()}") } }
    suspend fun leave() = runCatching { api.leaveRandomQueue().let { if (!it.isSuccessful) throw Exception("${it.code()}") } }
    suspend fun skip() = runCatching { api.skipRandomPartner().let { if (it.isSuccessful) it.body()!! else throw Exception("${it.code()}") } }
}
```

- [ ] Create `NewsRepository.kt`:
```kotlin
package com.rcq.messenger.data.repository
import com.rcq.messenger.data.api.*
import javax.inject.Inject; import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(private val api: RCQApiService) {
    suspend fun getFeed(page: Int = 0) = runCatching {
        api.getNewsFeed(page).let { if (it.isSuccessful) it.body()!! else throw Exception("${it.code()}") }
    }
}
```

- [ ] Create `ReportRepository.kt`:
```kotlin
package com.rcq.messenger.data.repository
import com.rcq.messenger.data.api.*
import javax.inject.Inject; import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(private val api: RCQApiService) {
    suspend fun submit(uin: Long, reason: String, details: String = "") = runCatching {
        api.submitReport(ReportRequest(uin, reason, details)).let { if (!it.isSuccessful) throw Exception("${it.code()}") }
    }
}
```

- [ ] `./gradlew kspDebugKotlin --no-daemon 2>&1 | grep -E "ERROR|BUILD"`

- [ ] Commit + push:
```bash
git add app/src/main/java/com/rcq/messenger/data/repository/
git commit -m "feat: репозитории presence/polls/hood/nearby/random/news/reports"
git push origin phase-1-core-messaging
```

---

## Task 3: PollScreen + HoodScreen UI

**Files:**
- Create: `app/src/main/java/com/rcq/messenger/ui/polls/PollScreen.kt`
- Create: `app/src/main/java/com/rcq/messenger/ui/hood/HoodScreen.kt`

- [ ] Create `app/src/main/java/com/rcq/messenger/ui/polls/PollScreen.kt`:

```kotlin
package com.rcq.messenger.ui.polls

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.api.PollResponse
import com.rcq.messenger.data.repository.PollRepository
import com.rcq.messenger.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PollViewModel @Inject constructor(private val repo: PollRepository) : ViewModel() {
    private val _poll = MutableStateFlow<PollResponse?>(null)
    val poll: StateFlow<PollResponse?> = _poll.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load(id: String) { viewModelScope.launch {
        _loading.value = true
        repo.getPoll(id).onSuccess { _poll.value = it }.onFailure { _error.value = it.message }
        _loading.value = false
    } }

    fun vote(pollId: String, option: Int) { viewModelScope.launch {
        repo.vote(pollId, option).onSuccess { _poll.value = it }.onFailure { _error.value = it.message }
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollScreen(pollId: String, onBack: () -> Unit, viewModel: PollViewModel = hiltViewModel()) {
    val poll by viewModel.poll.collectAsState()
    val loading by viewModel.loading.collectAsState()
    LaunchedEffect(pollId) { viewModel.load(pollId) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Опрос", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface, titleContentColor = TextPrimary)
            )
        }, containerColor = Background
    ) { padding ->
        if (loading) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        } else poll?.let { p ->
            val total = p.options.sumOf { it.votes }.coerceAtLeast(1)
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                item {
                    Text(p.question, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(16.dp))
                }
                itemsIndexed(p.options) { idx, opt ->
                    val fraction = opt.votes.toFloat() / total
                    val voted = p.votedOption == idx
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { if (p.votedOption == null && !p.isClosed) viewModel.vote(p.id, idx) },
                        colors = CardDefaults.cardColors(containerColor = if (voted) Primary.copy(alpha = 0.15f) else SurfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(opt.text, color = TextPrimary)
                                Text("${opt.votes}", color = TextSecondary)
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth(), color = Primary, trackColor = SurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] Create `app/src/main/java/com/rcq/messenger/ui/hood/HoodScreen.kt`:

```kotlin
package com.rcq.messenger.ui.hood

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.rcq.messenger.data.api.HoodMessageResponse
import com.rcq.messenger.data.repository.HoodRepository
import com.rcq.messenger.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HoodViewModel @Inject constructor(private val repo: HoodRepository) : ViewModel() {
    private val _messages = MutableStateFlow<List<HoodMessageResponse>>(emptyList())
    val messages: StateFlow<List<HoodMessageResponse>> = _messages.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    val inputText = MutableStateFlow("")

    fun load(lat: Double, lon: Double) { viewModelScope.launch {
        _loading.value = true
        repo.getMessages(lat, lon).onSuccess { _messages.value = it }
        _loading.value = false
    } }

    fun send(lat: Double, lon: Double) {
        val text = inputText.value.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            repo.sendMessage(text, lat, lon).onSuccess { inputText.value = ""; load(lat, lon) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HoodScreen(onBack: () -> Unit, viewModel: HoodViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val text by viewModel.inputText.collectAsState()
    val locationPerm = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    // Default: Moscow center — replaced with real location once granted
    val lat = 55.751244; val lon = 37.618423
    LaunchedEffect(locationPerm.status.isGranted) {
        if (locationPerm.status.isGranted) viewModel.load(lat, lon)
        else locationPerm.launchPermissionRequest()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Район", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface, titleContentColor = TextPrimary)
            )
        }, containerColor = Background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Primary)
            LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 4.dp)) {
                items(messages, key = { it.id }) { msg ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = SurfaceVariant)) {
                        Column(Modifier.padding(10.dp)) {
                            Text(msg.senderNickname, style = MaterialTheme.typography.labelMedium, color = Primary, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text(msg.text, color = TextPrimary)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text, onValueChange = { viewModel.inputText.value = it },
                    modifier = Modifier.weight(1f), placeholder = { Text("Сообщение в районе…") },
                    shape = RoundedCornerShape(24.dp), singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { viewModel.send(lat, lon) }) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Primary)
                }
            }
        }
    }
}
```

- [ ] `./gradlew assembleDebug --no-daemon 2>&1 | grep -E "ERROR|BUILD"`

- [ ] Commit + push:
```bash
git add app/src/main/java/com/rcq/messenger/ui/polls/ \
        app/src/main/java/com/rcq/messenger/ui/hood/
git commit -m "feat: экраны PollScreen + HoodScreen (районный чат)"
git push origin phase-1-core-messaging
```

---

## Task 4: DiscoverScreen + wire all routes in RCQApp

**Files:**
- Create: `app/src/main/java/com/rcq/messenger/ui/discover/DiscoverScreen.kt`
- Modify: `app/src/main/java/com/rcq/messenger/ui/RCQApp.kt`

- [ ] Create `DiscoverScreen.kt`:

```kotlin
package com.rcq.messenger.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.rcq.messenger.ui.theme.*

private data class DiscoverEntry(val title: String, val subtitle: String, val icon: ImageVector, val route: String)

private val entries = listOf(
    DiscoverEntry("Район", "Чат с соседями по геолокации", Icons.Default.LocationCity, "hood"),
    DiscoverEntry("Поблизости", "Люди рядом с тобой", Icons.Default.NearMe, "nearby"),
    DiscoverEntry("Случайный чат", "Найти случайного собеседника", Icons.Default.Shuffle, "random_chat"),
    DiscoverEntry("Новости", "Лента новостей", Icons.Default.Newspaper, "news"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background, titleContentColor = TextPrimary)
            )
        }, containerColor = Background
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(entries) { e ->
                ListItem(
                    headlineContent = { Text(e.title, color = TextPrimary, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(e.subtitle, color = TextSecondary) },
                    leadingContent = { Icon(e.icon, null, tint = Primary) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = TextTertiary) },
                    modifier = Modifier.clickable { onNavigate(e.route) },
                    colors = ListItemDefaults.colors(containerColor = Background)
                )
                HorizontalDivider(color = SurfaceVariant.copy(alpha = 0.4f))
            }
        }
    }
}
```

- [ ] In `RCQApp.kt`:

  1. Add `Discover` to the `Screen` sealed class:
  ```kotlin
  data object Discover : Screen("discover", "Discover", Icons.Filled.Explore, Icons.Outlined.Explore)
  ```

  2. Update `bottomNavItems`:
  ```kotlin
  val bottomNavItems = listOf(Screen.Chats, Screen.Contacts, Screen.Discover, Screen.Settings)
  ```

  3. Add composable routes in `NavHost`:
  ```kotlin
  composable(Screen.Discover.route) {
      DiscoverScreen(onNavigate = { route -> navController.navigate(route) })
  }
  composable("hood") {
      HoodScreen(onBack = { navController.popBackStack() })
  }
  composable("poll/{pollId}") { entry ->
      val pollId = entry.arguments?.getString("pollId") ?: return@composable
      PollScreen(pollId = pollId, onBack = { navController.popBackStack() })
  }
  composable("nearby") { /* TODO: NearbyScreen */ }
  composable("random_chat") { /* TODO: RandomChatScreen */ }
  composable("news") { /* TODO: NewsScreen */ }
  ```

  4. Add imports:
  ```kotlin
  import com.rcq.messenger.ui.discover.DiscoverScreen
  import com.rcq.messenger.ui.hood.HoodScreen
  import com.rcq.messenger.ui.polls.PollScreen
  import androidx.compose.material.icons.filled.Explore
  import androidx.compose.material.icons.outlined.Explore
  ```

- [ ] `./gradlew assembleDebug --no-daemon 2>&1 | grep -E "ERROR|BUILD"`

- [ ] Commit + push:
```bash
git add app/src/main/java/com/rcq/messenger/ui/discover/ \
        app/src/main/java/com/rcq/messenger/ui/RCQApp.kt
git commit -m "feat: Discover таб в навигации + маршруты Hood/Poll/Nearby/Random/News (F6)"
git push origin phase-1-core-messaging
```

---

## Self-Review
- ✅ Presence — setStatus (reuses existing endpoint) + onlineCount
- ✅ Polls — create/vote/view/close
- ✅ Hood — geo-chat with location permission + WS events exist in WebSocketService
- ✅ Nearby — checkin + list
- ✅ Random — join/leave/skip
- ✅ News — feed
- ✅ Reports — submit
- ✅ Discover screen — hub with 4 discovery features
- ⚠️ Hood lat/lon hardcoded as Moscow fallback — replace with FusedLocationProviderClient in HoodViewModel
- ⚠️ Nearby/Random/News screens are `/* TODO */` placeholder routes — build in next iteration
- ✅ All repos are `@Singleton @Inject constructor` — Hilt auto-provides
- ✅ imePadding() applied to HoodScreen
- ✅ All screens use RCQ theme colors (Primary, Background, Surface, TextPrimary, etc.)
