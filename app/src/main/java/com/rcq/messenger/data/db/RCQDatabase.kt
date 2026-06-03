package com.rcq.messenger.data.db

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rcq.messenger.domain.model.*
import com.rcq.messenger.data.db.UserDao
import com.rcq.messenger.data.db.ContactDao
import com.rcq.messenger.data.db.ChatDao
import com.rcq.messenger.data.db.MessageDao
import com.rcq.messenger.data.db.GroupDao
import com.rcq.messenger.data.db.StoryDao
import com.rcq.messenger.data.db.CallDao
import com.rcq.messenger.data.db.SignalKeyDao
import com.rcq.messenger.data.db.PendingOutboxDao

@Database(
    entities = [
        UserEntity::class,
        ContactEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        GroupEntity::class,
        StoryEntity::class,
        StoryItemEntity::class,
        CallEntity::class,
        SignalKeyEntity::class,
        PendingOutboxEntity::class
    ],
    version = 16,
    exportSchema = false
)
@TypeConverters(RoomTypeConverters::class)
abstract class RCQDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun contactDao(): ContactDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun groupDao(): GroupDao
    abstract fun storyDao(): StoryDao
    abstract fun callDao(): CallDao
    abstract fun signalKeyDao(): SignalKeyDao
    abstract fun pendingOutboxDao(): PendingOutboxDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE messages ADD COLUMN ciphertext TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN signalType INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE messages ADD COLUMN isEncrypted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `signal_keys` (
                        `id` TEXT NOT NULL,
                        `keyType` TEXT NOT NULL,
                        `address` TEXT,
                        `keyId` INTEGER,
                        `keyData` BLOB NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Messages: add Phase 1 fields
                database.execSQL("ALTER TABLE messages ADD COLUMN isFromMe INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE messages ADD COLUMN kind TEXT NOT NULL DEFAULT 'TEXT'")
                database.execSQL("ALTER TABLE messages ADD COLUMN mediaId TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN receivedWhileAway INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE messages ADD COLUMN deletedForEveryone INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE messages ADD COLUMN reactions TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN thumbnailB64 TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN durationSec REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE messages ADD COLUMN ttlSeconds INTEGER")
                database.execSQL("ALTER TABLE messages ADD COLUMN forwardedFromName TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN replyToContent TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN replyToAuthorName TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN premiumPriceTokens INTEGER")
                database.execSQL("ALTER TABLE messages ADD COLUMN premiumUnlocked INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE messages ADD COLUMN albumId TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN fileName TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN fileMime TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN fileSizeBytes INTEGER")
                database.execSQL("ALTER TABLE messages ADD COLUMN latitude REAL")
                database.execSQL("ALTER TABLE messages ADD COLUMN longitude REAL")
                database.execSQL("ALTER TABLE messages ADD COLUMN pollId TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN mentionedUserIds TEXT")

                // Contacts: add profile fields
                database.execSQL("ALTER TABLE contacts ADD COLUMN avatarUrl TEXT")
                database.execSQL("ALTER TABLE contacts ADD COLUMN status TEXT NOT NULL DEFAULT 'OFFLINE'")
                database.execSQL("ALTER TABLE contacts ADD COLUMN lastSeen TEXT")
                database.execSQL("ALTER TABLE contacts ADD COLUMN notificationSound TEXT")
                database.execSQL("ALTER TABLE contacts ADD COLUMN customNickname TEXT")

                // Chats: replace old schema with new direct-chat fields
                // SQLite can't DROP columns, so we recreate the table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS chats_new (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `targetId` INTEGER NOT NULL DEFAULT 0,
                        `targetNickname` TEXT NOT NULL DEFAULT '',
                        `targetAvatar` TEXT,
                        `unreadCount` INTEGER NOT NULL DEFAULT 0,
                        `isPinned` INTEGER NOT NULL DEFAULT 0,
                        `isMuted` INTEGER NOT NULL DEFAULT 0,
                        `isArchived` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT OR IGNORE INTO chats_new (id, unreadCount, isMuted, isArchived, createdAt, updatedAt)
                    SELECT id, unreadCount, isMuted, isArchived, createdAt, updatedAt FROM chats
                """.trimIndent())
                database.execSQL("DROP TABLE chats")
                database.execSQL("ALTER TABLE chats_new RENAME TO chats")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chats ADD COLUMN lastMessageContent TEXT")
                database.execSQL("ALTER TABLE chats ADD COLUMN lastMessageTimestamp INTEGER")
                database.execSQL("ALTER TABLE chats ADD COLUMN lastMessageKind TEXT")

                // Pets: recreate with game-model fields
                database.execSQL("DROP TABLE IF EXISTS pets")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS pets (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        rarity TEXT NOT NULL,
                        `imageUrl` TEXT NOT NULL,
                        `equippedBy` INTEGER,
                        `isForSale` INTEGER NOT NULL DEFAULT 0,
                        `salePrice` INTEGER
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE contacts ADD COLUMN identityKey TEXT")
                database.execSQL("ALTER TABLE contacts ADD COLUMN signingKey TEXT")
            }
        }


        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS pending_outbox (
                        localId TEXT NOT NULL PRIMARY KEY,
                        chatId TEXT NOT NULL,
                        recipientUin INTEGER NOT NULL,
                        isGroup INTEGER NOT NULL DEFAULT 0,
                        plainContent TEXT NOT NULL,
                        messageKind TEXT NOT NULL DEFAULT 'TEXT',
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        maxRetries INTEGER NOT NULL DEFAULT 5,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS pets")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE contacts ADD COLUMN statusMessage TEXT")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE groups ADD COLUMN pinnedText TEXT")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE contacts ADD COLUMN signalIdentityKey TEXT")
            }
        }
    }
}
