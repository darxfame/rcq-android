package app.rcq.android.crypto

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite backing for the libsignal protocol stores (v=2 forward secrecy),
 * scoped per [Account.id] like [MessageDb]. Mirrors the iOS SignalProtocolDB
 * schema so the mental model matches across platforms; the stored records are
 * libsignal-serialized blobs (the serialization is protocol-stable, so the
 * exact table layout is a local detail).
 *
 * Tables: local_identity (our keypair + regId), prekeys / signed_prekeys /
 * kyber_prekeys (id -> serialized record), sessions / identities
 * (address "uin:device" -> blob). Group sender-keys are deferred (1:1 v=2
 * first).
 */
class SignalStoreDb(context: Context, accountId: String) :
    SQLiteOpenHelper(context.applicationContext, dbName(accountId), null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE local_identity (id INTEGER PRIMARY KEY, uin INTEGER NOT NULL, identity_keypair BLOB NOT NULL, registration_id INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE prekeys (prekey_id INTEGER PRIMARY KEY, record BLOB NOT NULL)")
        db.execSQL("CREATE TABLE signed_prekeys (signed_prekey_id INTEGER PRIMARY KEY, record BLOB NOT NULL)")
        db.execSQL("CREATE TABLE kyber_prekeys (kyber_prekey_id INTEGER PRIMARY KEY, record BLOB NOT NULL)")
        db.execSQL("CREATE TABLE sessions (address TEXT PRIMARY KEY, record BLOB NOT NULL)")
        db.execSQL("CREATE TABLE identities (address TEXT PRIMARY KEY, identity_key BLOB NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 only so far; future schema bumps are additive.
    }

    // ── local identity ───────────────────────────────────────────────
    /** (uin, serialized IdentityKeyPair, registrationId) or null if unset. */
    fun loadLocalIdentity(): Triple<Int, ByteArray, Int>? {
        readableDatabase.rawQuery(
            "SELECT uin, identity_keypair, registration_id FROM local_identity WHERE id = 1", null,
        ).use { c ->
            if (!c.moveToFirst()) return null
            return Triple(c.getInt(0), c.getBlob(1), c.getInt(2))
        }
    }

    fun storeLocalIdentity(uin: Int, identityKeyPair: ByteArray, registrationId: Int) {
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO local_identity (id, uin, identity_keypair, registration_id) VALUES (1, ?, ?, ?)",
            arrayOf<Any>(uin, identityKeyPair, registrationId),
        )
    }

    // ── int-keyed record tables (prekeys / signed_prekeys / kyber_prekeys) ──
    fun recordByInt(table: String, idCol: String, id: Int): ByteArray? =
        readableDatabase.rawQuery("SELECT record FROM $table WHERE $idCol = ?", arrayOf(id.toString())).use {
            if (it.moveToFirst()) it.getBlob(0) else null
        }

    fun putRecordByInt(table: String, idCol: String, id: Int, record: ByteArray) {
        val v = ContentValues().apply { put(idCol, id); put("record", record) }
        writableDatabase.insertWithOnConflict(table, null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun containsInt(table: String, idCol: String, id: Int): Boolean =
        readableDatabase.rawQuery("SELECT 1 FROM $table WHERE $idCol = ? LIMIT 1", arrayOf(id.toString())).use { it.moveToFirst() }

    fun deleteByInt(table: String, idCol: String, id: Int) {
        writableDatabase.delete(table, "$idCol = ?", arrayOf(id.toString()))
    }

    fun allRecords(table: String): List<ByteArray> {
        val out = ArrayList<ByteArray>()
        readableDatabase.rawQuery("SELECT record FROM $table", null).use { c ->
            while (c.moveToNext()) out.add(c.getBlob(0))
        }
        return out
    }

    // ── text-keyed tables (sessions / identities), key = "uin:device" ──
    fun blobByAddress(table: String, col: String, address: String): ByteArray? =
        readableDatabase.rawQuery("SELECT $col FROM $table WHERE address = ?", arrayOf(address)).use {
            if (it.moveToFirst()) it.getBlob(0) else null
        }

    fun putBlobByAddress(table: String, col: String, address: String, blob: ByteArray) {
        val v = ContentValues().apply { put("address", address); put(col, blob) }
        writableDatabase.insertWithOnConflict(table, null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun containsAddress(table: String, address: String): Boolean =
        readableDatabase.rawQuery("SELECT 1 FROM $table WHERE address = ? LIMIT 1", arrayOf(address)).use { it.moveToFirst() }

    fun deleteByAddress(table: String, address: String) {
        writableDatabase.delete(table, "address = ?", arrayOf(address))
    }

    fun deleteByAddressPrefix(table: String, prefix: String) {
        writableDatabase.delete(table, "address LIKE ?", arrayOf("$prefix%"))
    }

    /** Addresses in [table] starting with "[name]:" (for getSubDeviceSessions). */
    fun addressesWithName(table: String, name: String): List<String> {
        val out = ArrayList<String>()
        readableDatabase.rawQuery("SELECT address FROM $table WHERE address LIKE ?", arrayOf("$name:%")).use { c ->
            while (c.moveToNext()) out.add(c.getString(0))
        }
        return out
    }

    companion object {
        const val VERSION = 1
        private fun dbName(accountId: String) = "signal-stores-$accountId.db"

        /** Drop an account's libsignal store file (burn / local delete). */
        fun wipeAccount(context: Context, accountId: String) {
            context.applicationContext.deleteDatabase(dbName(accountId))
        }
    }
}
