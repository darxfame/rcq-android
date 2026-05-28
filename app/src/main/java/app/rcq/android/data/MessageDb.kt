package app.rcq.android.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import app.rcq.android.model.ChatMessage
import app.rcq.android.model.DeliveryState

/**
 * Local message store. The backend drains messages on delivery (rcq-spec
 * 6.3.1), so chat history only survives if the client keeps it — this is
 * the Android analogue of the iOS MessageDB. Hand-rolled SQLite (like iOS)
 * rather than Room to avoid an annotation-processor dependency.
 *
 * The primary key is the envelope UUID, and inserts are INSERT OR IGNORE,
 * which gives free de-duplication for a message that arrives over both the
 * WebSocket and the queue drain.
 */
class MessageDb(context: Context) : SQLiteOpenHelper(context.applicationContext, NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE messages (
              id        TEXT PRIMARY KEY,
              peer_uin  INTEGER NOT NULL,
              from_me   INTEGER NOT NULL,
              body      TEXT NOT NULL,
              sent_at   INTEGER NOT NULL,
              state     TEXT NOT NULL DEFAULT 'DELIVERED',
              kind      TEXT NOT NULL DEFAULT 'text',
              media_id  TEXT,
              media_key TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_messages_peer ON messages(peer_uin, sent_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE messages ADD COLUMN state TEXT NOT NULL DEFAULT 'DELIVERED'")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE messages ADD COLUMN kind TEXT NOT NULL DEFAULT 'text'")
            db.execSQL("ALTER TABLE messages ADD COLUMN media_id TEXT")
            db.execSQL("ALTER TABLE messages ADD COLUMN media_key TEXT")
        }
    }

    /** Insert; returns true if it was new (false if the UUID already existed). */
    fun insert(msg: ChatMessage): Boolean {
        val values = ContentValues().apply {
            put("id", msg.id)
            put("peer_uin", msg.peerUin)
            put("from_me", if (msg.fromMe) 1 else 0)
            put("body", msg.body)
            put("sent_at", msg.sentAt)
            put("state", msg.state.name)
            put("kind", msg.kind)
            put("media_id", msg.mediaId)
            put("media_key", msg.mediaKey)
        }
        val rowId = writableDatabase.insertWithOnConflict(
            "messages", null, values, SQLiteDatabase.CONFLICT_IGNORE,
        )
        return rowId != -1L
    }

    fun updateState(id: String, state: DeliveryState) {
        val values = ContentValues().apply { put("state", state.name) }
        writableDatabase.update("messages", values, "id = ?", arrayOf(id))
    }

    fun wipe() {
        writableDatabase.execSQL("DELETE FROM messages")
    }

    fun all(): List<ChatMessage> {
        val out = ArrayList<ChatMessage>()
        readableDatabase.rawQuery(
            "SELECT id, peer_uin, from_me, body, sent_at, state, kind, media_id, media_key FROM messages ORDER BY sent_at ASC", null,
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    ChatMessage(
                        id = c.getString(0),
                        peerUin = c.getInt(1),
                        fromMe = c.getInt(2) == 1,
                        body = c.getString(3),
                        sentAt = c.getLong(4),
                        state = runCatching { DeliveryState.valueOf(c.getString(5)) }.getOrDefault(DeliveryState.DELIVERED),
                        kind = c.getString(6) ?: "text",
                        mediaId = c.getString(7),
                        mediaKey = c.getString(8),
                    )
                )
            }
        }
        return out
    }

    private companion object {
        const val NAME = "rcq-messages.db"
        const val VERSION = 3
    }
}
