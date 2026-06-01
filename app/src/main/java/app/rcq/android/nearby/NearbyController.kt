package app.rcq.android.nearby

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import app.rcq.android.net.RcqApi
import app.rcq.android.util.Geohash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * People Nearby — Android port of the iOS `NearbyService`. Privacy-preserving
 * geo discovery: take a level-6 geohash (~1.2km tile) from the device location
 * and ship ONLY the hash to `POST /nearby/checkin`; never raw coordinates. Poll
 * `GET /nearby/list` (self + 8 neighbour tiles) every 30s for others checked in
 * nearby. Anonymous by default (a minted "Adj Noun #NNNN" handle); the real
 * nickname is only surfaced if the user turns anonymous off.
 */
class NearbyController(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val api: () -> RcqApi,
) {
    sealed interface State {
        data object Idle : State
        data object Pending : State
        data class Active(val bucketId: String, val expiresAtMs: Long) : State
        data object Denied : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()
    private val _people = MutableStateFlow<List<RcqApi.NearbyUser>>(emptyList())
    val people: StateFlow<List<RcqApi.NearbyUser>> = _people.asStateFlow()
    private val _displayName = MutableStateFlow(loadOrMintName())
    val displayName: StateFlow<String> = _displayName.asStateFlow()
    private val _anonymous = MutableStateFlow(loadAnonymous())
    val anonymous: StateFlow<Boolean> = _anonymous.asStateFlow()

    private var refreshJob: Job? = null
    @Volatile private var ttlSeconds = 60 * 60

    val isActive: Boolean get() = _state.value is State.Active || _state.value is State.Pending

    /** Begin sharing. Caller must have already obtained location permission
     *  (the UI requests it); pass [permissionGranted]=false to surface Denied. */
    fun start(permissionGranted: Boolean, ttl: Int = 60 * 60) {
        if (_state.value is State.Active || _state.value is State.Pending) return
        if (!permissionGranted) { _state.value = State.Denied; return }
        ttlSeconds = ttl
        _state.value = State.Pending
        scope.launch {
            val loc = awaitLocation()
            if (loc == null) { _state.value = State.Error("no_location"); return@launch }
            val bucket = Geohash.encode(loc.latitude, loc.longitude, 6)
            val res = runCatching { api().nearbyCheckin(bucket, ttlSeconds, if (_anonymous.value) _displayName.value else null) }.getOrNull()
            if (res == null) { _state.value = State.Error("checkin_failed"); return@launch }
            _state.value = State.Active(bucket, expiryMs(res.expires_at))
            refreshList()
            startRefresh()
        }
    }

    fun stop() {
        refreshJob?.cancel(); refreshJob = null
        scope.launch { runCatching { api().nearbyEndCheckin() } }
        _people.value = emptyList()
        _state.value = State.Idle
    }

    fun regenerateName() {
        val n = mintName()
        _displayName.value = n
        prefs().edit().putString(K_NAME, n).apply()
        if (_state.value is State.Active && _anonymous.value) recheckin()
    }

    fun setAnonymous(value: Boolean) {
        _anonymous.value = value
        prefs().edit().putBoolean(K_ANON, value).apply()
        if (_state.value is State.Active) recheckin()
    }

    /** Burn/rebind/lock/wipe hook: drop the local session (server TTL is the
     *  backstop; we also best-effort DELETE). */
    fun teardown() {
        refreshJob?.cancel(); refreshJob = null
        if (_state.value is State.Active) scope.launch { runCatching { api().nearbyEndCheckin() } }
        _people.value = emptyList()
        _state.value = State.Idle
    }

    // ── internals ─────────────────────────────────────────────────────
    private fun recheckin() {
        val cur = _state.value as? State.Active ?: return
        scope.launch {
            runCatching {
                api().nearbyCheckin(cur.bucketId, ttlSeconds, if (_anonymous.value) _displayName.value else null)
            }
        }
    }

    private fun startRefresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            while (isActive) {
                delay(30_000)
                if (_state.value is State.Active) refreshList()
            }
        }
    }

    private suspend fun refreshList() {
        val cur = _state.value as? State.Active ?: return
        val buckets = Geohash.selfAndNeighbours(cur.bucketId)
        runCatching { api().nearbyList(buckets) }.onSuccess { _people.value = it }
    }

    @SuppressLint("MissingPermission")
    private suspend fun awaitLocation(): Location? {
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        // Cached fix first (instant; fine for a ~1.2km tile).
        for (p in providers) runCatching { lm.getLastKnownLocation(p) }.getOrNull()?.let { return it }
        // Fresh single fix (API 30+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && providers.isNotEmpty()) {
            return suspendCancellableCoroutine { cont ->
                val cancel = CancellationSignal()
                cont.invokeOnCancellation { cancel.cancel() }
                runCatching {
                    lm.getCurrentLocation(providers.first(), cancel, appContext.mainExecutor) { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                }.onFailure { if (cont.isActive) cont.resume(null) }
            }
        }
        return null
    }

    private fun expiryMs(iso: String?): Long {
        if (iso.isNullOrBlank()) return System.currentTimeMillis() + ttlSeconds * 1000L
        return runCatching { java.time.Instant.parse(iso).toEpochMilli() }
            .recoverCatching { java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli() }
            .recoverCatching { java.time.LocalDateTime.parse(iso).toInstant(java.time.ZoneOffset.UTC).toEpochMilli() }
            .getOrDefault(System.currentTimeMillis() + ttlSeconds * 1000L)
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun loadOrMintName(): String {
        prefs().getString(K_NAME, null)?.takeIf { it.isNotEmpty() }?.let { return it }
        val n = mintName(); prefs().edit().putString(K_NAME, n).apply(); return n
    }

    private fun loadAnonymous(): Boolean = prefs().getBoolean(K_ANON, true) // default ON

    private fun mintName(): String {
        val adj = ADJECTIVES.random(); val noun = NOUNS.random(); val num = (1000..9999).random()
        return "$adj $noun #$num"
    }

    companion object {
        private const val PREFS = "rcq_nearby"
        private const val K_NAME = "display_name"
        private const val K_ANON = "anonymous"
        private val ADJECTIVES = listOf(
            "Wandering", "Curious", "Silent", "Quirky", "Hopeful", "Restless", "Drifting",
            "Gentle", "Vibrant", "Witty", "Roaming", "Quiet", "Lucky", "Cosy", "Misty",
            "Twilight", "Dreamy", "Easy", "Mellow", "Fleeting",
        )
        private val NOUNS = listOf(
            "Stranger", "Wanderer", "Traveler", "Passerby", "Drifter", "Visitor", "Voyager",
            "Nomad", "Soul", "Walker", "Guest", "Watcher", "Reader", "Sketcher", "Daydreamer",
            "Listener", "Observer", "Pilgrim", "Rambler", "Spirit",
        )
    }
}
