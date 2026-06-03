package app.rcq.android.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local-only "people viewed your profile" tally — the Android analogue of
 * the iOS VisitStore. When someone opens our profile their client sends a
 * sealed `visit` envelope; [Session] decrypts it and calls [record]. The
 * server never sees the count or that it exists.
 *
 * Multi-account: the tally is per-identity, so storage is keyed by the
 * active [Account.id]. [bindAccount] swaps which account's visits the flow
 * reflects (and is re-called on every account switch by [Session]).
 *
 * Same-viewer pings within [DEDUP_MS] collapse into one (unique view);
 * visits older than [PRUNE_MS] are dropped on every save. [recentViews] is
 * the rolling count within [WINDOW_MS] (7 days).
 *
 * Call [init] once from MainActivity.onCreate, then [bindAccount].
 */
object VisitStore {
    private lateinit var prefs: SharedPreferences

    /** Active account prefix; null before any account is bound. */
    private var acct: String? = null

    /** (viewerUin, atEpochMillis) pairs. */
    private val _visits = MutableStateFlow<List<Pair<Int, Long>>>(emptyList())

    private val _recentViews = MutableStateFlow(0)
    val recentViews: StateFlow<Int> = _recentViews.asStateFlow()

    const val DEDUP_MS = 60L * 60 * 1000           // 1h unique-visitor window
    const val PRUNE_MS = 30L * 86_400 * 1000        // keep 30 days
    const val WINDOW_MS = 7L * 86_400 * 1000        // "last 7 days" count

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("rcq_visits", Context.MODE_PRIVATE)
    }

    /** Point the tally at [accountId]'s slot and reload it. null resets. */
    fun bindAccount(accountId: String?) {
        acct = accountId
        _visits.value = if (accountId == null) emptyList() else load(key())
        recompute()
    }

    /** Record a profile view from [viewer] at [atMillis] (epoch). Future
     *  timestamps are clamped to now; repeats within [DEDUP_MS] are ignored. */
    fun record(viewer: Int, atMillis: Long) {
        if (!::prefs.isInitialized || acct == null) return
        val now = System.currentTimeMillis()
        val clamped = minOf(atMillis, now)
        val last = _visits.value.lastOrNull { it.first == viewer }?.second
        if (last != null && clamped - last < DEDUP_MS) return
        _visits.value = (_visits.value + (viewer to clamped)).filter { now - it.second <= PRUNE_MS }
        persist()
        recompute()
    }

    /** Wipe the bound account's tally (burn). */
    fun wipe() {
        _visits.value = emptyList()
        _recentViews.value = 0
        if (::prefs.isInitialized && acct != null) prefs.edit().remove(key()).apply()
    }

    /** Wipe a specific (possibly non-active) account's tally (local delete). */
    fun wipeAccount(accountId: String) {
        if (::prefs.isInitialized) prefs.edit().remove("$accountId.$K_VISITS").apply()
        if (accountId == acct) { _visits.value = emptyList(); _recentViews.value = 0 }
    }

    /** Lift the legacy unprefixed tally under [accountId]. Idempotent. */
    fun migrateLegacyToAccount(accountId: String) {
        if (!::prefs.isInitialized || !prefs.contains(K_VISITS)) return
        prefs.getStringSet(K_VISITS, emptySet())?.let {
            prefs.edit().putStringSet("$accountId.$K_VISITS", it.toSet()).remove(K_VISITS).apply()
        }
    }

    private fun key() = "$acct.$K_VISITS"

    private fun recompute() {
        val cutoff = System.currentTimeMillis() - WINDOW_MS
        _recentViews.value = _visits.value.count { it.second >= cutoff }
    }

    private fun persist() {
        prefs.edit().putStringSet(key(), _visits.value.map { "${it.first}:${it.second}" }.toSet()).apply()
    }

    private fun load(storageKey: String): List<Pair<Int, Long>> =
        prefs.getStringSet(storageKey, emptySet())!!.mapNotNull { e ->
            val i = e.lastIndexOf(':')
            if (i <= 0) return@mapNotNull null
            val u = e.substring(0, i).toIntOrNull() ?: return@mapNotNull null
            val t = e.substring(i + 1).toLongOrNull() ?: return@mapNotNull null
            u to t
        }.sortedBy { it.second }

    private const val K_VISITS = "visits"
}
