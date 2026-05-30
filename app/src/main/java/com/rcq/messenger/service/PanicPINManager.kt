package com.rcq.messenger.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

// Mirrors iOS PanicPINService — manages real/decoy/wipe session modes.
// Real: normal session with real UIN/messages.
// Decoy: fake UIN, cleared contacts/messages.
// Wipe: triggers secure data deletion then returns to unlocked state.
@Singleton
class PanicPINManager @Inject constructor(
    private val vault: PINVaultManager,
    private val biometricManager: BiometricManager
) {
    enum class SessionMode { none, real, decoy, wipe }
    enum class LockState { locked, unlocked }

    sealed class SubmitResult {
        object UnlockedReal : SubmitResult()
        data class UnlockedDecoy(val uin: Long, val nickname: String) : SubmitResult()
        object Wipe : SubmitResult()
        object Wrong : SubmitResult()
        data class LockedOut(val untilMs: Long) : SubmitResult()
    }

    private val _lockState = MutableStateFlow(
        if (vault.isConfigured) LockState.locked else LockState.unlocked
    )
    val lockState: StateFlow<LockState> = _lockState

    private val _mode = MutableStateFlow(SessionMode.none)
    val mode: StateFlow<SessionMode> = _mode

    private val _biometricEnabled = MutableStateFlow(biometricManager.isEnabled)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled

    private val _lockoutUntilMs = MutableStateFlow(0L)
    val lockoutUntilMs: StateFlow<Long> = _lockoutUntilMs

    private var realPayload: PINVaultManager.SlotPayload? = null
    private var realSlotKey: SecretKey? = null
    var dataKey: ByteArray? = null; private set

    val isConfigured: Boolean get() = vault.isConfigured
    val isDecoy: Boolean get() = _mode.value == SessionMode.decoy
    val isLocked: Boolean get() = _lockState.value == LockState.locked
    val hasDecoyPIN: Boolean get() = realPayload?.layout?.decoySlot != null
    val hasWipePIN: Boolean get() = realPayload?.layout?.wipeSlot != null

    init {
        val state = vault.loadAttemptState()
        if (state.lockoutUntilMs > System.currentTimeMillis()) {
            _lockoutUntilMs.value = state.lockoutUntilMs
        }
    }

    suspend fun submit(pin: String): SubmitResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lockedUntil = _lockoutUntilMs.value
        if (lockedUntil > now) return@withContext SubmitResult.LockedOut(lockedUntil)

        val unlock = vault.unlock(pin)
        if (unlock == null) {
            val state = vault.loadAttemptState()
            val newCount = state.failedCount + 1
            val delayMs = vault.lockoutDurationMs(newCount)
            val until = if (delayMs > 0) now + delayMs else 0L
            vault.saveAttemptState(state.copy(failedCount = newCount, lockoutUntilMs = until))
            _lockoutUntilMs.value = until
            return@withContext SubmitResult.Wrong
        }

        vault.clearAttemptState()
        _lockoutUntilMs.value = 0L
        applyUnlock(unlock)
    }

    suspend fun unlockWithBiometrics(cipher: javax.crypto.Cipher): Boolean =
        withContext(Dispatchers.IO) {
            val payload = biometricManager.decryptPayload(cipher) ?: return@withContext false
            realPayload = payload
            realSlotKey = null
            dataKey = payload.dataKey?.let { Base64.getDecoder().decode(it) }
            _mode.value = SessionMode.real
            _lockState.value = LockState.unlocked
            vault.clearAttemptState()
            _lockoutUntilMs.value = 0L
            Timber.d("PanicPIN: unlocked via biometrics")
            true
        }

    private fun applyUnlock(unlock: PINVaultManager.Unlock): SubmitResult {
        return when (unlock.payload.mode) {
            PINVaultManager.SlotMode.real -> {
                realPayload = unlock.payload
                realSlotKey = unlock.slotKey
                dataKey = unlock.payload.dataKey?.let { Base64.getDecoder().decode(it) }
                _mode.value = SessionMode.real
                _lockState.value = LockState.unlocked
                Timber.d("PanicPIN: unlocked real session")
                SubmitResult.UnlockedReal
            }
            PINVaultManager.SlotMode.decoy -> {
                realPayload = null; realSlotKey = null
                dataKey = unlock.payload.dataKey?.let { Base64.getDecoder().decode(it) }
                val uin = unlock.payload.decoyUIN ?: randomDecoyUIN()
                val nick = unlock.payload.decoyNickname ?: "user-${(1000..9999).random()}"
                _mode.value = SessionMode.decoy
                _lockState.value = LockState.unlocked
                Timber.d("PanicPIN: unlocked decoy session uin=$uin")
                SubmitResult.UnlockedDecoy(uin, nick)
            }
            PINVaultManager.SlotMode.wipe -> SubmitResult.Wipe
        }
    }

    fun lock() {
        if (!vault.isConfigured || _lockState.value == LockState.locked) return
        dataKey = null; realPayload = null; realSlotKey = null
        _mode.value = SessionMode.none
        _lockState.value = LockState.locked
        Timber.d("PanicPIN: locked")
    }

    // Real PIN: create vault or update existing real slot
    suspend fun setRealPIN(pin: String) = withContext(Dispatchers.IO) {
        require(pin.length >= 4) { "PIN must be at least 4 digits" }
        if (!vault.isConfigured) {
            val unlock = vault.createWithRealPIN(pin)
            realPayload = unlock.payload
            realSlotKey = unlock.slotKey
            dataKey = unlock.payload.dataKey?.let { Base64.getDecoder().decode(it) }
            _mode.value = SessionMode.real
            _lockState.value = LockState.unlocked
        } else {
            require(_mode.value == SessionMode.real) { "Not in real session" }
            val payload = realPayload ?: error("No real payload")
            val layout = payload.layout ?: error("No layout")
            val salt = vault.vaultSalt() ?: error("No vault salt")
            val newKey = vault.deriveKey(pin, salt)
            vault.writeSlot(layout.realSlot, payload, newKey)
            realSlotKey = newKey
        }
    }

    suspend fun setDecoyPIN(pin: String) = withContext(Dispatchers.IO) {
        require(pin.length >= 4) { "PIN must be at least 4 digits" }
        require(_mode.value == SessionMode.real) { "Not in real session" }
        require(!_biometricEnabled.value) { "Disable biometric before setting decoy PIN" }
        val payload = realPayload ?: error("No real payload")
        var layout = payload.layout ?: error("No layout")
        val salt = vault.vaultSalt() ?: error("No vault salt")
        val slotKey = realSlotKey ?: error("No slot key")

        val decoyDataKey = layout.decoyDataKey
            ?: Base64.getEncoder().encodeToString(randomBytes(32))
        val decoyUIN = layout.decoyUIN ?: randomDecoyUIN()
        val decoyNick = layout.decoyNickname ?: "user-${(1000..9999).random()}"
        val slotIndex = layout.decoySlot ?: vault.freeSlotIndex(layout) ?: error("No free slot")

        vault.writeSlot(slotIndex,
            PINVaultManager.SlotPayload(
                mode = PINVaultManager.SlotMode.decoy,
                dataKey = decoyDataKey,
                decoyUIN = decoyUIN,
                decoyNickname = decoyNick
            ),
            vault.deriveKey(pin, salt)
        )
        layout = layout.copy(
            decoySlot = slotIndex,
            decoyDataKey = decoyDataKey,
            decoyUIN = decoyUIN,
            decoyNickname = decoyNick
        )
        val updated = payload.copy(layout = layout)
        vault.writeSlot(layout.realSlot, updated, slotKey)
        realPayload = updated
    }

    suspend fun setWipePIN(pin: String) = withContext(Dispatchers.IO) {
        require(pin.length >= 4) { "PIN must be at least 4 digits" }
        require(_mode.value == SessionMode.real) { "Not in real session" }
        require(!_biometricEnabled.value) { "Disable biometric before setting wipe PIN" }
        val payload = realPayload ?: error("No real payload")
        var layout = payload.layout ?: error("No layout")
        val salt = vault.vaultSalt() ?: error("No vault salt")
        val slotKey = realSlotKey ?: error("No slot key")

        val slotIndex = layout.wipeSlot ?: vault.freeSlotIndex(layout) ?: error("No free slot")
        vault.writeSlot(slotIndex,
            PINVaultManager.SlotPayload(mode = PINVaultManager.SlotMode.wipe),
            vault.deriveKey(pin, salt)
        )
        layout = layout.copy(wipeSlot = slotIndex)
        val updated = payload.copy(layout = layout)
        vault.writeSlot(layout.realSlot, updated, slotKey)
        realPayload = updated
    }

    fun enableBiometric(): Boolean {
        require(_mode.value == SessionMode.real) { "Not in real session" }
        val p = realPayload ?: return false
        require(!hasDecoyPIN && !hasWipePIN) { "Disable decoy/wipe PINs first" }
        val ok = biometricManager.enable(p)
        if (ok) _biometricEnabled.value = true
        return ok
    }

    fun disableBiometric() {
        biometricManager.disable()
        _biometricEnabled.value = false
    }

    suspend fun removeAllPINs() = withContext(Dispatchers.IO) {
        require(_mode.value == SessionMode.real) { "Not in real session" }
        vault.destroy()
        biometricManager.disable()
        dataKey = null; realPayload = null; realSlotKey = null
        _mode.value = SessionMode.none
        _lockState.value = LockState.unlocked
        _biometricEnabled.value = false
    }

    fun finishWipe() {
        vault.destroy()
        biometricManager.disable()
        dataKey = null; realPayload = null; realSlotKey = null
        _mode.value = SessionMode.none
        _lockState.value = LockState.unlocked
        _biometricEnabled.value = false
    }

    private fun randomDecoyUIN() = (100_000_000L..999_999_999L).random()
    private fun randomBytes(n: Int) = ByteArray(n).also { SecureRandom().nextBytes(it) }
}
