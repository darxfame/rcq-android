package app.rcq.android.nearby

import app.rcq.android.net.RcqApi
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Hood Chat (district chat) + Hood Banners (district announcements) — both
 * scoped to a geohash bucket, the server-backed half of People Nearby. Port of
 * the iOS HoodChatService + the banner endpoints. NOT end-to-end encrypted
 * (pseudonymous via the Nearby display name); the chat UI shows an "unencrypted"
 * notice. Chat state flows through the WS (hood_* events routed from
 * Session.handleEvent); banners are plain REST.
 */
class HoodController(
    private val scope: CoroutineScope,
    private val api: () -> RcqApi,
    private val send: (JsonObject) -> Unit,
    private val nick: () -> String,
    private val isAnonymous: () -> Boolean,
) {
    private val gson = Gson()

    // ── chat ──────────────────────────────────────────────────────────
    private val _bucket = MutableStateFlow<String?>(null)
    val bucket: StateFlow<String?> = _bucket.asStateFlow()
    private val _messages = MutableStateFlow<List<RcqApi.HoodMessage>>(emptyList())
    val messages: StateFlow<List<RcqApi.HoodMessage>> = _messages.asStateFlow()
    private val _bucketCount = MutableStateFlow(0)
    val bucketCount: StateFlow<Int> = _bucketCount.asStateFlow()

    fun joinChat(bucket: String) {
        _bucket.value = bucket
        _messages.value = emptyList()
        // Subscribe BEFORE the catch-up fetch so the count includes us and any
        // in-flight hood_message is fanned out to us.
        send(JsonObject().apply { addProperty("type", "hood_subscribe"); addProperty("bucket", bucket) })
        scope.launch { runCatching { api().hoodMessages(bucket) }.onSuccess { upsertAll(it) } }
    }

    fun leaveChat() {
        if (_bucket.value != null) send(JsonObject().apply { addProperty("type", "hood_unsubscribe") })
        _bucket.value = null
        _messages.value = emptyList()
        _bucketCount.value = 0
    }

    fun sendMessage(text: String) {
        val t = text.trim()
        if (t.isEmpty() || _bucket.value == null) return
        scope.launch {
            runCatching {
                api().hoodSend(RcqApi.HoodSendBody(body = t, nickname = nick(), anonymous = isAnonymous()))
            }
            // No optimistic append: the hood_message broadcast adds it.
        }
    }

    fun deleteMessage(id: Int) {
        scope.launch { runCatching { api().hoodDelete(id) } }
    }

    fun react(id: Int, emoji: String) {
        scope.launch { runCatching { api().hoodReact(id, emoji) } }
    }

    /** Routed from Session.handleEvent for hood_* events. */
    fun onSignal(type: String, obj: JsonObject) {
        val active = _bucket.value
        when (type) {
            "hood_message" -> {
                val m = runCatching { gson.fromJson(obj.getAsJsonObject("message"), RcqApi.HoodMessage::class.java) }.getOrNull() ?: return
                if (active == null || m.bucket_id != active) return
                upsertOne(m)
                obj.get("bucket_count")?.takeIf { !it.isJsonNull }?.asInt?.let { _bucketCount.value = it }
            }
            "hood_count" -> {
                if (obj.get("bucket_id")?.asString != active) return
                _bucketCount.value = obj.get("count")?.asInt ?: _bucketCount.value
            }
            "hood_delete" -> {
                if (obj.get("bucket_id")?.asString != active) return
                val id = obj.get("message_id")?.asInt ?: return
                _messages.value = _messages.value.map { if (it.id == id) it.copy(deleted = true, body = "") else it }
            }
            "hood_reaction" -> {
                if (obj.get("bucket_id")?.asString != active) return
                val id = obj.get("message_id")?.asInt ?: return
                val reactions = runCatching {
                    gson.fromJson(obj.getAsJsonObject("reactions"), Map::class.java) as Map<String, String>
                }.getOrDefault(emptyMap())
                _messages.value = _messages.value.map { if (it.id == id) it.copy(reactions = reactions) else it }
            }
        }
    }

    private fun upsertAll(list: RcqApi.HoodList) {
        _messages.value = list.messages.sortedBy { it.id }
        _bucketCount.value = list.bucket_count
    }

    private fun upsertOne(m: RcqApi.HoodMessage) {
        val cur = _messages.value
        _messages.value = if (cur.any { it.id == m.id }) cur.map { if (it.id == m.id) m else it }
        else (cur + m).sortedBy { it.id }
    }

    // ── banners ───────────────────────────────────────────────────────
    private val _banners = MutableStateFlow<List<RcqApi.Banner>>(emptyList())
    val banners: StateFlow<List<RcqApi.Banner>> = _banners.asStateFlow()
    private val _canPost = MutableStateFlow(true)
    val canPostBanner: StateFlow<Boolean> = _canPost.asStateFlow()
    private val _pricing = MutableStateFlow<List<RcqApi.BannerPricing>>(emptyList())
    val pricing: StateFlow<List<RcqApi.BannerPricing>> = _pricing.asStateFlow()

    fun loadBanners(bucket: String) {
        scope.launch {
            runCatching { api().banners(bucket) }.onSuccess { _banners.value = it.items; _canPost.value = it.can_post }
            if (_pricing.value.isEmpty()) runCatching { api().bannerPricing() }.onSuccess { _pricing.value = it }
        }
    }

    suspend fun postBanner(bucket: String, text: String, anonymous: Boolean, duration: String): Boolean = runCatching {
        // Mock IAP receipt — any non-empty string passes server-side today.
        api().createBanner(
            RcqApi.CreateBannerBody(
                bucket_id = bucket, text = text.trim(), is_anonymous = anonymous,
                duration = duration, receipt = "android-mock-$duration",
            ),
        )
        loadBanners(bucket)
        true
    }.getOrDefault(false)

    fun deleteBanner(id: Int, bucket: String) {
        scope.launch { runCatching { api().deleteBanner(id) }; loadBanners(bucket) }
    }

    fun teardown() {
        if (_bucket.value != null) runCatching { send(JsonObject().apply { addProperty("type", "hood_unsubscribe") }) }
        _bucket.value = null
        _messages.value = emptyList()
        _bucketCount.value = 0
        _banners.value = emptyList()
    }
}
