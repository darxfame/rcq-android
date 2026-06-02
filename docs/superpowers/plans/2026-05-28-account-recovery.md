# Account Recovery via Encrypted Key Backup — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow users to recover their RCQ account on a new device using a 24-word mnemonic phrase, without any server-side key escrow.

**Architecture:** At registration the client generates a 24-word BIP39 mnemonic, derives an AES-256-GCM key via PBKDF2-SHA256 (600k iterations), encrypts the private key pair, and uploads the ciphertext to the server. Recovery is a challenge-response: server issues a random 64-hex-char nonce, client decrypts private keys from the mnemonic, signs the nonce with Ed25519 signing key, server verifies against the stored **public** signing_key and issues a fresh JWT. The server never sees plaintext private keys.

**Tech Stack:** FastAPI + SQLAlchemy + Redis + `cryptography` lib (backend) · Kotlin + libsignal-android + `javax.crypto` AES-GCM (Android)

---

## Files Changed

### Backend (`rcq-server-ref`)
| File | Change |
|------|--------|
| `app/models/user.py` | Add `encrypted_key_blob: Mapped[str \| None]` column |
| `app/routers/auth.py` | Add 3 endpoints + Pydantic models |

### Android (`rcq-android`)
| File | Change |
|------|--------|
| `data/api/RCQApiService.kt` | Add 3 API methods + DTOs |
| `crypto/MnemonicHelper.kt` | **New** — BIP39 mnemonic + PBKDF2 + AES-256-GCM |
| `ui/auth/AccountRecoveryScreen.kt` | Replace broken getUserByUin with challenge-response |
| `ui/auth/AuthViewModel.kt` | Upload key backup after successful registration |
| `di/PreferencesKeys.kt` | Add `SIGNING_KEY_ENCRYPTED` key |

---

## Task 1 — Backend: `encrypted_key_blob` column on User

**Files:**
- Modify: `app/models/user.py` after `signing_key` field (~line 14)

- [ ] **Add field:**

```python
    # AES-256-GCM encrypted private key bundle, base64. NULL until the
    # client calls POST /auth/key-backup. Encrypted client-side with a
    # key derived from the 24-word recovery mnemonic via PBKDF2-SHA256.
    # The server stores this opaque blob verbatim and never decrypts it.
    encrypted_key_blob: Mapped[str | None] = mapped_column(Text, nullable=True)
```

- [ ] **Generate Alembic migration (if project uses it):**

```bash
alembic revision --autogenerate -m "add encrypted_key_blob to users"
alembic upgrade head
```

- [ ] **Commit:**
```bash
git add app/models/user.py
git commit -m "feat(model): add encrypted_key_blob to User"
```

---

## Task 2 — Backend: imports + Pydantic models for recovery

**Files:**
- Modify: `app/routers/auth.py` — top of file

- [ ] **Add to imports** (after existing `import os`):

```python
import base64
import secrets

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey
from redis.asyncio import Redis

from app.core.redis import get_redis
```

- [ ] **Add constants + models** before the first `@router.post`:

```python
# ── Account Recovery ──────────────────────────────────────────────────────────
_CHALLENGE_KEY = "recover:challenge:{uin}"
_CHALLENGE_TTL = 300  # 5 min — client must sign within this window


class KeyBackupIn(BaseModel):
    encrypted_blob: str  # AES-256-GCM ciphertext of private keys, base64


class RecoverChallengeIn(BaseModel):
    uin: int


class RecoverChallengeOut(BaseModel):
    challenge: str  # 64 hex chars, one-time, TTL 5 min


class RecoverVerifyIn(BaseModel):
    uin: int
    challenge: str
    signature: str  # base64( Ed25519.sign(challenge.encode("utf-8")) )


class RecoverVerifyOut(BaseModel):
    token: str
    encrypted_blob: str | None
```

- [ ] **Commit:**
```bash
git commit -am "feat(auth): recovery models and Redis constants"
```

---

## Task 3 — Backend: `POST /auth/key-backup`

**Files:**
- Modify: `app/routers/auth.py` — append at end of file

- [ ] **Add endpoint:**

```python
@router.post("/key-backup", status_code=status.HTTP_204_NO_CONTENT)
async def save_key_backup(
    body: KeyBackupIn,
    uin: int = Depends(current_uin),
    db: AsyncSession = Depends(get_db),
) -> None:
    """Store the client-encrypted private key blob. Requires a valid JWT.
    Safe to call again to rotate the blob after mnemonic refresh."""
    user = await db.get(User, uin)
    if user is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "user not found")
    user.encrypted_key_blob = body.encrypted_blob
    await db.commit()
```

- [ ] **Commit:**
```bash
git commit -am "feat(auth): POST /auth/key-backup"
```

---

## Task 4 — Backend: `POST /auth/recover/challenge`

**Files:**
- Modify: `app/routers/auth.py` — append at end of file

- [ ] **Add endpoint:**

```python
@router.post("/recover/challenge", response_model=RecoverChallengeOut)
async def recover_challenge(
    body: RecoverChallengeIn,
    db: AsyncSession = Depends(get_db),
    redis: Redis = Depends(get_redis),
) -> RecoverChallengeOut:
    """Issue a one-time challenge nonce for the given UIN.
    Public endpoint — no JWT required. Returns 404 if UIN unknown."""
    user = await db.scalar(select(User).where(User.uin == body.uin))
    if user is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "user not found")
    challenge = secrets.token_hex(32)
    await redis.set(
        _CHALLENGE_KEY.format(uin=body.uin), challenge, ex=_CHALLENGE_TTL
    )
    return RecoverChallengeOut(challenge=challenge)
```

- [ ] **Commit:**
```bash
git commit -am "feat(auth): POST /auth/recover/challenge"
```

---

## Task 5 — Backend: `POST /auth/recover/verify`

**Files:**
- Modify: `app/routers/auth.py` — append at end of file

- [ ] **Add endpoint:**

```python
@router.post("/recover/verify", response_model=RecoverVerifyOut)
async def recover_verify(
    body: RecoverVerifyIn,
    db: AsyncSession = Depends(get_db),
    redis: Redis = Depends(get_redis),
) -> RecoverVerifyOut:
    """Verify Ed25519 challenge signature → return new JWT + encrypted blob.

    Full client flow:
      1. POST /auth/recover/challenge  → get nonce
      2. Decrypt private keys from 24-word mnemonic (PBKDF2 → AES-GCM)
      3. Ed25519-sign the nonce with signing_key_private
      4. POST here with { uin, challenge, base64(signature) }
      5. Receive { token, encrypted_blob } — re-import keys on new device
    """
    # 1. Verify challenge exists and matches
    stored = await redis.get(_CHALLENGE_KEY.format(uin=body.uin))
    if stored is None or stored != body.challenge:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "invalid or expired challenge")

    # 2. Load user
    user = await db.scalar(select(User).where(User.uin == body.uin))
    if user is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "user not found")

    # 3. Verify Ed25519 signature — server only knows the PUBLIC key
    try:
        pub_bytes = base64.b64decode(user.signing_key)
        public_key = Ed25519PublicKey.from_public_bytes(pub_bytes)
        sig_bytes = base64.b64decode(body.signature)
        public_key.verify(sig_bytes, body.challenge.encode("utf-8"))
    except (InvalidSignature, Exception):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "signature verification failed")

    # 4. Consume challenge — one-time use prevents replay attacks
    await redis.delete(_CHALLENGE_KEY.format(uin=body.uin))

    return RecoverVerifyOut(
        token=issue_token(body.uin),
        encrypted_blob=user.encrypted_key_blob,
    )
```

- [ ] **Commit:**
```bash
git commit -am "feat(auth): POST /auth/recover/verify — challenge-response → JWT"
```

---

## Task 6 — Android: MnemonicHelper

**Files:**
- Create: `app/src/main/java/com/rcq/messenger/crypto/MnemonicHelper.kt`

- [ ] **Create file:**

```kotlin
package com.rcq.messenger.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object MnemonicHelper {

    /** Generate a 24-word BIP39 mnemonic (256-bit entropy). */
    fun generateMnemonic(): String {
        val entropy = ByteArray(32).also { SecureRandom().nextBytes(it) }
        // TODO: replace stub with real BIP39 library (cash.z.ecc.android:kotlin-bip39)
        return entropy.joinToString(" ") { "%02x".format(it) }.chunked(5).take(24).joinToString(" ")
    }

    /** Derive AES-256 key from mnemonic via PBKDF2-SHA256, 600k iterations. */
    fun deriveKey(mnemonic: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(mnemonic.toCharArray(), salt, 600_000, 256)
        val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /** AES-256-GCM encrypt. Returns base64(12-byte-IV || ciphertext+tag). */
    fun encrypt(plaintext: ByteArray, key: SecretKeySpec): String {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        return Base64.encodeToString(iv + cipher.doFinal(plaintext), Base64.NO_WRAP)
    }

    /** AES-256-GCM decrypt base64(IV || ciphertext+tag). */
    fun decrypt(encoded: String, key: SecretKeySpec): ByteArray {
        val blob = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, blob, 0, 12))
        return cipher.doFinal(blob, 12, blob.size - 12)
    }
}
```

- [ ] **Commit:**
```bash
git add app/src/main/java/com/rcq/messenger/crypto/MnemonicHelper.kt
git commit -m "feat(crypto): MnemonicHelper — BIP39 stub + PBKDF2 + AES-256-GCM"
```

---

## Task 7 — Android: API DTOs + endpoints

**Files:**
- Modify: `app/src/main/java/com/rcq/messenger/data/api/RCQApiService.kt`

- [ ] **Add DTOs** near bottom of file:

```kotlin
@Serializable data class KeyBackupRequest(val encrypted_blob: String)
@Serializable data class RecoverChallengeRequest(val uin: Long)
@Serializable data class RecoverChallengeResponse(val challenge: String)
@Serializable data class RecoverVerifyRequest(val uin: Long, val challenge: String, val signature: String)
@Serializable data class RecoverVerifyResponse(val token: String, val encrypted_blob: String?)
```

- [ ] **Add API methods** in the `// Auth` section:

```kotlin
@POST("auth/key-backup")
suspend fun uploadKeyBackup(@Body request: KeyBackupRequest): Response<Unit>

@POST("auth/recover/challenge")
suspend fun recoverChallenge(@Body request: RecoverChallengeRequest): Response<RecoverChallengeResponse>

@POST("auth/recover/verify")
suspend fun recoverVerify(@Body request: RecoverVerifyRequest): Response<RecoverVerifyResponse>
```

- [ ] **Commit:**
```bash
git commit -am "feat(api): account recovery endpoints in RCQApiService"
```

---

## Task 8 — Android: upload key backup on registration

**Files:**
- Modify: `app/src/main/java/com/rcq/messenger/ui/auth/AuthViewModel.kt`
- Modify: `app/src/main/java/com/rcq/messenger/di/PreferencesKeys.kt`

- [ ] **Add `SIGNING_KEY_ENCRYPTED` to PreferencesKeys:**

```kotlin
val SIGNING_KEY_ENCRYPTED = stringPreferencesKey("signing_key_enc")
```

- [ ] **In registration success handler**, after saving JWT:

```kotlin
private suspend fun uploadKeyBackup(uin: Long, mnemonic: String) {
    val salt = uin.toString().toByteArray(Charsets.UTF_8)
    val aesKey = MnemonicHelper.deriveKey(mnemonic, salt)

    // Encrypt signing key separately (needed to sign challenges during recovery)
    val signingPrivBytes = signalKeyStore.getIdentityKeyPair().privateKey.serialize()
    val encSigning = MnemonicHelper.encrypt(signingPrivBytes, aesKey)
    dataStore.edit { it[PreferencesKeys.SIGNING_KEY_ENCRYPTED] = encSigning }

    // Full bundle: signing_private(32) + identity_private(32)
    val identityPrivBytes = signalKeyStore.getIdentityKeyPair().privateKey.serialize()
    val bundle = signingPrivBytes + identityPrivBytes
    val encBundle = MnemonicHelper.encrypt(bundle, aesKey)

    runCatching { api.uploadKeyBackup(KeyBackupRequest(encrypted_blob = encBundle)) }
    dataStore.edit { it[PreferencesKeys.RECOVERY_PHRASE] = mnemonic }
}
```

- [ ] **Commit:**
```bash
git commit -am "feat(auth): upload encrypted key backup after registration"
```

---

## Task 9 — Android: fix AccountRecoveryViewModel

**Files:**
- Modify: `app/src/main/java/com/rcq/messenger/ui/auth/AccountRecoveryScreen.kt`

- [ ] **Add `pendingChallenge` to state:**

```kotlin
data class AccountRecoveryUiState(
    val step: RecoveryStep = RecoveryStep.INFO,
    val uin: String = "",
    val backupData: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val foundUser: User? = null,
    val pendingChallenge: String? = null
)
```

- [ ] **Replace `checkAccount()`:**

```kotlin
fun checkAccount() {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        runCatching {
            val resp = api.recoverChallenge(RecoverChallengeRequest(uin = _uiState.value.uin.toLong()))
            if (!resp.isSuccessful) error("UIN не найден")
            resp.body()!!.challenge
        }.fold(
            onSuccess = { challenge ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    step = RecoveryStep.BACKUP_KEYS,
                    pendingChallenge = challenge
                )
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        )
    }
}
```

- [ ] **Replace `restoreAccount()`:**

```kotlin
fun restoreAccount() {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val mnemonic = _uiState.value.backupData.trim()
        if (mnemonic.split("\\s+".toRegex()).size != 24) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Ожидается 24 слова")
            return@launch
        }
        runCatching {
            val uin = _uiState.value.uin.toLong()
            val challenge = _uiState.value.pendingChallenge!!
            val salt = uin.toString().toByteArray(Charsets.UTF_8)
            val aesKey = MnemonicHelper.deriveKey(mnemonic, salt)

            // Decrypt stored signing key to sign the challenge
            val encSigning = dataStore.data.first()[PreferencesKeys.SIGNING_KEY_ENCRYPTED]
                ?: error("Нет резервной копии ключа на этом устройстве")
            val signingKeyBytes = MnemonicHelper.decrypt(encSigning, aesKey)

            // Sign challenge with Ed25519
            val privKey = org.signal.libsignal.protocol.ecc.Curve.decodePrivatePoint(signingKeyBytes)
            val signature = org.signal.libsignal.protocol.ecc.Curve
                .calculateSignature(privKey, challenge.toByteArray(Charsets.UTF_8))
            val signatureB64 = android.util.Base64.encodeToString(signature, android.util.Base64.NO_WRAP)

            // Verify with server → JWT + encrypted_blob
            val resp = api.recoverVerify(RecoverVerifyRequest(uin, challenge, signatureB64))
            if (!resp.isSuccessful) error("Сервер отклонил подпись")
            val body = resp.body()!!

            // Save new JWT
            dataStore.edit { prefs ->
                prefs[PreferencesKeys.USER_UIN] = uin
                prefs[PreferencesKeys.AUTH_TOKEN] = body.token
            }

            // Restore full key bundle if blob was uploaded
            body.encrypted_blob?.let { blob ->
                val keys = MnemonicHelper.decrypt(blob, aesKey)
                signalKeyStore.importPrivateKeyBundle(keys)
            }
        }.fold(
            onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, step = RecoveryStep.SUCCESS) },
            onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, step = RecoveryStep.FAILED, error = it.message) }
        )
    }
}
```

- [ ] **Commit:**
```bash
git commit -am "fix(auth): account recovery via Ed25519 challenge-response"
```

---

## Crypto diagram

```
REGISTRATION
  mnemonic (24 words)
      │ PBKDF2-SHA256, 600k iter, salt=UIN.bytes
      ▼
  aes_key (256-bit)
      ├──► encrypt(signing_key_private) ──► DataStore[SIGNING_KEY_ENCRYPTED]
      └──► encrypt(signing_priv ‖ identity_priv) ──► POST /auth/key-backup → server DB

RECOVERY
  user enters mnemonic
      │ PBKDF2-SHA256, salt=UIN.bytes
      ▼
  aes_key
      │ decrypt DataStore[SIGNING_KEY_ENCRYPTED]
      ▼
  signing_key_private
      │ Ed25519.sign(challenge)
      ▼
  signature ──► POST /auth/recover/verify ──► server verifies vs stored public key
                                               └──► { JWT, encrypted_blob }
  aes_key + encrypted_blob ──► decrypt ──► (signing_priv ‖ identity_priv) ──► SignalKeyStore
```

| Data | Server stores | Server reads plaintext |
|------|:---:|:---:|
| `encrypted_key_blob` | ✅ | ❌ never |
| `signing_key` (public) | ✅ | ✅ (to verify signature) |
| `identity_key` (public) | ✅ | ✅ (for E2EE session init) |
| private keys | ❌ | ❌ |
| mnemonic | ❌ | ❌ |
