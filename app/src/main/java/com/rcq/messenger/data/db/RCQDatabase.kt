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
import com.rcq.messenger.data.db.PetDao

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
        PetEntity::class
    ],
    version = 7,
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
    abstract fun petDao(): PetDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add Signal Protocol E2EE fields to messages table
                database.execSQL("ALTER TABLE messages ADD COLUMN ciphertext TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN signalType INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE messages ADD COLUMN isEncrypted INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}