# libsignal E2EE Integration Plan

> **For agentic workers:** Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Replace basic AES-GCM encryption with full Signal Protocol (Double Ratchet) using libsignal library, matching iOS implementation.

**Architecture:** Add libsignal dependency → Create SignalKeyStore implementation → Build SessionManager for Double Ratchet → Integrate into CryptoService → Update MessageEntity schema → Implement send/receive encrypted messages with E2E test.

**Tech Stack:** libsignal, Room Database v3, Retrofit API, Kotlin Coroutines

---

## File Structure

**New files:**
- `app/src/main/java/com/rcq/messenger/crypto/SignalKeyStore.kt` - Signal Protocol Store
- `app/src/main/java/com/rcq/messenger/crypto/SessionManager.kt` - Session management (Double Ratchet)
- `app/src/main/java/com/rcq/messenger/data/entity/SignalKeyEntity.kt` - DB schema for keys
- `app/src/main/java/com/rcq/messenger/data/dao/SignalKeyDao.kt` - Key DAO

**Modify:**
- `app/build.gradle.kts` - Add libsignal dependency
- `app/src/main/java/com/rcq/messenger/crypto/CryptoService.kt` - Replace with Signal Protocol
- `app/src/main/kotlin/com/rcq/messenger/data/entity/MessageEntity.kt` - Add signal_type, ciphertext_type fields
- `app/src/main/kotlin/com/rcq/messenger/data/database/RCQDatabase.kt` - Version 3 migration
- `app/src/main/java/com/rcq/messenger/api/RCQApiService.kt` - Pre-key upload/fetch endpoints

---

## Tasks

### Task 1: Add libsignal Dependency

- [ ] **Step 1:** Modify `app/build.gradle.kts` dependencies section
```kotlin
dependencies {
    // ... existing deps ...
    implementation("org.signal:libsignal-android:0.33.0")
}
```

- [ ] **Step 2:** Run `./gradlew app:dependencies` to verify no conflicts
- [ ] **Step 3:** Commit
```bash
git add app/build.gradle.kts
git commit -m "feat: добавить libsignal зависимость для Signal Protocol E2EE"
```

---

### Task 2: Create Signal Protocol Store Implementation

- [ ] **Step 1:** Create `SignalKeyStore.kt`
```kotlin
package com.rcq.messenger.crypto

import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolException
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalKeyStore @Inject constructor(
    private val signalKeyDao: SignalKeyDao,
    private val identityKeyPair: IdentityKeyPair
) : InMemorySignalProtocolStore(identityKeyPair, 0) {

    override fun storeSession(address: String, record: SessionRecord) {
        super.storeSession(address, record)
        // Persist to DB async
    }

    override fun loadSession(address: String): SessionRecord {
        return super.loadSession(address)
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        super.storePreKey(preKeyId, record)
    }

    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        return super.loadPreKey(preKeyId) ?: throw SignalProtocolException("PreKey $preKeyId not found")
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        super.storeSignedPreKey(signedPreKeyId, record)
    }
}
```

- [ ] **Step 2:** Create test file `SignalKeyStoreTest.kt`
```kotlin
class SignalKeyStoreTest {
    @Test
    fun testLoadPreKey() {
        // Load pre-key, verify not null
        val preKey = store.loadPreKey(1)
        assertNotNull(preKey)
    }
}
```

- [ ] **Step 3:** Run test: `./gradlew app:testDebug -k SignalKeyStoreTest`
- [ ] **Step 4:** Commit
```bash
git add app/src/main/java/com/rcq/messenger/crypto/SignalKeyStore.kt
git commit -m "feat: реализовать SignalKeyStore для Signal Protocol хранилища"
```

---

### Task 3: Create Session Manager for Double Ratchet

- [ ] **Step 1:** Create `SessionManager.kt`
```kotlin
package com.rcq.messenger.crypto

import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.message.CiphertextMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val signalKeyStore: SignalKeyStore
) {

    fun encryptMessage(recipientUin: Long, plaintext: String): CiphertextMessage {
        val address = SignalProtocolAddress(recipientUin.toString(), 1)
        val cipher = SessionCipher(signalKeyStore, address)
        return cipher.encrypt(plaintext.toByteArray(Charsets.UTF_8))
    }

    fun decryptMessage(senderUin: Long, ciphertext: CiphertextMessage): String {
        val address = SignalProtocolAddress(senderUin.toString(), 1)
        val cipher = SessionCipher(signalKeyStore, address)
        val decrypted = cipher.decrypt(ciphertext)
        return String(decrypted, Charsets.UTF_8)
    }
}
```

- [ ] **Step 2:** Write test
```kotlin
class SessionManagerTest {
    @Test
    fun testEncryptDecrypt() {
        val plaintext = "Hello Signal"
        val ciphertext = sessionManager.encryptMessage(123L, plaintext)
        val decrypted = sessionManager.decryptMessage(123L, ciphertext)
        assertEquals(plaintext, decrypted)
    }
}
```

- [ ] **Step 3:** Run: `./gradlew app:testDebug -k SessionManagerTest`
- [ ] **Step 4:** Commit
```bash
git add app/src/main/java/com/rcq/messenger/crypto/SessionManager.kt
git commit -m "feat: реализовать SessionManager для Double Ratchet шифрования"
```

---

### Task 4: Update Message Entity Schema (DB v3)

- [ ] **Step 1:** Update `MessageEntity.kt` to add Signal fields
```kotlin
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: Long,
    val chatId: Long,
    val fromUin: Long,
    val text: String,
    val createdAt: Long,
    val ciphertext: String?, // Signal-encrypted text (base64)
    val nonce: String?, // GCM nonce
    val signal_type: Int = 1, // Signal Protocol version
    val deliveryState: String = "sending",
    val reactions: String = "[]", // JSON array
    val editedAt: Long? = null,
    val replyToId: Long? = null,
    val deletedAt: Long? = null,
    val ttlSeconds: Int? = null
)
```

- [ ] **Step 2:** Create migration file `RCQDatabase_2_to_3.sql`
```sql
ALTER TABLE messages ADD COLUMN ciphertext TEXT;
ALTER TABLE messages ADD COLUMN nonce TEXT;
ALTER TABLE messages ADD COLUMN signal_type INTEGER DEFAULT 1;
```

- [ ] **Step 3:** Update RCQDatabase version to 3 and add migration
```kotlin
@Database(
    entities = [ContactEntity::class, MessageEntity::class, SignalKeyEntity::class],
    version = 3
)
abstract class RCQDatabase : RoomDatabase() {
    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Execute migration
            }
        }
    }
}
```

- [ ] **Step 4:** Run migrations: `./gradlew app:testDebug`
- [ ] **Step 5:** Commit
```bash
git add app/src/main/kotlin/com/rcq/messenger/data/entity/MessageEntity.kt
git commit -m "feat: обновить MessageEntity с Signal Protocol полями (DB v3)"
```

---

### Task 5: Add Pre-Key API Endpoints

- [ ] **Step 1:** Add endpoints to `RCQApiService.kt`
```kotlin
@POST("/crypto/register-keys")
suspend fun uploadPreKeys(@Body request: UploadPreKeysRequest): Response<UploadPreKeysResponse>

@GET("/crypto/keys/{uin}")
suspend fun fetchPreKeyBundle(@Path("uin") uin: Long): Response<PreKeyBundleResponse>

data class UploadPreKeysRequest(
    val preKeys: List<PreKeyData>,
    val signedPreKey: SignedPreKeyData,
    val identityKey: String
)

data class PreKeyData(val id: Int, val key: String)
data class SignedPreKeyData(val id: Int, val key: String, val signature: String)
```

- [ ] **Step 2:** Test endpoints with mock: `./gradlew app:testDebug`
- [ ] **Step 3:** Commit
```bash
git add app/src/main/java/com/rcq/messenger/api/RCQApiService.kt
git commit -m "feat: добавить API endpoints для обмена pre-key бандлами"
```

---

### Task 6: Integrate libsignal into CryptoService

- [ ] **Step 1:** Rewrite `CryptoService.kt` to use Signal Protocol
```kotlin
package com.rcq.messenger.crypto

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoService @Inject constructor(
    private val sessionManager: SessionManager,
    private val keyStore: SignalKeyStore
) {

    fun encryptMessage(recipientUin: Long, text: String): EncryptedMessage {
        val ciphertext = sessionManager.encryptMessage(recipientUin, text)
        return EncryptedMessage(
            ciphertext = android.util.Base64.encodeToString(
                ciphertext.serialize(),
                android.util.Base64.NO_WRAP
            ),
            signalType = 1
        )
    }

    fun decryptMessage(senderUin: Long, ciphertextBase64: String): String {
        val ciphertext = org.signal.libsignal.protocol.message.CiphertextMessage(
            android.util.Base64.decode(ciphertextBase64, android.util.Base64.NO_WRAP)
        )
        return sessionManager.decryptMessage(senderUin, ciphertext)
    }

    data class EncryptedMessage(
        val ciphertext: String,
        val signalType: Int
    )
}
```

- [ ] **Step 2:** Write E2E test
```kotlin
class CryptoServiceE2ETest {
    @Test
    fun testFullE2EEncryption() {
        val plaintext = "Secret message"
        val encrypted = cryptoService.encryptMessage(456L, plaintext)
        val decrypted = cryptoService.decryptMessage(456L, encrypted.ciphertext)
        assertEquals(plaintext, decrypted)
    }
}
```

- [ ] **Step 3:** Run: `./gradlew app:testDebug -k CryptoServiceE2ETest`
- [ ] **Step 4:** Commit
```bash
git add app/src/main/java/com/rcq/messenger/crypto/CryptoService.kt
git commit -m "feat: интегрировать libsignal в CryptoService с полной E2EE"
```

---

### Task 7: Update ChatRepository for Encrypted Messages

- [ ] **Step 1:** Modify `sendMessage()` to encrypt
```kotlin
suspend fun sendMessage(chatId: Long, text: String) {
    val encrypted = cryptoService.encryptMessage(recipientUin, text)
    val messageEntity = MessageEntity(
        text = text,
        ciphertext = encrypted.ciphertext,
        signal_type = 1,
        deliveryState = "sending"
    )
    messageDao.insert(messageEntity)
    
    apiService.sendMessage(
        chatId = chatId,
        ciphertext = encrypted.ciphertext,
        signalType = 1
    )
}
```

- [ ] **Step 2:** Test: `./gradlew app:testDebug`
- [ ] **Step 3:** Commit
```bash
git add app/src/main/java/com/rcq/messenger/data/repository/ChatRepository.kt
git commit -m "feat: обновить ChatRepository для отправки зашифрованных сообщений"
```

---

## Summary

✅ **After all tasks:**
- libsignal dependency added
- Full Double Ratchet encryption working
- Pre-key exchange with server
- Encrypted message storage
- E2E tests passing
- iOS-compatible Signal Protocol

**Commit count:** ~7 commits  
**Test coverage:** >80% crypto paths  
**Estimated time:** 3-4 hours for experienced engineer
