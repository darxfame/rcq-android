package app.rcq.android.security

import android.content.Context
import app.rcq.android.crypto.PinVault
import app.rcq.android.data.SecureStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Panic-PIN lock state + dataKey custody (Phase 1: real PIN only). Android port
 * of the real-mode slice of iOS `PanicPINService`. Holds the in-memory unlocked
 * `dataKey` (the SQLCipher passphrase source for every account's message DB)
 * and the app lock state. The DB rekeying on set/change/remove is orchestrated
 * by [app.rcq.android.Session]; this object owns the vault + state only.
 *
 * Decoy / wipe / biometric are deferred to later phases — but [submit] already
 * routes a wipe-slot match to [SubmitResult.WIPE], so adding those modes later
 * won't reshuffle the vault.
 */
object PanicPinService {
    enum class SubmitResult { REAL, WIPE, WRONG, LOCKED_OUT }

    @Volatile
    var dataKey: ByteArray? = null
        private set

    /** True when a PIN is configured and the app has not been unlocked this
     *  foreground session. The UI gates on this to show the lock screen. */
    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    val isLocked: Boolean get() = _locked.value

    private var realPayload: PinVault.SlotPayload? = null
    // The real slot's derived key, kept while unlocked so we can re-seal the
    // real slot (e.g. to record a new decoy/wipe slot in its layout) without
    // re-deriving from the PIN.
    @Volatile
    private var realSlotKey: ByteArray? = null

    fun isConfigured(context: Context): Boolean = PinVault.isConfigured(context)

    /** Call once at process start: start locked iff a PIN is configured. */
    fun initLockState(context: Context) {
        _locked.value = PinVault.isConfigured(context)
        dataKey = null
        realPayload = null
        realSlotKey = null
    }

    /** Epoch-ms the lockout lasts until, or null if not locked out. */
    fun lockedOutUntil(context: Context): Long? =
        PinVault.loadAttempts(context).lockoutUntil?.takeIf { it > System.currentTimeMillis() }

    /** Try [pin] at the lock screen. On [SubmitResult.REAL] the dataKey is set
     *  and [locked] clears; the caller then (re)binds the message DB. */
    fun submit(context: Context, pin: String): SubmitResult {
        if (lockedOutUntil(context) != null) return SubmitResult.LOCKED_OUT
        val unlock = PinVault.unlock(context, pin)
        if (unlock == null) {
            val n = PinVault.loadAttempts(context).failedCount + 1
            val lo = PinVault.lockoutMillis(n)
            PinVault.saveAttempts(
                context,
                PinVault.AttemptState(n, if (lo > 0) System.currentTimeMillis() + lo else null),
            )
            return SubmitResult.WRONG
        }
        PinVault.clearAttempts(context)
        return when (unlock.payload.mode) {
            PinVault.MODE_REAL -> {
                realPayload = unlock.payload
                realSlotKey = unlock.slotKey
                dataKey = PinVault.dataKeyBytes(unlock.payload)
                _locked.value = false
                SubmitResult.REAL
            }
            PinVault.MODE_WIPE -> SubmitResult.WIPE
            else -> SubmitResult.WRONG // decoy: Phase 2c
        }
    }

    /** Create the real PIN (no PIN currently). Returns the new vault dataKey,
     *  or null if [pin] is too short. The caller rekeys the message DBs from
     *  the device key to this one. */
    fun setRealPin(context: Context, pin: String): ByteArray? {
        if (pin.length < PinVault.MIN_PIN_LENGTH) return null
        val unlock = PinVault.createWithRealPin(context, pin)
        realPayload = unlock.payload
        realSlotKey = unlock.slotKey
        dataKey = PinVault.dataKeyBytes(unlock.payload)
        _locked.value = false
        return dataKey
    }

    /** Is a wipe PIN configured? (Only meaningful while unlocked-real.) */
    fun hasWipePin(): Boolean = realPayload?.layout?.wipeSlot != null

    /** Add a wipe PIN: entering it at the lock screen erases everything (the
     *  caller acts on [SubmitResult.WIPE]). Seals a WIPE slot under [pin] and
     *  records it in the real slot's layout. Requires an unlocked real session;
     *  [pin] must differ from the real/decoy PINs and a slot must be free. */
    fun setWipePin(context: Context, pin: String): Boolean {
        if (pin.length < PinVault.MIN_PIN_LENGTH) return false
        val real = realPayload ?: return false
        val key = realSlotKey ?: return false
        val layout = real.layout ?: return false
        val slot = PinVault.addSlot(context, pin, PinVault.SlotPayload(mode = PinVault.MODE_WIPE), layout) ?: return false
        layout.wipeSlot = slot
        PinVault.writeSlot(context, layout.realSlot, real, key) // re-seal real with the updated layout
        return true
    }

    /** Remove the wipe PIN (clears its slot back to random, drops it from the
     *  real layout). */
    fun removeWipePin(context: Context): Boolean {
        val real = realPayload ?: return false
        val key = realSlotKey ?: return false
        val layout = real.layout ?: return false
        val slot = layout.wipeSlot ?: return true
        PinVault.writeSlot(context, slot, null, null)
        layout.wipeSlot = null
        PinVault.writeSlot(context, layout.realSlot, real, key)
        return true
    }

    /** Re-seal the real slot under [newPin]; the dataKey (and the DB) are
     *  unchanged. Requires an unlocked real session. */
    fun changeRealPin(context: Context, newPin: String): Boolean {
        if (newPin.length < PinVault.MIN_PIN_LENGTH) return false
        val payload = realPayload ?: return false
        val realSlot = payload.layout?.realSlot ?: return false
        return runCatching { PinVault.reSealSlot(context, realSlot, payload, newPin) }.isSuccess
    }

    /** Remove the PIN entirely. Returns the device key the message DBs revert
     *  to (the caller rekeys them back). */
    fun removePin(context: Context): ByteArray {
        PinVault.destroy(context)
        dataKey = null
        realPayload = null
        realSlotKey = null
        _locked.value = false
        return SecureStore.deviceKey(context)
    }

    /** Re-lock when the app goes to background (only if a PIN is configured). */
    fun lock(context: Context) {
        if (!PinVault.isConfigured(context)) return
        dataKey = null
        realPayload = null
        realSlotKey = null
        _locked.value = true
    }
}
