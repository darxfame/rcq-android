package app.rcq.android.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.rcq.android.model.ChatMessage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device tests for the SQLCipher-encrypted message DB: write/read under a
 * key, and rekey (PIN set/change) preserves rows while changing the key.
 */
@RunWith(AndroidJUnit4::class)
class MessageDbEncryptedTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val acctId = "test-pin-msgdb"

    @After
    fun cleanup() = MessageDb.wipeAccount(ctx, acctId)

    private fun msg(id: String, body: String) =
        ChatMessage(id = id, peerUin = 42, fromMe = true, body = body, sentAt = 1000L)

    private fun keyOf(seed: Int) = ByteArray(32) { (it + seed).toByte() }

    @Test
    fun writeReadUnderKey() {
        MessageDb.wipeAccount(ctx, acctId)
        val db = MessageDb(ctx, acctId, keyOf(0))
        assertTrue(db.insert(msg("m1", "hello")))
        assertTrue(db.insert(msg("m2", "world")))
        assertEquals(2, db.all().size)
        assertEquals("hello", db.all().first { it.id == "m1" }.body)
        db.close()
        // Reopening under the same key reads it all back.
        val db2 = MessageDb(ctx, acctId, keyOf(0))
        assertEquals(2, db2.all().size)
        db2.close()
    }

    @Test
    fun plaintextMigrationPreservesRows() {
        val migId = "test-pin-migrate"
        SecureStore.wipeAccount(ctx, migId) // clear the "migrated" marker
        MessageDb.wipeAccount(ctx, migId)
        // Seed a PLAINTEXT db (the pre-SQLCipher format) with the framework SQLite.
        val file = ctx.getDatabasePath("rcq-messages-$migId.db")
        file.parentFile?.mkdirs()
        val plain = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
        plain.execSQL(
            "CREATE TABLE messages (id TEXT PRIMARY KEY, peer_uin INTEGER NOT NULL, from_me INTEGER NOT NULL, " +
                "body TEXT NOT NULL, sent_at INTEGER NOT NULL, state TEXT NOT NULL DEFAULT 'DELIVERED', " +
                "kind TEXT NOT NULL DEFAULT 'text', media_id TEXT, media_key TEXT, reply_snippet TEXT, " +
                "reply_author TEXT, group_id INTEGER, sender_uin INTEGER, reactions TEXT, " +
                "edited INTEGER NOT NULL DEFAULT 0, file_name TEXT, file_mime TEXT, file_size INTEGER, " +
                "duration_sec INTEGER, thumb_b64 TEXT, lat REAL, lng REAL)"
        )
        plain.execSQL("INSERT INTO messages (id, peer_uin, from_me, body, sent_at) VALUES ('m1', 42, 1, 'plainhello', 1000)")
        plain.execSQL("INSERT INTO messages (id, peer_uin, from_me, body, sent_at) VALUES ('m2', 42, 0, 'plainworld', 1001)")
        plain.version = MessageDb.VERSION
        plain.close()
        try {
            // Migrate plaintext -> SQLCipher-encrypted under the device key.
            val deviceKey = keyOf(3)
            assertTrue(MessageDb.migrateToEncrypted(ctx, migId, deviceKey))
            // The encrypted DB opens under that key and every row survived.
            val db = MessageDb(ctx, migId, deviceKey)
            val all = db.all()
            assertEquals(2, all.size)
            assertEquals("plainhello", all.first { it.id == "m1" }.body)
            db.close()
            // And it's genuinely encrypted now: opening with no key fails.
            var plaintextOpenFailed = false
            try {
                MessageDb(ctx, migId, ByteArray(32)).all()
            } catch (e: Exception) {
                plaintextOpenFailed = true
            }
            assertTrue("migrated DB must be encrypted", plaintextOpenFailed)
        } finally {
            MessageDb.wipeAccount(ctx, migId)
            SecureStore.wipeAccount(ctx, migId)
        }
    }

    @Test
    fun rekeyChangesKeyKeepsRows() {
        MessageDb.wipeAccount(ctx, acctId)
        val db = MessageDb(ctx, acctId, keyOf(0))
        db.insert(msg("m1", "secret"))
        db.rekey(keyOf(7))
        db.close()
        // The NEW key opens and reads the row.
        val db2 = MessageDb(ctx, acctId, keyOf(7))
        assertEquals("secret", db2.all().single().body)
        db2.close()
        // The OLD key can no longer open the rekeyed DB.
        var failed = false
        try {
            MessageDb(ctx, acctId, keyOf(0)).all()
        } catch (e: Exception) {
            failed = true
        }
        assertTrue("old key must not open the rekeyed DB", failed)
    }
}
