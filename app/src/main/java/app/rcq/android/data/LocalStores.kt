package app.rcq.android.data

import android.content.Context
import android.content.SharedPreferences
import app.rcq.android.ui.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Client-side, non-secret preference state — the Android analogue of the
 * iOS `FavoritesStore` / `ArchiveStore` / `SoundService` mute set /
 * `RemovedContactsStore`, plus the appearance setting. Everything is a
 * set of thread keys ("peer:<uin>" or "group:<id>") so the same store
 * serves 1:1 and group rows. State is exposed as StateFlows the Compose
 * UI collects, and mirrored into a plain (unencrypted) SharedPreferences
 * — none of this is sensitive.
 *
 * Call [init] once from `MainActivity.onCreate` before composing.
 */
object LocalStores {
    private lateinit var prefs: SharedPreferences

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _muted = MutableStateFlow<Set<String>>(emptySet())
    val muted: StateFlow<Set<String>> = _muted.asStateFlow()

    private val _archived = MutableStateFlow<Set<String>>(emptySet())
    val archived: StateFlow<Set<String>> = _archived.asStateFlow()

    /** UINs of contacts the user removed — incoming sealed messages from
     *  them are dropped client-side (sealed sender means the server can't
     *  filter by sender). Mirrors iOS RemovedContactsStore. */
    private val _removed = MutableStateFlow<Set<Int>>(emptySet())
    val removed: StateFlow<Set<Int>> = _removed.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    /** Notification-sound toggles (iOS SoundService parity). Message sound
     *  plays on an inbound message to a non-active, non-muted thread;
     *  presence sound plays when a contact comes online / goes offline. */
    private val _soundMessages = MutableStateFlow(true)
    val soundMessages: StateFlow<Boolean> = _soundMessages.asStateFlow()

    private val _soundPresence = MutableStateFlow(true)
    val soundPresence: StateFlow<Boolean> = _soundPresence.asStateFlow()

    /** Persistent per-thread unread counters, keyed "peer:<uin>"/"group:<id>".
     *  Mirrors the iOS UnreadStore: survives cold starts (contacts reload
     *  from the server, which doesn't track per-client unread), bumped on
     *  inbound message, cleared when the chat opens. */
    private val _unread = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unread: StateFlow<Map<String, Int>> = _unread.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("rcq_local", Context.MODE_PRIVATE)
        _favorites.value = prefs.getStringSet(K_FAV, emptySet())!!.toSet()
        _muted.value = prefs.getStringSet(K_MUTE, emptySet())!!.toSet()
        _archived.value = prefs.getStringSet(K_ARCH, emptySet())!!.toSet()
        _removed.value = prefs.getStringSet(K_REMOVED, emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet()
        _themeMode.value = runCatching { ThemeMode.valueOf(prefs.getString(K_THEME, null) ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM)
        _soundMessages.value = prefs.getBoolean(K_SND_MSG, true)
        _soundPresence.value = prefs.getBoolean(K_SND_PRES, true)
        _unread.value = loadUnread()
    }

    // ── thread-key helpers ───────────────────────────────────────────
    fun peerThread(uin: Int) = "peer:$uin"
    fun groupThread(id: Int) = "group:$id"

    fun isFavorite(thread: String) = thread in _favorites.value
    fun toggleFavorite(thread: String) = toggle(_favorites, K_FAV, thread)

    fun isMuted(thread: String) = thread in _muted.value
    fun toggleMute(thread: String) = toggle(_muted, K_MUTE, thread)

    fun isArchived(thread: String) = thread in _archived.value
    fun toggleArchive(thread: String) = toggle(_archived, K_ARCH, thread)

    fun isRemoved(uin: Int) = uin in _removed.value
    fun addRemoved(uin: Int) {
        if (uin in _removed.value) return
        _removed.value = _removed.value + uin
        prefs.edit().putStringSet(K_REMOVED, _removed.value.map(Int::toString).toSet()).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(K_THEME, mode.name).apply()
    }

    // ── sound toggles ────────────────────────────────────────────────
    fun soundMessagesOn() = _soundMessages.value
    fun soundPresenceOn() = _soundPresence.value
    fun setSoundMessages(on: Boolean) { _soundMessages.value = on; prefs.edit().putBoolean(K_SND_MSG, on).apply() }
    fun setSoundPresence(on: Boolean) { _soundPresence.value = on; prefs.edit().putBoolean(K_SND_PRES, on).apply() }

    // ── unread counters ──────────────────────────────────────────────
    fun unreadOf(thread: String): Int = _unread.value[thread] ?: 0

    fun bumpUnread(thread: String) {
        val cur = _unread.value.toMutableMap()
        cur[thread] = (cur[thread] ?: 0) + 1
        _unread.value = cur
        persistUnread()
    }

    fun clearUnread(thread: String) {
        if (_unread.value[thread] == null) return
        _unread.value = _unread.value - thread
        persistUnread()
    }

    /** Encode the map as a CSV "thread=count" StringSet for SharedPreferences. */
    private fun persistUnread() {
        prefs.edit().putStringSet(K_UNREAD, _unread.value.map { "${it.key}=${it.value}" }.toSet()).apply()
    }

    private fun loadUnread(): Map<String, Int> =
        prefs.getStringSet(K_UNREAD, emptySet())!!.mapNotNull { entry ->
            val i = entry.lastIndexOf('=')
            if (i <= 0) return@mapNotNull null
            val k = entry.substring(0, i)
            val v = entry.substring(i + 1).toIntOrNull() ?: return@mapNotNull null
            k to v
        }.toMap()

    private fun toggle(flow: MutableStateFlow<Set<String>>, key: String, thread: String) {
        flow.value = if (thread in flow.value) flow.value - thread else flow.value + thread
        // StringSet must be copied — SharedPreferences keeps the same
        // instance otherwise and silently no-ops on the next read.
        prefs.edit().putStringSet(key, flow.value.toSet()).apply()
    }

    private const val K_FAV = "favorites"
    private const val K_MUTE = "muted"
    private const val K_ARCH = "archived"
    private const val K_REMOVED = "removed"
    private const val K_THEME = "theme_mode"
    private const val K_UNREAD = "unread"
    private const val K_SND_MSG = "sound_messages"
    private const val K_SND_PRES = "sound_presence"
}
