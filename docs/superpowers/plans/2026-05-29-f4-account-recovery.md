# F4 Account Recovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Wire the existing `AccountRecoveryScreen` + `MnemonicHelper` to the real server endpoints `/auth/key-backup`, `/auth/recover/challenge`, `/auth/recover/verify`.

**Architecture:** After registration, encrypt the Signal identity private key with a PBKDF2-derived AES key from the mnemonic and upload to `/auth/key-backup`. Recovery: call `/auth/recover/challenge` (get nonce + encrypted blob), decrypt the identity key with the mnemonic, sign the nonce with Ed25519 via libsignal, call `/auth/recover/verify` to get a new token.

**Tech Stack:** Kotlin, libsignal-android (Ed25519 via `Curve.calculateSignature`), MnemonicHelper (PBKDF2-SHA256 600k + AES-256-GCM), Retrofit, DataStore

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `data/api/RCQApiService.kt` | Modify | Add 3 endpoints + 4 DTOs |
| `crypto/CryptoService.kt` | Modify | Add `exportIdentityPrivateKey()` |
| `ui/auth/AuthViewModel.kt` | Modify | Upload key-backup post-registration |
| `ui/auth/AccountRecoveryScreen.kt` | Modify | Wire VM to real challenge-response flow |

---

## Task 1: Add recovery API endpoints

**Files:**
- Modify: `app/src/main/java/com/rcq/messenger/data/api/RCQApiService.kt`

- [ ] Add after `suspend fun register(...)`:

```kotlin
@POST("auth/key-backup")
suspend fun uploadKeyBackup(@Body request: KeyBackupRequest): Response<Unit>

@POST("auth/recover/challenge")
suspend fun recoverChallenge(@Body request: RecoverChallengeRequest): Response<RecoverChallengeResponse>

@POST("auth/recover/verify")
suspend fun recoverVerify(@Body request: RecoverVerifyRequest): Response<AuthResponse>
```

- [ ] Add DTOs at the bottom of the file before closing `}`:

```kotlin
@kotlinx.serialization.Serializable
data class KeyBackupRequest(
    @kotlinx.serialization.SerialName("encrypted_blob") val encryptedBlob: String,
    @kotlinx.serialization.SerialName("pbkdf2_salt") val pbkdf2Salt: String
)

@kotlinx.serialization.Serializable
data class RecoverChallengeRequest(val uin: Long)

@kotlinx.serialization.Serializable
data class RecoverChallengeResponse(
    val nonce: String,
    @kotlinx.serialization.SerialName("encrypted_blob") val encryptedBlob: String,
    @kotlinx.serialization.SerialName("pbkdf2_salt") val pbkdf2Salt: String
)

@kotlinx.serialization.Serializable
data class RecoverVerifyRequest(
    val uin: Long,
    val nonce: String,
    val signature: String
)
```

- [ ] `./gradlew kspDebugKotlin --no-daemon 2>&1 | grep -E "ERROR|BUILD"`

- [ ] Commit + push:
```bash
git add app/src/main/java/com/rcq/messenger/data/api/RCQApiService.kt
git commit -m "feat: эндпоинты восстановления — key-backup/challenge/verify"
git push origin phase-1-core-messaging
```

---

## Task 2: Export identity key from CryptoService

**Files:**
- Modify: `app/src/main/java/com/rcq/messenger/crypto/CryptoService.kt`

- [ ] Add to `CryptoService` class:

```kotlin
/** Returns raw 32-byte Ed25519 private key bytes for backup encryption. */
fun exportIdentityPrivateKey(): ByteArray =
    store.identityKeyPair.privateKey.serialize()
```

- [ ] `./gradlew kspDebugKotlin --no-daemon 2>&1 | grep -E "ERROR|BUILD"`

- [ ] Commit + push:
```bash
git add app/src/main/java/com/rcq/messenger/crypto/CryptoService.kt
git commit -m "feat: экспорт Ed25519 identity key для резервного копирования"
git push origin phase-1-core-messaging
```

---

## Task 3: Upload key-backup after registration

**Files:**
- Modify: `app/src/main/java/com/rcq/messenger/ui/auth/AuthViewModel.kt`

- [ ] Find where registration saves UIN+token to DataStore. Immediately after that block add:

```kotlin
// Async key backup — non-blocking, failure is logged but not fatal
viewModelScope.launch {
    runCatching {
        val mnemonic = prefs[KEY_RECOVERY_PHRASE] ?: return@runCatching
        val privKey = cryptoService.exportIdentityPrivateKey()
        val salt = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        val aesKey = com.rcq.messenger.crypto.MnemonicHelper.deriveKey(mnemonic, salt)
        val blob = com.rcq.messenger.crypto.MnemonicHelper.encrypt(privKey, aesKey)
        val saltB64 = android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)
        api.uploadKeyBackup(com.rcq.messenger.data.api.KeyBackupRequest(blob, saltB64))
    }.onFailure { android.util.Log.w("AuthVM", "key-backup failed: ${it.message}") }
}
```

- [ ] `./gradlew kspDebugKotlin --no-daemon 2>&1 | grep -E "ERROR|BUILD"`

- [ ] Commit + push:
```bash
git add app/src/main/java/com/rcq/messenger/ui/auth/AuthViewModel.kt
git commit -m "feat: загрузка зашифрованного ключа после регистрации"
git push origin phase-1-core-messaging
```

---

## Task 4: Wire AccountRecoveryViewModel to server challenge-response

**Files:**
- Modify: `app/src/main/java/com/rcq/messenger/ui/auth/AccountRecoveryScreen.kt`

- [ ] Add `api` and `cryptoService` to `AccountRecoveryViewModel` constructor:

```kotlin
@HiltViewModel
class AccountRecoveryViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val dataStore: DataStore<Preferences>,
    private val api: com.rcq.messenger.data.api.RCQApiService,
    private val cryptoService: com.rcq.messenger.crypto.CryptoService,
    @ApplicationContext private val context: Context
) : ViewModel() {
```

- [ ] Replace `restoreAccount()` with:

```kotlin
fun restoreAccount() {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val words = _uiState.value.backupData.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (words.size != 24) {
            _uiState.value = _uiState.value.copy(isLoading = false,
                error = "Ожидается 24 слова, введено: ${words.size}")
            return@launch
        }
        runCatching {
            val uin = _uiState.value.foundUser?.id ?: _uiState.value.uin.toLong()
            val mnemonic = words.joinToString(" ")

            // 1. Get challenge + encrypted blob
            val cr = api.recoverChallenge(
                com.rcq.messenger.data.api.RecoverChallengeRequest(uin)
            )
            if (!cr.isSuccessful) throw Exception("Challenge failed: ${cr.code()}")
            val ch = cr.body()!!

            // 2. Decrypt identity private key from blob
            val salt = android.util.Base64.decode(ch.pbkdf2Salt, android.util.Base64.NO_WRAP)
            val aesKey = com.rcq.messenger.crypto.MnemonicHelper.deriveKey(mnemonic, salt)
            val privKeyBytes = com.rcq.messenger.crypto.MnemonicHelper.decrypt(ch.encryptedBlob, aesKey)

            // 3. Sign nonce with recovered Ed25519 key
            val privKey = org.signal.libsignal.protocol.ecc.Curve.decodePrivatePoint(privKeyBytes)
            val nonce = android.util.Base64.decode(ch.nonce, android.util.Base64.NO_WRAP)
            val sig = org.signal.libsignal.protocol.ecc.Curve.calculateSignature(privKey, nonce)
            val sigB64 = android.util.Base64.encodeToString(sig, android.util.Base64.NO_WRAP)

            // 4. Verify → token
            val vr = api.recoverVerify(
                com.rcq.messenger.data.api.RecoverVerifyRequest(uin, ch.nonce, sigB64)
            )
            if (!vr.isSuccessful) throw Exception("Verify failed: ${vr.code()}")
            val auth = vr.body()!!

            // 5. Persist credentials
            dataStore.edit { p ->
                p[KEY_UIN] = auth.uin
                p[KEY_TOKEN] = auth.token
                p[KEY_RECOVERY_PHRASE] = mnemonic
                p[KEY_NICKNAME] = _uiState.value.foundUser?.nickname ?: ""
            }
            _uiState.value = _uiState.value.copy(isLoading = false, step = RecoveryStep.SUCCESS)
        }.onFailure { e ->
            _uiState.value = _uiState.value.copy(isLoading = false,
                step = RecoveryStep.FAILED, error = e.message)
        }
    }
}
```

- [ ] `./gradlew assembleDebug --no-daemon 2>&1 | grep -E "ERROR|BUILD"`

- [ ] Commit + push:
```bash
git add app/src/main/java/com/rcq/messenger/ui/auth/AccountRecoveryScreen.kt
git commit -m "feat: восстановление аккаунта Ed25519 challenge-response (F4 complete)"
git push origin phase-1-core-messaging
```

---

## Self-Review
- ✅ All 3 server endpoints covered
- ✅ MnemonicHelper PBKDF2-SHA256 600k iters matches server spec
- ✅ AES-256-GCM encryption for blob
- ✅ Ed25519 signing via libsignal `Curve.calculateSignature`
- ⚠️ `Curve.decodePrivatePoint` — if compile error, try `org.signal.libsignal.protocol.ecc.ECPrivateKey` constructor
- ✅ Non-blocking key-backup (won't block registration if server down)
- ✅ AuthResponse DTO reused (already exists)
