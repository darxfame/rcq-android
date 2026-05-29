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
              media_key TEXT,
              reply_snippet TEXT,
              reply_author  TEXT,
              group_id   INTEGER,
              sender_uin INTEGER,
              reactions  TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_messages_peer ON messages(peer_uin, sent_at)")
        db.execSQL("CREATE INDEX idx_messages_group ON messages(group_id, sent_at)")
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
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE messages ADD COLUMN reply_snippet TEXT")
            db.execSQL("ALTER TABLE messages ADD COLUMN reply_author TEXT")
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE messages ADD COLUMN group_id INTEGER")
            db.execSQL("ALTER TABLE messages ADD COLUMN sender_uin INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_group ON messages(group_id, sent_at)")
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE messages ADD COLUMN reactions TEXT")
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
            put("reply_snippet", msg.replyToSnippet)
            put("reply_author", msg.replyToAuthor)
            put("group_id", msg.groupId)
            put("sender_uin", msg.senderUin)
            put("reactions", msg.reactions.joinToString(REACTION_DELIM))
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

    fun updateReactions(id: String, reactions: List<String>) {
        val values = ContentValues().apply { put("reactions", reactions.joinToString(REACTION_DELIM)) }
        writableDatabase.update("messages", values, "id = ?", arrayOf(id))
    }

    fun wipe() {
        writableDatabase.execSQL("DELETE FROM messages")
    }

    fun delete(id: String) {
        writableDatabase.delete("messages", "id = ?", arrayOf(id))
    }

    fun all(): List<ChatMessage> {
        val out = ArrayList<ChatMessage>()
        readableDatabase.rawQuery(
            "SELECT id, peer_uin, from_me, body, sent_at, state, kind, media_id, media_key, reply_snippet, reply_author, group_id, sender_uin, reactions FROM messages ORDER BY sent_at ASC", null,
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
                        replyToSnippet = c.getString(9),
                        replyToAuthor = c.getString(10),
                        groupId = if (c.isNull(11)) null else c.getInt(11),
                        senderUin = if (c.isNull(12)) null else c.getInt(12),
                        reactions = c.getString(13)?.split(REACTION_DELIM)?.filter { it.isNotEmpty() } ?: emptyList(),
                    )
                )
            }
        }
        return out
    }

    private companion object {
        const val NAME = "rcq-messages.db"
        const val VERSION = 6
        // Delimiter for the joined reactions column; not a valid emoji char.
        const val REACTION_DELIM = "\u0001"
    }
}
